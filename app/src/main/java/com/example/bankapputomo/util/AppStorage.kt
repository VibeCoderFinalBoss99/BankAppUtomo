package com.example.bankapputomo.util

import android.content.Context
import android.content.SharedPreferences
import com.example.bankapputomo.BankApplication

data class TransactionModel(
    val id: String,
    val title: String,
    val category: String,
    val amount: Long,
    val isIncome: Boolean,
    val counterparty: String,
    val date: String,
    val referenceNo: String,
    val status: String = "SUCCESS",
    val timestamp: Long = 0L
)

typealias TransactionRecord = TransactionModel

data class BeneficiaryModel(
    val id: String,
    val name: String,
    val bank: String,
    val account: String
)

data class CardModel(
    val id: String = "",
    val cardNumber: String = "",
    var cardHolder: String = "",
    val expiry: String = "",
    var balance: Long = 0L,
    val cardType: String = "Debit",
    val gradientType: Int = 1,
    var isBlocked: Boolean = false,
    var status: String = "active",
    var statusReasonCode: String? = null,
    var statusChangedBy: String? = null,
    var statusChangedAt: Long? = null,
    var lockedByStaff: Boolean = false
)

data class SavingGoalModel(
    val id: String = "",
    val name: String = "",
    val targetAmount: Long = 0L,
    val currentAmount: Long = 0L,
    val durationMonths: Int = 0,
    val monthlyDebit: Long = 0L
)

data class RegisteredUserRecord(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val ktp: String,
    val password: String,
    var isPinSet: Boolean = false,
    var pin: String = "",
    var biometricEnabled: Boolean = false,
    var googleLinked: Boolean = false,
    var googleEmail: String = "",
    var googleName: String = "",
    var debitBalance: Long = 25000000L,
    var savingsBalance: Long = 0L,
    var status: String = "active",
    var statusReason: String = ""
)

data class BranchModel(
    val id: String = "",
    val name: String = "",
    val type: String = "KC", // "KC", "KCP", "ATM"
    val address: String = "",
    val city: String = "Jakarta Pusat",
    val distance: String = "0.8 km",
    val hours: String = "08:00 - 15:30",
    val services: String = "ATM 24 Jam • Tarik Tunai",
    val latitude: Double = -6.2146,
    val longitude: Double = 106.8217,
    val isCardlessSupported: Boolean = true
)

class AppStorage private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("BankAppUtomoPrefs", Context.MODE_PRIVATE)
    private val legacyPrefs: SharedPreferences = appContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    // Business data lives in Firestore. These fields are process-memory caches only;
    // they are hydrated after Firebase Authentication succeeds and are never persisted
    // to SharedPreferences.
    private var memoryUserName = ""
    private var memoryUserPhone = ""
    private var memoryUserAvatarResName = "source_b_avatar"
    private var memoryUserCustomAvatarUri = ""
    private var memoryUserPin = ""
    private var memoryAccountStatus = "active"
    private var memoryAccountSuspendedReason = ""
    private var memoryBiometricEnabled = false
    private var memoryPinConfigured = false
    private var memoryGoogleLinked = false
    private var memoryGoogleEmail = ""
    private var memoryGoogleName = ""
    private var memoryCardOnlineTransactionsEnabled = true
    private var memoryCardFrozen = false
    private var memoryDebitBalance = 0L
    private var memorySavingsBalance = 0L
    private var memoryCreditCardBalance = 0L
    private var memoryCreditCardLimit = 0L
    private var memoryMonthlySpendingLimit = 15_000_000L
    private var memoryCurrencyBalances = mutableMapOf<String, Double>()
    private var memoryTransactions = mutableListOf<TransactionModel>()
    private var memoryBeneficiaries = mutableListOf<BeneficiaryModel>()
    private var memoryCards = mutableListOf<CardModel>()
    private var memorySavingGoals = mutableListOf<SavingGoalModel>()
    private var memoryBranches = mutableListOf<BranchModel>()
    private var memoryRegisteredUsers = mutableListOf<RegisteredUserRecord>()
    private var memoryHasSeenInAppGuidance = false
    private var memoryHasSeenOnboarding = false
    private var memoryActiveCardVariants = mutableSetOf<Int>()
    private var memoryActiveSupportTicketId: String? = null
    private var memoryCloudUserId = ""
    private var cloudBalancesLoaded = false
    private var isApplyingCloudState = false

    companion object {
        @Volatile
        private var instance: AppStorage? = null

        fun init(context: Context): AppStorage {
            return instance ?: synchronized(this) {
                instance ?: AppStorage(context).also { instance = it }
            }
        }

        fun get(context: Context? = null): AppStorage {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val ctx = context ?: BankApplication.instance
                    AppStorage(ctx).also { instance = it }
                }
            }
        }
    }

    // App State & Preferences
    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", legacyPrefs.getBoolean("dark_mode", false))
        set(value) {
            prefs.edit().putBoolean("dark_mode", value).apply()
            legacyPrefs.edit().putBoolean("dark_mode", value).apply()
        }

    var language: String
        get() = prefs.getString("language", "id") ?: "id"
        set(value) = prefs.edit().putString("language", value).apply()

    var hasSeenOnboarding: Boolean
        get() = memoryHasSeenOnboarding
        set(value) {
            memoryHasSeenOnboarding = value
            syncUserField("hasSeenOnboarding", value)
        }

    var biometricEnabled: Boolean
        get() = memoryBiometricEnabled
        set(value) {
            memoryBiometricEnabled = value
            syncUserField("biometricEnabled", value)
        }

    var isPinConfigured: Boolean
        get() = memoryPinConfigured
        set(value) {
            memoryPinConfigured = value
            syncUserField("pinEnabled", value)
        }

    var googleLinked: Boolean
        get() = memoryGoogleLinked
        set(value) {
            memoryGoogleLinked = value
            syncUserField("googleLinked", value)
        }

    var activeSupportTicketId: String?
        // The open ticket is transient UI state. Ticket data itself lives in
        // Firestore under tickets/{ticketId}; it must never be persisted locally.
        get() = memoryActiveSupportTicketId
        set(value) {
            memoryActiveSupportTicketId = value
        }

    var googleEmail: String
        get() = memoryGoogleEmail
        set(value) {
            memoryGoogleEmail = value
            syncUserField("googleEmail", value)
        }

    var googleName: String
        get() = memoryGoogleName
        set(value) {
            memoryGoogleName = value
            syncUserField("googleName", value)
        }

    var currentUserId: String
        get() = prefs.getString("current_user_id", "") ?: ""
        set(value) = prefs.edit().putString("current_user_id", value).apply()

    var cardOnlineTransactionsEnabled: Boolean
        get() = memoryCardOnlineTransactionsEnabled
        set(value) {
            memoryCardOnlineTransactionsEnabled = value
            syncUserField("cardOnlineTransactionsEnabled", value)
        }

    var cardFrozen: Boolean
        get() = memoryCardFrozen
        set(value) {
            memoryCardFrozen = value
            syncUserField("cardFrozen", value)
        }

    fun getActiveCardVariants(): MutableList<Int> {
        val variants = getCards().map { it.gradientType }.distinct().toMutableList()
        if (variants.isNotEmpty()) return variants
        return memoryActiveCardVariants.toMutableList().ifEmpty { mutableListOf(1, 3) }
    }

    fun setCardVariantActive(variant: Int, active: Boolean) {
        val variants = getActiveCardVariants()
        if (active && variant in 1..4 && variant !in variants) variants.add(variant)
        if (!active) variants.remove(variant)
        memoryActiveCardVariants = variants.toMutableSet()
        syncUserField("activeCardVariants", variants)
    }

    var hasSeenInAppGuidance: Boolean
        get() = memoryHasSeenInAppGuidance
        set(value) {
            memoryHasSeenInAppGuidance = value
            syncUserField("hasSeenInAppGuidance", value)
        }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) {
            prefs.edit().putBoolean("is_logged_in", value).apply()
            legacyPrefs.edit().putBoolean("is_logged_in", value).apply()
        }

    fun resetSessionAndOnboarding() {
        prefs.edit()
            .remove("is_logged_in")
            .remove("current_user_id")
            .remove("logged_in_user")
            .remove("user_email")
            .apply()
        legacyPrefs.edit()
            .remove("is_logged_in")
            .remove("logged_in_user")
            .apply()
        memoryCloudUserId = ""
        memoryUserName = ""
        memoryUserPhone = ""
        memoryUserAvatarResName = "source_b_avatar"
        memoryUserCustomAvatarUri = ""
        memoryUserPin = ""
        memoryAccountStatus = "active"
        memoryAccountSuspendedReason = ""
        memoryGoogleLinked = false
        memoryGoogleEmail = ""
        memoryGoogleName = ""
        memoryBiometricEnabled = false
        memoryPinConfigured = false
        memoryHasSeenInAppGuidance = false
        memoryHasSeenOnboarding = false
        memoryActiveCardVariants.clear()
        memoryActiveSupportTicketId = null
        memoryDebitBalance = 0L
        memorySavingsBalance = 0L
        cloudBalancesLoaded = false
        memoryCreditCardBalance = 0L
        memoryCreditCardLimit = 0L
        memoryCurrencyBalances.clear()
        memoryTransactions.clear()
        memoryBeneficiaries.clear()
        memoryCards.clear()
        memorySavingGoals.clear()
        memoryBranches.clear()
        memoryRegisteredUsers.clear()
        isApplyingCloudState = false
    }

    var loggedInUser: String
        get() = prefs.getString("logged_in_user", "") ?: ""
        set(value) = prefs.edit().putString("logged_in_user", value).apply()

    var userName: String
        get() = memoryUserName.ifEmpty { loggedInUser }
        set(value) {
            memoryUserName = value
            loggedInUser = value
            syncUserField("name", value)
        }

    var userEmail: String
        get() = prefs.getString("user_email", "") ?: ""
        set(value) = prefs.edit().putString("user_email", value).apply()

    var userPhone: String
        get() = memoryUserPhone
        set(value) {
            memoryUserPhone = value
            syncUserField("phone", value)
        }

    var userAvatarResName: String
        get() = memoryUserAvatarResName
        set(value) {
            memoryUserAvatarResName = value
            syncUserField("avatarResName", value)
        }

    var userCustomAvatarUri: String
        get() = memoryUserCustomAvatarUri
        set(value) {
            memoryUserCustomAvatarUri = value
            syncUserField("customAvatarUri", value)
        }

    var userPin: String
        get() = memoryUserPin
        set(value) {
            memoryUserPin = value
            syncUserField("pin", value)
        }

    var accountStatus: String
        get() = memoryAccountStatus
        set(value) {
            memoryAccountStatus = value
            syncUserField("status", value)
        }

    fun switchUser(
        uid: String,
        name: String,
        email: String,
        phone: String,
        isPinSet: Boolean = false,
        bioEnabled: Boolean = false,
        isGoogleLinked: Boolean = false,
        linkedGoogleEmail: String = "",
        linkedGoogleName: String = "",
        initialDebit: Long = 25000000L,
        initialSavings: Long = 0L,
        pinValue: String = ""
    ) {
        currentUserId = uid
        memoryCloudUserId = uid
        isApplyingCloudState = true
        memoryUserName = name
        userEmail = email
        memoryUserPhone = phone
        // A successful login has already passed the Firestore account-status
        // check. Clear the previous session's suspended state before any
        // Activity security check runs; hydration will apply the cloud value.
        memoryAccountStatus = "active"
        memoryAccountSuspendedReason = ""
        memoryPinConfigured = isPinSet
        memoryUserPin = pinValue
        memoryBiometricEnabled = bioEnabled
        memoryGoogleLinked = isGoogleLinked
        memoryGoogleEmail = linkedGoogleEmail
        memoryGoogleName = linkedGoogleName
        memoryActiveSupportTicketId = null
        memoryDebitBalance = initialDebit
        memorySavingsBalance = initialSavings
        memoryCreditCardBalance = 0L
        memoryCreditCardLimit = 0L
        memoryCurrencyBalances = defaultCurrencyBalances()
        memoryTransactions.clear()
        memoryBeneficiaries.clear()
        memoryCards.clear()
        memorySavingGoals.clear()
        memoryBranches.clear()
        memoryActiveCardVariants.clear()
        isApplyingCloudState = false
        isLoggedIn = true
    }

    private fun defaultCurrencyBalances(): MutableMap<String, Double> = mutableMapOf(
        "USD" to 0.0,
        "KRW" to 0.0,
        "VND" to 0.0,
        "HKD" to 0.0,
        "NTD" to 0.0,
        "JMD" to 0.0
    )

    private fun syncUserField(field: String, value: Any) {
        if (!isApplyingCloudState && currentUserId.isNotEmpty()) {
            BackOfficeRepository.updateUserFields(appContext, mapOf(field to value))
        }
    }

    fun applyCloudUserData(data: Map<String, Any?>) {
        isApplyingCloudState = true
        try {
            memoryUserName = data["name"]?.toString().orEmpty()
            memoryUserPhone = data["phone"]?.toString().orEmpty()
            memoryUserAvatarResName = data["avatarResName"]?.toString() ?: "source_b_avatar"
            memoryUserCustomAvatarUri = (
                data["customAvatarUrl"]?.toString()
                    ?: data["customAvatarUri"]?.toString()
                    ?: ""
            )
            memoryUserPin = data["pin"]?.toString().orEmpty()
            memoryPinConfigured = data["pinEnabled"] as? Boolean ?: false
            memoryBiometricEnabled = data["biometricEnabled"] as? Boolean ?: false
            memoryGoogleLinked = data["googleLinked"] as? Boolean ?: false
            memoryGoogleEmail = data["googleEmail"]?.toString().orEmpty()
            memoryGoogleName = data["googleName"]?.toString().orEmpty()
            memoryAccountStatus = data["status"]?.toString() ?: "active"
            memoryAccountSuspendedReason = data["statusReason"]?.toString().orEmpty()
            memoryCardOnlineTransactionsEnabled = data["cardOnlineTransactionsEnabled"] as? Boolean ?: true
            memoryCardFrozen = data["cardFrozen"] as? Boolean ?: false
            memoryHasSeenInAppGuidance = data["hasSeenInAppGuidance"] as? Boolean ?: false
            memoryHasSeenOnboarding = data["hasSeenOnboarding"] as? Boolean ?: false
            memoryActiveCardVariants = ((data["activeCardVariants"] as? List<*>) ?: emptyList())
                .mapNotNull { (it as? Number)?.toInt() }
                .toMutableSet()
            memoryDebitBalance = (data["debitBalance"] as? Number)?.toLong() ?: memoryDebitBalance
            memorySavingsBalance = (data["savingsBalance"] as? Number)?.toLong() ?: memorySavingsBalance
            cloudBalancesLoaded = data.containsKey("debitBalance") || data.containsKey("savingsBalance")
            memoryCreditCardBalance = (data["creditCardBalance"] as? Number)?.toLong() ?: 0L
            memoryCreditCardLimit = (data["creditCardLimit"] as? Number)?.toLong() ?: 0L
            memoryMonthlySpendingLimit = (data["monthlySpendingLimit"] as? Number)?.toLong() ?: memoryMonthlySpendingLimit
            val rawCurrency = data["currencyBalances"] as? Map<*, *>
            if (rawCurrency != null) {
                memoryCurrencyBalances = defaultCurrencyBalances().apply {
                    rawCurrency.forEach { (key, value) ->
                        val amount = (value as? Number)?.toDouble() ?: value?.toString()?.toDoubleOrNull()
                        if (amount != null) put(key.toString().uppercase(java.util.Locale.US), amount)
                    }
                }
            }
        } finally {
            isApplyingCloudState = false
        }
    }

    fun replaceCloudTransactions(list: List<TransactionModel>) {
        memoryTransactions = list.sortedByDescending { getTransactionTimestamp(it) }.toMutableList()
    }

    fun replaceCloudBeneficiaries(list: List<BeneficiaryModel>) {
        memoryBeneficiaries = list.toMutableList()
    }

    fun replaceCloudCards(list: List<CardModel>) {
        memoryCards = list.toMutableList()
    }

    fun replaceCloudSavingGoals(list: List<SavingGoalModel>) {
        memorySavingGoals = list.toMutableList()
    }

    fun getRegisteredUsers(): MutableList<RegisteredUserRecord> {
        return memoryRegisteredUsers
    }

    fun saveRegisteredUser(record: RegisteredUserRecord) {
        val list = getRegisteredUsers()
        val index = list.indexOfFirst { it.uid == record.uid || it.email.equals(record.email, ignoreCase = true) }
        if (index != -1) {
            list[index] = record
        } else {
            list.add(record)
        }
        BackOfficeRepository.updateUserFields(
            appContext,
            mapOf(
                "id" to record.uid,
                "uid" to record.uid,
                "name" to record.name,
                "email" to record.email,
                "phone" to record.phone,
                "ktp" to record.ktp,
                "status" to record.status,
                "role" to "customer",
                "debitBalance" to record.debitBalance,
                "savingsBalance" to record.savingsBalance
            )
        )
    }

    fun findRegisteredUserByEmail(email: String): RegisteredUserRecord? {
        val clean = email.trim()
        return getRegisteredUsers().firstOrNull { it.email.equals(clean, ignoreCase = true) }
    }

    fun findRegisteredUserByGoogleEmail(googleEmail: String): RegisteredUserRecord? {
        val clean = googleEmail.trim()
        return getRegisteredUsers().firstOrNull {
            (it.googleLinked && it.googleEmail.equals(clean, ignoreCase = true)) ||
            (it.googleLinked && it.email.equals(clean, ignoreCase = true))
        }
    }

    fun updateRegisteredUserSecurity(
        uid: String,
        pinSet: Boolean? = null,
        pinVal: String? = null,
        bio: Boolean? = null,
        gLinked: Boolean? = null,
        gEmail: String? = null,
        gName: String? = null
    ) {
        val list = getRegisteredUsers()
        val user = list.firstOrNull { it.uid == uid } ?: return
        if (pinSet != null) user.isPinSet = pinSet
        if (pinVal != null) user.pin = pinVal
        if (bio != null) user.biometricEnabled = bio
        if (gLinked != null) user.googleLinked = gLinked
        if (gEmail != null) user.googleEmail = gEmail
        if (gName != null) user.googleName = gName
        BackOfficeRepository.updateUserFields(
            appContext,
            mapOf(
                "pinEnabled" to user.isPinSet,
                "pin" to user.pin,
                "biometricEnabled" to user.biometricEnabled,
                "googleLinked" to user.googleLinked,
                "googleEmail" to user.googleEmail,
                "googleName" to user.googleName
            )
        )
    }

    var accountSuspendedReason: String
        get() = memoryAccountSuspendedReason
        set(value) {
            memoryAccountSuspendedReason = value
            syncUserField("statusReason", value)
        }

    // Balances
    var debitBalance: Long
        get() = memoryDebitBalance
        set(value) {
            memoryDebitBalance = value.coerceAtLeast(0L)
            cloudBalancesLoaded = true
            syncBalancesToFirebase()
        }

    fun hasStoredDebitBalanceForUser(uid: String = currentUserId): Boolean {
        return uid.isNotEmpty() && uid == memoryCloudUserId && cloudBalancesLoaded
    }

    var savingsBalance: Long
        get() = memorySavingsBalance
        set(value) {
            memorySavingsBalance = value.coerceAtLeast(0L)
            cloudBalancesLoaded = true
            syncBalancesToFirebase()
        }

    var creditCardBalance: Long
        get() = memoryCreditCardBalance
        set(value) {
            memoryCreditCardBalance = value.coerceAtLeast(0L)
            syncBalancesToFirebase()
        }

    var creditCardLimit: Long
        get() = memoryCreditCardLimit
        set(value) {
            memoryCreditCardLimit = value.coerceAtLeast(0L)
            syncBalancesToFirebase()
        }

    fun syncBalancesToFirebase() {
        val currencyBalances = getCurrencyBalances()
        BackOfficeRepository.syncUserBalancesToFirestore(
            appContext,
            debitBalance,
            savingsBalance,
            currencyBalances,
            creditCardBalance,
            creditCardLimit,
            memoryMonthlySpendingLimit
        )
    }

    fun getCurrencyBalances(): MutableMap<String, Double> {
        return memoryCurrencyBalances.toMutableMap()
    }

    fun restoreCurrencyBalances(values: Map<String, Any?>) {
        val restored = getCurrencyBalances()
        values.forEach { (key, value) ->
            val amount = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
            if (amount != null && amount >= 0.0) {
                restored[key.uppercase(java.util.Locale.US)] = amount
            }
        }
        memoryCurrencyBalances = restored
        syncBalancesToFirebase()
    }

    fun getCurrencyBalance(currency: String): Double {
        return if (currency.equals("IDR", ignoreCase = true)) {
            debitBalance.toDouble()
        } else {
            getCurrencyBalances()[currency.uppercase(java.util.Locale.US)] ?: 0.0
        }
    }

    private fun setCurrencyBalance(currency: String, amount: Double) {
        val normalized = currency.uppercase(java.util.Locale.US)
        if (normalized == "IDR") {
            debitBalance = amount.coerceAtLeast(0.0).toLong()
            return
        }
        val balances = getCurrencyBalances()
        balances[normalized] = amount.coerceAtLeast(0.0)
        memoryCurrencyBalances = balances
        syncBalancesToFirebase()
    }

    fun executeCurrencyExchange(
        fromCurrency: String,
        toCurrency: String,
        fromAmount: Double,
        toAmount: Double
    ): Boolean {
        val from = fromCurrency.uppercase(java.util.Locale.US)
        val to = toCurrency.uppercase(java.util.Locale.US)
        if (from == to || fromAmount <= 0.0 || toAmount <= 0.0) return false

        val available = getCurrencyBalance(from)
        if (available + 0.000001 < fromAmount) return false

        val title = "Exchange $from ke $to"
        val counterparty = "UTOMO Bank FX ($from/$to)"

        return when {
            from == "IDR" -> {
                val recorded = addTransaction(
                    title = title,
                    category = "Exchange",
                    amount = fromAmount.toLong(),
                    isIncome = false,
                    counterparty = counterparty
                )
                if (recorded) setCurrencyBalance(to, getCurrencyBalance(to) + toAmount)
                recorded
            }
            to == "IDR" -> {
                setCurrencyBalance(from, available - fromAmount)
                val recorded = addTransaction(
                    title = title,
                    category = "Exchange",
                    amount = toAmount.toLong(),
                    isIncome = true,
                    counterparty = counterparty
                )
                if (!recorded) setCurrencyBalance(from, available)
                recorded
            }
            else -> {
                setCurrencyBalance(from, available - fromAmount)
                setCurrencyBalance(to, getCurrencyBalance(to) + toAmount)
                val recorded = addTransaction(
                    title = title,
                    category = "Exchange",
                    amount = toAmount.toLong(),
                    isIncome = false,
                    counterparty = counterparty,
                    adjustDebitBalance = false
                )
                if (!recorded) {
                    setCurrencyBalance(from, available)
                    setCurrencyBalance(to, getCurrencyBalance(to) - toAmount)
                }
                recorded
            }
        }
    }

    // Transactions List
    fun getTransactions(): MutableList<TransactionModel> {
        return memoryTransactions
    }

    fun saveTransactions(list: List<TransactionModel>) {
        memoryTransactions = list.sortedByDescending { getTransactionTimestamp(it) }.toMutableList()
        BackOfficeRepository.syncTransactionsToFirestore(appContext, memoryTransactions)
    }

    fun getTransactionTimestamp(transaction: TransactionModel): Long {
        if (transaction.timestamp > 0L) return transaction.timestamp
        val formats = listOf(
            java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.ENGLISH),
            java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID")),
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH),
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
        )
        for (format in formats) {
            try {
                val parsed = format.parse(transaction.date)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // Try the next supported locale/date format.
            }
        }
        return 0L
    }

    fun addTransaction(
        title: String,
        category: String,
        amount: Long,
        isIncome: Boolean,
        counterparty: String,
        adjustDebitBalance: Boolean = true
    ): Boolean {
        if (!isIncome && adjustDebitBalance) {
            val primaryCard = getCards().firstOrNull()
            if (primaryCard != null && (primaryCard.status == "blocked" || primaryCard.status == "frozen" || primaryCard.isBlocked)) {
                return false
            }
            if (amount > debitBalance) {
                return false
            }
        }
        val list = getTransactions()
        val tx = TransactionModel(
            id = "TX-${(10000000..99999999).random()}",
            title = title,
            category = category,
            amount = amount,
            isIncome = isIncome,
            counterparty = counterparty,
            date = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
            referenceNo = "REF-${(10000000..99999999).random()}",
            timestamp = System.currentTimeMillis()
        )
        list.add(0, tx)
        if (adjustDebitBalance) {
            if (isIncome) {
                debitBalance += amount
            } else {
                debitBalance -= amount
            }
            val cards = getCards()
            cards.filter { it.cardType.equals("Debit", ignoreCase = true) }
                .forEach { it.balance = debitBalance }
            saveCards(cards)
        }
        saveTransactions(list)
        BackOfficeRepository.syncTransactionToFirestore(appContext, tx)
        return true
    }

    // Beneficiaries
    fun getBeneficiaries(): MutableList<BeneficiaryModel> {
        return memoryBeneficiaries
    }

    fun saveBeneficiaries(list: List<BeneficiaryModel>) {
        memoryBeneficiaries = list.toMutableList()
        BackOfficeRepository.syncBeneficiariesToFirestore(appContext, memoryBeneficiaries)
    }

    fun addBeneficiary(name: String, account: String, bank: String) {
        val list = getBeneficiaries()
        list.add(0, BeneficiaryModel("b-${System.currentTimeMillis()}", name, bank, account))
        saveBeneficiaries(list)
    }

    fun deleteBeneficiary(name: String) {
        val list = getBeneficiaries()
        val removed = list.filter { it.name.equals(name, ignoreCase = true) }
        list.removeAll { it.name.equals(name, ignoreCase = true) }
        saveBeneficiaries(list)
        removed.forEach { BackOfficeRepository.deleteBeneficiaryFromFirestore(appContext, it.id) }
    }

    fun updateBeneficiary(id: String, newName: String, newAccount: String, newBank: String) {
        val list = getBeneficiaries()
        val idx = list.indexOfFirst { it.id == id || it.name.equals(id, ignoreCase = true) }
        if (idx != -1) {
            list[idx] = BeneficiaryModel(list[idx].id, newName, newBank, newAccount)
            saveBeneficiaries(list)
        }
    }

    fun updateBeneficiary(identifier: String, updated: BeneficiaryModel) {
        updateBeneficiary(identifier, updated.name, updated.account, updated.bank)
    }

    // Monthly Spending Limit
    var monthlySpendingLimit: Long
        get() = memoryMonthlySpendingLimit
        set(value) {
            memoryMonthlySpendingLimit = value.coerceAtLeast(0L)
            syncBalancesToFirebase()
        }

    fun getMonthlyExpense(): Long {
        return getTransactions()
            .filter { !it.isIncome }
            .sumOf { it.amount }
    }

    // User Cards CRUD
    fun initUserCards(realName: String, initialBalance: Long = 25000000L): MutableList<CardModel> {
        if (memoryCards.isNotEmpty()) return memoryCards
        val prefix = "4"
        val random15 = (1..15).map { (0..9).random() }.joinToString("")
        val cardNumber = (prefix + random15).chunked(4).joinToString(" ")
        val cards = mutableListOf(
            CardModel(
                id = "card-1",
                cardNumber = cardNumber,
                cardHolder = realName,
                expiry = "12/29",
                balance = initialBalance,
                cardType = "Debit",
                gradientType = 1,
                isBlocked = false
            )
        )
        saveCards(cards)
        return cards
    }

    fun getCards(): MutableList<CardModel> {
        return memoryCards
    }

    fun saveCards(cards: List<CardModel>, syncFirebase: Boolean = true) {
        memoryCards = cards.toMutableList()
        if (syncFirebase) {
            BackOfficeRepository.syncUserCardsToFirestore(appContext, cards)
        }
    }

    fun addCard(cardType: String = "Debit", gradientType: Int = 1): CardModel {
        val list = getCards()
        val prefix = if (cardType == "Debit") "4" else "5"
        val random15 = (1..15).map { (0..9).random() }.joinToString("")
        val cardNumber = (prefix + random15).chunked(4).joinToString(" ")
        val expMonth = (1..12).random().toString().padStart(2, '0')
        val expYear = (28..31).random().toString()
        val expiry = "$expMonth/$expYear"
        val newCard = CardModel(
            id = "card-${System.currentTimeMillis()}",
            cardNumber = cardNumber,
            cardHolder = userName.ifEmpty { "Nasabah" },
            expiry = expiry,
            balance = if (cardType == "Debit") debitBalance else 25000000L,
            cardType = cardType,
            gradientType = gradientType,
            isBlocked = false
        )
        list.add(0, newCard)
        saveCards(list)
        return newCard
    }

    fun toggleBlockCard(cardId: String): Boolean {
        val list = getCards()
        val card = list.find { it.id == cardId } ?: return false
        card.isBlocked = !card.isBlocked
        saveCards(list)
        return card.isBlocked
    }

    fun deleteCard(cardId: String): Boolean {
        val list = getCards()
        val removed = list.removeAll { it.id == cardId }
        if (removed) {
            saveCards(list)
            BackOfficeRepository.deleteUserCardFromFirestore(appContext, cardId)
        }
        return removed
    }

    // Saving Goals CRUD
    fun getSavingGoals(): MutableList<SavingGoalModel> {
        return memorySavingGoals
    }

    fun saveSavingGoals(list: List<SavingGoalModel>, syncFirebase: Boolean = true) {
        memorySavingGoals = list.toMutableList()
        if (syncFirebase) {
            BackOfficeRepository.syncSavingGoalsToFirestore(appContext, list)
        }
    }

    fun addSavingGoal(name: String, targetAmount: Long, durationMonths: Int): SavingGoalModel {
        val list = getSavingGoals()
        val monthly = if (durationMonths > 0) targetAmount / durationMonths else targetAmount
        val initialDeposit = kotlin.math.min(monthly, debitBalance)
        if (initialDeposit > 0) {
            addTransaction(
                title = "Setoran Tabungan: $name",
                category = "Tabungan",
                amount = initialDeposit,
                isIncome = false,
                counterparty = "Utomo Save Online"
            )
        }
        val newGoal = SavingGoalModel(
            id = "g-${System.currentTimeMillis()}",
            name = name,
            targetAmount = targetAmount,
            currentAmount = initialDeposit,
            durationMonths = durationMonths,
            monthlyDebit = monthly
        )
        list.add(0, newGoal)
        saveSavingGoals(list)
        return newGoal
    }

    fun updateSavingGoal(updated: SavingGoalModel) {
        val list = getSavingGoals()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            list[idx] = updated
            saveSavingGoals(list)
        }
    }

    fun deleteSavingGoal(goalId: String): Long {
        val list = getSavingGoals()
        val goal = list.find { it.id == goalId }
        var refundedAmount = 0L
        if (goal != null) {
            refundedAmount = goal.currentAmount
            if (refundedAmount > 0) {
                addTransaction(
                    title = "Pencairan Tabungan: ${goal.name}",
                    category = "Tabungan",
                    amount = refundedAmount,
                    isIncome = true,
                    counterparty = "Utomo Save Online"
                )
            }
            list.remove(goal)
            saveSavingGoals(list)
            BackOfficeRepository.deleteSavingGoalFromFirestore(appContext, goalId)
        }
        return refundedAmount
    }

    fun topUpSavingGoal(goalId: String, amount: Long): Boolean {
        if (amount <= 0 || debitBalance < amount) return false
        val list = getSavingGoals()
        val idx = list.indexOfFirst { it.id == goalId }
        if (idx != -1) {
            val current = list[idx]
            addTransaction(
                title = "Setoran Tabungan: ${current.name}",
                category = "Tabungan",
                amount = amount,
                isIncome = false,
                counterparty = "Utomo Save Online"
            )
            list[idx] = current.copy(currentAmount = current.currentAmount + amount)
            saveSavingGoals(list)
            return true
        }
        return false
    }

    fun getDefaultBranches(): List<BranchModel> {
        return listOf(
            BranchModel(
                id = "branch-1",
                name = "KC Sudirman Utama",
                type = "KC",
                address = "Jl. Jend. Sudirman Kav 21, Jakarta Pusat",
                city = "Jakarta Pusat",
                distance = "0.8 km",
                hours = "Buka: 08:00 - 15:30 • ATM 24 Jam",
                services = "Layanan Teller • ATM 24 Jam • Tarik Tunai",
                latitude = -6.2146,
                longitude = 106.8217,
                isCardlessSupported = true
            ),
            BranchModel(
                id = "branch-2",
                name = "KCP Plaza Senayan",
                type = "KCP",
                address = "Plaza Senayan Lt. LG Unit 12, Jakarta Pusat",
                city = "Jakarta Pusat",
                distance = "1.4 km",
                hours = "Buka: 10:00 - 19:00 • CDM & ATM Tersedia",
                services = "CDM & ATM Tersedia • Tarik Tunai",
                latitude = -6.2253,
                longitude = 106.7997,
                isCardlessSupported = true
            ),
            BranchModel(
                id = "branch-3",
                name = "ATM Center Grand Indonesia",
                type = "ATM",
                address = "East Mall Basement 1, Thamrin, Jakarta Pusat",
                city = "Jakarta Pusat",
                distance = "2.3 km",
                hours = "Buka 24 Jam • Mendukung Tarik Tunai Tanpa Kartu",
                services = "Mendukung Tarik Tunai Tanpa Kartu",
                latitude = -6.1950,
                longitude = 106.8210,
                isCardlessSupported = true
            ),
            BranchModel(
                id = "branch-4",
                name = "KC Thamrin Boulevard",
                type = "KC",
                address = "Jl. M.H. Thamrin No. 28-30, Jakarta Pusat",
                city = "Jakarta Pusat",
                distance = "3.1 km",
                hours = "Buka: 08:00 - 16:00 • ATM & Valas",
                services = "Layanan Teller • ATM 24 Jam • Penukaran Valas",
                latitude = -6.1895,
                longitude = 106.8236,
                isCardlessSupported = true
            ),
            BranchModel(
                id = "branch-5",
                name = "KCP Kuningan City",
                type = "KCP",
                address = "Kuningan City Mall Lt. G-08, Jakarta Selatan",
                city = "Jakarta Selatan",
                distance = "3.8 km",
                hours = "Buka: 09:00 - 18:00 • CDM Tersedia",
                services = "Setor Tarik Tunai • Konsultasi CS",
                latitude = -6.2238,
                longitude = 106.8288,
                isCardlessSupported = true
            ),
            BranchModel(
                id = "branch-6",
                name = "ATM Center Pacific Place",
                type = "ATM",
                address = "SCBD Lot 3-5, Jl. Jend. Sudirman, Jakarta Selatan",
                city = "Jakarta Selatan",
                distance = "4.5 km",
                hours = "Buka 24 Jam • Tarik & Setor Tunai",
                services = "Mendukung Tarik Tunai Tanpa Kartu",
                latitude = -6.2246,
                longitude = 106.8098,
                isCardlessSupported = true
            )
        )
    }

    fun getBranches(): List<BranchModel> {
        return memoryBranches
    }

    fun saveBranches(branches: List<BranchModel>) {
        memoryBranches = branches.toMutableList()
    }

    fun replaceCloudBranches(branches: List<BranchModel>) {
        memoryBranches = branches.toMutableList()
    }

    fun syncBranchesFromFirestore(onComplete: ((List<BranchModel>) -> Unit)? = null) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("branches")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = mutableListOf<BranchModel>()
                    for (doc in snapshot.documents) {
                        val branch = BranchModel(
                            id = doc.getString("id") ?: doc.id,
                            name = doc.getString("name") ?: "Cabang UTOMO",
                            type = doc.getString("type") ?: "KC",
                            address = doc.getString("address") ?: "",
                            city = doc.getString("city") ?: "Jakarta",
                            distance = doc.getString("distance") ?: "1.0 km",
                            hours = doc.getString("hours") ?: "08:00 - 15:30",
                            services = doc.getString("services") ?: "ATM 24 Jam",
                            latitude = doc.getDouble("latitude") ?: -6.2146,
                            longitude = doc.getDouble("longitude") ?: 106.8217,
                            isCardlessSupported = doc.getBoolean("isCardlessSupported") ?: true
                        )
                        list.add(branch)
                    }
                    if (list.isNotEmpty()) {
                        saveBranches(list)
                        onComplete?.invoke(list)
                        return@addOnSuccessListener
                    }
                }
                onComplete?.invoke(getBranches())
            }
            .addOnFailureListener {
                onComplete?.invoke(getBranches())
            }
    }
}
