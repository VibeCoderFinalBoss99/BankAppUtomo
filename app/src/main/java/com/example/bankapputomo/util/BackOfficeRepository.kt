package com.example.bankapputomo.util

import android.content.Context
import android.util.Log
import com.example.bankapputomo.model.InAppNotificationModel
import com.example.bankapputomo.model.TicketMessageModel
import com.example.bankapputomo.model.TicketModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.Date

object BackOfficeRepository {

    private const val TAG = "BackOfficeRepository"

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    fun getEffectiveUserId(context: Context? = null): String {
        val storage = AppStorage.get(context)
        if (storage.currentUserId.isNotEmpty()) {
            return storage.currentUserId
        }
        val fbUser = auth.currentUser
        if (fbUser != null && fbUser.uid.isNotEmpty()) {
            return fbUser.uid
        }
        val email = storage.userEmail.ifEmpty { "default_user" }
        return "user_" + email.replace(Regex("[^a-zA-Z0-9]"), "_")
    }

    // ==========================================
    // MAINTENANCE MODE
    // ==========================================
    fun listenToMaintenanceMode(
        context: Context,
        onStatusChanged: (isUnderMaintenance: Boolean, title: String, message: String, endsAt: Long?) -> Unit
    ): ListenerRegistration? {
        val uid = getEffectiveUserId(context)
        val lang = AppStorage.get(context).language

        try {
            com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseSync.FIREBASE_DATABASE_URL)
                .getReference("config/maintenance")
                .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val enabled = snapshot.exists() &&
                            (snapshot.child("enabled").getValue(Boolean::class.java) ?: false)

                        if (!enabled) {
                            onStatusChanged(false, "", "", null)
                            return
                        }

                        val title = snapshot.child("title").child(lang).getValue(String::class.java)
                            ?: snapshot.child("title").child("id").getValue(String::class.java)
                            ?: "Pemeliharaan Sistem"
                        val msg = snapshot.child("message").child(lang).getValue(String::class.java)
                            ?: snapshot.child("message").child("id").getValue(String::class.java)
                            ?: "Sistem sedang dalam peningkatan berkala."
                        onStatusChanged(true, title, msg, null)
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        Log.w(TAG, "RTDB maintenance listener cancelled: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            Log.w(TAG, "RTDB maintenance listener exception: ${e.message}")
        }

        return try {
            firestore.collection("config").document("maintenance")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Maintenance listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        onStatusChanged(false, "", "", null)
                        return@addSnapshotListener
                    }

                    val enabled = snapshot.getBoolean("enabled") ?: false
                    val startsAt = snapshot.getTimestamp("startsAt")?.toDate()
                    val endsAt = snapshot.getTimestamp("endsAt")?.toDate()

                    val now = Date()
                    val isStarted = startsAt == null || now.after(startsAt)
                    val isNotEnded = endsAt == null || now.before(endsAt)
                    val effective = enabled && isStarted && isNotEnded

                    if (!effective) {
                        onStatusChanged(false, "", "", null)
                        return@addSnapshotListener
                    }

                    // Check if user is in maintenance_bypass
                    firestore.collection("maintenance_bypass").document(uid).get()
                        .addOnSuccessListener { bypassDoc ->
                            if (bypassDoc.exists()) {
                                onStatusChanged(false, "", "", null)
                            } else {
                                val titleMap = snapshot.get("title") as? Map<*, *>
                                val msgMap = snapshot.get("message") as? Map<*, *>

                                val title = (titleMap?.get(lang) as? String)
                                    ?: (titleMap?.get("id") as? String)
                                    ?: "Pemeliharaan Sistem"
                                val msg = (msgMap?.get(lang) as? String)
                                    ?: (msgMap?.get("id") as? String)
                                    ?: "Sistem sedang dalam peningkatan berkala demi kenyamanan transaksi Anda."

                                onStatusChanged(true, title, msg, endsAt?.time)
                            }
                        }
                        .addOnFailureListener {
                            val titleMap = snapshot.get("title") as? Map<*, *>
                            val msgMap = snapshot.get("message") as? Map<*, *>
                            val title = (titleMap?.get(lang) as? String) ?: "Pemeliharaan Sistem"
                            val msg = (msgMap?.get(lang) as? String) ?: "Sistem perbankan sedang dalam peningkatan berkala."
                            onStatusChanged(true, title, msg, endsAt?.time)
                        }
                }
        } catch (e: Exception) {
            Log.w(TAG, "listenToMaintenanceMode exception: ${e.message}")
            null
        }
    }

    // ==========================================
    // ACCOUNT STATUS (SUSPENSION)
    // ==========================================
    fun listenToAccountStatus(
        context: Context,
        onStatusChanged: (isSuspended: Boolean, reason: String?) -> Unit
    ): ListenerRegistration? {
        val uid = getEffectiveUserId(context)
        return try {
            firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Account status listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status") ?: "active"
                        val reason = snapshot.getString("statusReason")
                        val isSuspended = status == "suspended"
                        AppStorage.get(context).accountStatus = status
                        AppStorage.get(context).accountSuspendedReason = reason.orEmpty()
                        onStatusChanged(isSuspended, reason)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "listenToAccountStatus exception: ${e.message}")
            null
        }
    }

    // ==========================================
    // TICKETS
    // ==========================================
    fun createTicket(
        context: Context,
        subject: String,
        category: String,
        initialMessage: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val uid = getEffectiveUserId(context)
        val storage = AppStorage.get(context)
        val ticketRef = firestore.collection("tickets").document()
        val now = Timestamp.now()

        val ticketData = hashMapOf(
            "userId" to uid,
            "subject" to subject,
            "category" to category,
            "priority" to "normal",
            "status" to "open",
            "createdAt" to now,
            "updatedAt" to now,
            "lastMessageAt" to now,
            "lastMessageBy" to "customer",
            "unreadForUser" to false,
            "unreadForStaff" to true,
            "customerLanguage" to storage.language
        )

        ticketRef.set(ticketData)
            .addOnSuccessListener {
                val messageRef = ticketRef.collection("messages").document()
                val messageData = hashMapOf(
                    "senderType" to "customer",
                    "senderId" to uid,
                    "senderName" to storage.userName.ifEmpty { "Customer" },
                    "text" to initialMessage,
                    "createdAt" to now
                )
                messageRef.set(messageData)
                    .addOnSuccessListener {
                        // Customer Service Auto-Reply Thank-You dialog
                        val autoReplyRef = ticketRef.collection("messages").document()
                        val autoReplyText = "Terima kasih telah mengisi tiket bantuan! 🙏\n\nTiket Anda (#${ticketRef.id.take(8).uppercase()}) telah kami teruskan ke staf Customer Service. Mohon menunggu sebentar, staf kami akan segera merespons percakapan ini."
                        val autoReplyData = hashMapOf(
                            "senderType" to "staff",
                            "senderId" to "system_bot",
                            "senderName" to "Customer Service (Auto-Reply)",
                            "text" to autoReplyText,
                            "createdAt" to Timestamp(now.seconds + 1, now.nanoseconds)
                        )
                        autoReplyRef.set(autoReplyData).addOnCompleteListener {
                            onSuccess(ticketRef.id)
                        }
                    }
                    .addOnFailureListener { e ->
                        onSuccess(ticketRef.id)
                    }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun getActiveTicket(
        context: Context,
        onResult: (TicketModel?) -> Unit
    ) {
        val uid = getEffectiveUserId(context)
        firestore.collection("tickets")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<TicketModel>()
                snapshot?.documents?.forEach { doc ->
                    val ticket = doc.toObject(TicketModel::class.java)
                    if (ticket != null) {
                        ticket.id = doc.id
                        list.add(ticket)
                    }
                }
                val active = list
                    .filter { it.status == "open" || it.status == "in_progress" || it.status == "waiting_customer" }
                    .maxByOrNull { it.createdAt?.seconds ?: 0L }
                onResult(active)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun listenToTickets(
        context: Context,
        onTicketsChanged: (List<TicketModel>) -> Unit
    ): ListenerRegistration? {
        val uid = getEffectiveUserId(context)
        return try {
            firestore.collection("tickets")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Tickets listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val list = mutableListOf<TicketModel>()
                    snapshot?.documents?.forEach { doc ->
                        val ticket = doc.toObject(TicketModel::class.java)
                        if (ticket != null) {
                            ticket.id = doc.id
                            list.add(ticket)
                        }
                    }
                    onTicketsChanged(list)
                }
        } catch (e: Exception) {
            Log.w(TAG, "listenToTickets exception: ${e.message}")
            null
        }
    }

    fun listenToTicketMessages(
        ticketId: String,
        onMessagesChanged: (List<TicketMessageModel>) -> Unit
    ): ListenerRegistration? {
        return try {
            firestore.collection("tickets").document(ticketId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Messages listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val list = mutableListOf<TicketMessageModel>()
                    snapshot?.documents?.forEach { doc ->
                        val msg = doc.toObject(TicketMessageModel::class.java)
                        if (msg != null) {
                            msg.id = doc.id
                            list.add(msg)
                        }
                    }
                    onMessagesChanged(list)
                }
        } catch (e: Exception) {
            Log.w(TAG, "listenToTicketMessages exception: ${e.message}")
            null
        }
    }

    fun sendCustomerMessage(
        context: Context,
        ticketId: String,
        text: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val uid = getEffectiveUserId(context)
        val storage = AppStorage.get(context)
        val now = Timestamp.now()
        val ticketRef = firestore.collection("tickets").document(ticketId)

        val messageData = hashMapOf(
            "senderType" to "customer",
            "senderId" to uid,
            "senderName" to storage.userName.ifEmpty { "Customer" },
            "text" to text,
            "createdAt" to now
        )

        ticketRef.collection("messages").add(messageData)
            .addOnSuccessListener {
                ticketRef.update(
                    mapOf(
                        "lastMessageAt" to now,
                        "lastMessageBy" to "customer",
                        "unreadForStaff" to true,
                        "updatedAt" to now
                    )
                )
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun updateTicketStatus(
        ticketId: String,
        newStatus: String,
        onComplete: () -> Unit
    ) {
        firestore.collection("tickets").document(ticketId)
            .update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to Timestamp.now()
                )
            )
            .addOnCompleteListener { onComplete() }
    }

    fun submitTicketRating(
        ticketId: String,
        rating: Int,
        onComplete: () -> Unit
    ) {
        firestore.collection("tickets").document(ticketId)
            .update(
                mapOf(
                    "rating" to rating,
                    "updatedAt" to Timestamp.now()
                )
            )
            .addOnCompleteListener { onComplete() }
    }

    // ==========================================
    // NOTIFICATIONS
    // ==========================================
    fun listenToNotifications(
        context: Context,
        onNotificationsChanged: (List<InAppNotificationModel>) -> Unit
    ): ListenerRegistration? {
        val uid = getEffectiveUserId(context)
        return try {
            firestore.collection("users").document(uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Notifications listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val list = mutableListOf<InAppNotificationModel>()
                    snapshot?.documents?.forEach { doc ->
                        val type = doc.getString("type").orEmpty()
                        val read = doc.getBoolean("read") ?: false
                        val params = doc.get("params") as? Map<String, Any?> ?: emptyMap()
                        val createdAt = doc.getTimestamp("createdAt")
                        list.add(
                            InAppNotificationModel(
                                id = doc.id,
                                type = type,
                                params = params,
                                read = read,
                                createdAt = createdAt
                            )
                        )
                    }
                    onNotificationsChanged(list)
                }
        } catch (e: Exception) {
            Log.w(TAG, "listenToNotifications exception: ${e.message}")
            null
        }
    }

    fun markNotificationRead(context: Context, notifId: String) {
        val uid = getEffectiveUserId(context)
        firestore.collection("users").document(uid)
            .collection("notifications").document(notifId)
            .update("read", true)
    }

    // ==========================================
    // CARDS REAL-TIME SYNC
    // ==========================================
    fun listenToUserCards(
        context: Context,
        onCardsChanged: (List<CardModel>) -> Unit
    ): ListenerRegistration? {
        val uid = getEffectiveUserId(context)
        return try {
            firestore.collection("users").document(uid)
                .collection("cards")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Cards listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = mutableListOf<CardModel>()
                        for (doc in snapshot.documents) {
                            val card = doc.toObject(CardModel::class.java)
                            if (card != null) {
                                val c = card.copy(id = doc.id)
                                list.add(c)
                            }
                        }
                        if (list.isNotEmpty()) {
                            AppStorage.get(context).saveCards(list, syncFirebase = false)
                            onCardsChanged(list)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "listenToUserCards exception: ${e.message}")
            null
        }
    }

    fun syncUserCardsToFirestore(context: Context, cards: List<CardModel>) {
        val storage = AppStorage.get(context)
        if (!storage.isLoggedIn && storage.currentUserId.isEmpty()) return

        val accountId = getEffectiveUserId(context)
        val ownerUid = auth.currentUser?.uid ?: accountId
        for (card in cards) {
            val data = mapOf<String, Any>(
                "id" to card.id,
                "cardNumber" to card.cardNumber,
                "cardHolder" to card.cardHolder,
                "expiry" to card.expiry,
                "balance" to card.balance,
                "cardType" to card.cardType,
                "gradientType" to card.gradientType,
                "isBlocked" to card.isBlocked,
                "status" to card.status,
                "statusReasonCode" to (card.statusReasonCode ?: ""),
                "statusChangedBy" to (card.statusChangedBy ?: ""),
                "statusChangedAt" to (card.statusChangedAt ?: 0L),
                "lockedByStaff" to card.lockedByStaff,
                "userId" to ownerUid,
                "accountId" to accountId
            )
            val cardRef = firestore.collection("users").document(accountId)
                .collection("cards").document(card.id)
            cardRef.set(data)

            // Keep the global collection populated for the admin Card Controls page.
            firestore.collection("cards").document("${accountId}_${card.id}").set(data)
        }
    }

    fun deleteUserCardFromFirestore(context: Context, cardId: String) {
        val accountId = getEffectiveUserId(context)
        if (accountId.isBlank()) return
        firestore.collection("users").document(accountId).collection("cards").document(cardId).delete()
        firestore.collection("cards").document("${accountId}_$cardId").delete()
    }

    fun syncLocalCardsToFirestore(context: Context) {
        val cards = AppStorage.get(context).getCards()
        syncUserCardsToFirestore(context, cards)
    }

    fun syncUserBalancesToFirestore(
        context: Context,
        debitBalance: Long,
        savingsBalance: Long,
        currencyBalances: Map<String, Double>,
        creditCardBalance: Long = 0L,
        creditCardLimit: Long = 0L,
        monthlySpendingLimit: Long = 15_000_000L
    ) {
        val storage = AppStorage.get(context)
        if (storage.currentUserId.isEmpty()) return

        firestore.collection("users").document(getEffectiveUserId(context)).set(
            mapOf(
                "debitBalance" to debitBalance,
                "savingsBalance" to savingsBalance,
                "creditCardBalance" to creditCardBalance,
                "creditCardLimit" to creditCardLimit,
                "monthlySpendingLimit" to monthlySpendingLimit,
                "currencyBalances" to currencyBalances,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).addOnFailureListener { error ->
            Log.w(TAG, "Balance Firestore sync failed: ${error.message}")
        }
    }

    fun updateUserFields(context: Context, fields: Map<String, Any>) {
        val uid = getEffectiveUserId(context)
        if (uid.isBlank()) return
        firestore.collection("users").document(uid)
            .set(fields + ("updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            .addOnFailureListener { error ->
                Log.w(TAG, "User field sync failed: ${error.message}")
            }
    }

    private fun beneficiaryData(item: BeneficiaryModel): Map<String, Any> = mapOf(
        "id" to item.id,
        "name" to item.name,
        "bank" to item.bank,
        "account" to item.account,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun syncBeneficiariesToFirestore(context: Context, items: List<BeneficiaryModel>) {
        val uid = getEffectiveUserId(context)
        if (uid.isBlank()) return
        val batch = firestore.batch()
        val collection = firestore.collection("users").document(uid).collection("beneficiaries")
        items.forEach { item -> batch.set(collection.document(item.id), beneficiaryData(item)) }
        batch.commit().addOnFailureListener { error ->
            Log.w(TAG, "Beneficiaries Firestore sync failed: ${error.message}")
        }
    }

    fun deleteBeneficiaryFromFirestore(context: Context, beneficiaryId: String) {
        val uid = getEffectiveUserId(context)
        if (uid.isBlank()) return
        firestore.collection("users").document(uid).collection("beneficiaries")
            .document(beneficiaryId).delete()
    }

    private fun savingGoalData(item: SavingGoalModel): Map<String, Any> = mapOf(
        "id" to item.id,
        "name" to item.name,
        "targetAmount" to item.targetAmount,
        "currentAmount" to item.currentAmount,
        "durationMonths" to item.durationMonths,
        "monthlyDebit" to item.monthlyDebit,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun syncSavingGoalsToFirestore(context: Context, items: List<SavingGoalModel>) {
        val uid = getEffectiveUserId(context)
        if (uid.isBlank()) return
        val batch = firestore.batch()
        val collection = firestore.collection("users").document(uid).collection("savingGoals")
        items.forEach { item -> batch.set(collection.document(item.id), savingGoalData(item)) }
        batch.commit().addOnFailureListener { error ->
            Log.w(TAG, "Saving goals Firestore sync failed: ${error.message}")
        }
    }

    fun deleteSavingGoalFromFirestore(context: Context, goalId: String) {
        val uid = getEffectiveUserId(context)
        if (uid.isBlank()) return
        firestore.collection("users").document(uid).collection("savingGoals")
            .document(goalId).delete()
    }

    fun listenToSavingGoals(
        context: Context,
        onChanged: (List<SavingGoalModel>) -> Unit
    ): ListenerRegistration? {
        val uid = getEffectiveUserId(context)
        if (uid.isBlank()) return null
        return firestore.collection("users").document(uid).collection("savingGoals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Saving goals listener error: ${error.message}")
                    return@addSnapshotListener
                }
                val goals = snapshot?.documents?.mapNotNull { savingGoalFromDocument(it) }.orEmpty()
                AppStorage.get(context).replaceCloudSavingGoals(goals)
                onChanged(goals)
            }
    }

    fun fetchSavingGoals(context: Context, onChanged: (List<SavingGoalModel>) -> Unit) {
        val uid = getEffectiveUserId(context)
        if (uid.isBlank()) {
            onChanged(emptyList())
            return
        }
        firestore.collection("users").document(uid).collection("savingGoals").get()
            .addOnSuccessListener { snapshot ->
                val goals = snapshot.documents.mapNotNull { savingGoalFromDocument(it) }
                AppStorage.get(context).replaceCloudSavingGoals(goals)
                onChanged(goals)
            }
            .addOnFailureListener { onChanged(AppStorage.get(context).getSavingGoals()) }
    }

    fun syncTransactionsToFirestore(context: Context, items: List<TransactionModel>) {
        val firebaseUid = auth.currentUser?.uid ?: return
        val accountId = AppStorage.get(context).currentUserId
        if (accountId.isBlank()) return
        val batch = firestore.batch()
        items.forEach { transaction ->
            val data = transactionData(transaction, firebaseUid, accountId)
            batch.set(firestore.collection("transactions").document(transaction.id), data)
        }
        batch.commit().addOnFailureListener { error ->
            Log.w(TAG, "Transactions Firestore sync failed: ${error.message}")
        }
    }

    private fun transactionData(
        transaction: TransactionModel,
        firebaseUid: String,
        accountId: String
    ): Map<String, Any?> = mapOf(
        "id" to transaction.id,
        "userId" to firebaseUid,
        "accountId" to accountId,
        "title" to transaction.title,
        "category" to transaction.category,
        "amount" to transaction.amount,
        "isIncome" to transaction.isIncome,
        "counterparty" to transaction.counterparty,
        "date" to transaction.date,
        "referenceNo" to transaction.referenceNo,
        "status" to transaction.status,
        "createdAt" to if (transaction.timestamp > 0L) {
            Timestamp(Date(transaction.timestamp))
        } else {
            FieldValue.serverTimestamp()
        }
    )

    /** Persists a newly created transaction for the authenticated Firebase user. */
    fun syncTransactionToFirestore(context: Context, transaction: TransactionModel) {
        val firebaseUser = auth.currentUser ?: return
        val firebaseUid = firebaseUser.uid.takeIf { it.isNotBlank() } ?: return
        val storage = AppStorage.get(context)
        val storageEmail = storage.userEmail.trim()
        val firebaseEmail = firebaseUser.email?.trim().orEmpty()
        if (storageEmail.isNotEmpty() && firebaseEmail.isNotEmpty() &&
            !storageEmail.equals(firebaseEmail, ignoreCase = true)
        ) return

        val data = transactionData(transaction, firebaseUid, storage.currentUserId)

        firestore.collection("transactions")
            .document(transaction.id)
            .set(data)
            .addOnFailureListener { error ->
                Log.w(TAG, "Transaction Firestore sync failed: ${error.message}")
            }
    }

    /** Loads every business collection into the process cache before the dashboard opens. */
    fun hydrateUserData(context: Context, onComplete: () -> Unit = {}) {
        val storage = AppStorage.get(context)
        val preferredId = getEffectiveUserId(context)
        val userRef = firestore.collection("users").document(preferredId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                hydrateFromUserDocument(context, document, onComplete)
            } else if (storage.userEmail.isNotBlank()) {
                firestore.collection("users")
                    .whereEqualTo("email", storage.userEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val fallback = snapshot.documents.firstOrNull()
                        if (fallback != null) {
                            storage.currentUserId = fallback.id
                            hydrateFromUserDocument(context, fallback, onComplete)
                        } else {
                            onComplete()
                        }
                    }
                    .addOnFailureListener { onComplete() }
            } else {
                onComplete()
            }
        }.addOnFailureListener { error ->
            Log.w(TAG, "User hydration failed: ${error.message}")
            onComplete()
        }
    }

    private fun hydrateFromUserDocument(
        context: Context,
        document: com.google.firebase.firestore.DocumentSnapshot,
        onComplete: () -> Unit
    ) {
        val storage = AppStorage.get(context)
        val uid = document.id
        storage.applyCloudUserData(document.data ?: emptyMap())

        firestore.collection("users").document(uid).collection("cards").get()
            .addOnSuccessListener { cardSnapshot ->
                val cards = cardSnapshot.documents.mapNotNull { cardFromDocument(it) }
                val continueHydration = {
                    firestore.collection("users").document(uid).collection("beneficiaries").get()
                        .addOnSuccessListener { beneficiarySnapshot ->
                            storage.replaceCloudBeneficiaries(
                                beneficiarySnapshot.documents.mapNotNull { beneficiaryFromDocument(it) }
                            )
                            firestore.collection("users").document(uid).collection("savingGoals").get()
                                .addOnSuccessListener { goalSnapshot ->
                                    storage.replaceCloudSavingGoals(
                                        goalSnapshot.documents.mapNotNull { savingGoalFromDocument(it) }
                                    )
                                    hydrateTransactions(context, uid, onComplete)
                                }
                                .addOnFailureListener { hydrateTransactions(context, uid, onComplete) }
                        }
                        .addOnFailureListener { hydrateTransactions(context, uid, onComplete) }
                }
                if (cards.isNotEmpty()) {
                    storage.replaceCloudCards(cards)
                    continueHydration()
                } else {
                    // Older builds sometimes wrote only the admin-facing card
                    // mirror. Recover it once without recreating a new card.
                    firestore.collection("cards").whereEqualTo("accountId", uid).get()
                        .addOnSuccessListener { globalSnapshot ->
                            val globalCards = globalSnapshot.documents.mapNotNull { cardFromDocument(it) }
                            if (globalCards.isNotEmpty()) {
                                storage.replaceCloudCards(globalCards)
                                continueHydration()
                            } else {
                                firestore.collection("cards").whereEqualTo("userId", uid).get()
                                    .addOnSuccessListener { legacySnapshot ->
                                        storage.replaceCloudCards(
                                            legacySnapshot.documents.mapNotNull { cardFromDocument(it) }
                                        )
                                        continueHydration()
                                    }
                                    .addOnFailureListener { continueHydration() }
                            }
                        }
                        .addOnFailureListener { continueHydration() }
                }
            }
            .addOnFailureListener { hydrateTransactions(context, uid, onComplete) }
    }

    private fun hydrateTransactions(context: Context, accountId: String, onComplete: () -> Unit) {
        val query = firestore.collection("transactions").whereEqualTo("accountId", accountId)
        query.get().addOnSuccessListener { snapshot ->
            AppStorage.get(context).replaceCloudTransactions(
                snapshot.documents.mapNotNull { transactionFromDocument(it) }
            )
            onComplete()
        }.addOnFailureListener {
            onComplete()
        }
    }

    private fun cardFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): CardModel? {
        val cardNumber = doc.getString("cardNumber") ?: return null
        return CardModel(
            id = doc.getString("id") ?: doc.id,
            cardNumber = cardNumber,
            cardHolder = doc.getString("cardHolder").orEmpty(),
            expiry = doc.getString("expiry").orEmpty(),
            balance = doc.getLong("balance") ?: 0L,
            cardType = doc.getString("cardType") ?: "Debit",
            gradientType = (doc.getLong("gradientType") ?: 1L).toInt(),
            isBlocked = doc.getBoolean("isBlocked") ?: false,
            status = doc.getString("status") ?: "active",
            statusReasonCode = doc.getString("statusReasonCode"),
            statusChangedBy = doc.getString("statusChangedBy"),
            statusChangedAt = doc.getLong("statusChangedAt"),
            lockedByStaff = doc.getBoolean("lockedByStaff") ?: false
        )
    }

    private fun beneficiaryFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): BeneficiaryModel? {
        val name = doc.getString("name") ?: return null
        return BeneficiaryModel(
            id = doc.getString("id") ?: doc.id,
            name = name,
            bank = doc.getString("bank").orEmpty(),
            account = doc.getString("account").orEmpty()
        )
    }

    private fun savingGoalFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): SavingGoalModel? {
        val name = doc.getString("name") ?: return null
        return SavingGoalModel(
            id = doc.getString("id") ?: doc.id,
            name = name,
            targetAmount = doc.getLong("targetAmount") ?: 0L,
            currentAmount = doc.getLong("currentAmount") ?: 0L,
            durationMonths = (doc.getLong("durationMonths") ?: 0L).toInt(),
            monthlyDebit = doc.getLong("monthlyDebit") ?: 0L
        )
    }

    private fun transactionFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): TransactionModel? {
        val title = doc.getString("title") ?: return null
        return TransactionModel(
            id = doc.getString("id") ?: doc.id,
            title = title,
            category = doc.getString("category").orEmpty(),
            amount = doc.getLong("amount") ?: 0L,
            isIncome = doc.getBoolean("isIncome") ?: false,
            counterparty = doc.getString("counterparty").orEmpty(),
            date = doc.getString("date").orEmpty(),
            referenceNo = doc.getString("referenceNo").orEmpty(),
            status = doc.getString("status") ?: "SUCCESS",
            timestamp = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    }
}
