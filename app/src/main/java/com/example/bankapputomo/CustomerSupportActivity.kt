package com.example.bankapputomo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.bankapputomo.model.TicketMessageModel
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerSupportActivity : BaseActivity() {

    private lateinit var chatScrollView: ScrollView
    private lateinit var chatDynamicMessagesContainer: LinearLayout
    private lateinit var inputChatMessage: EditText
    private lateinit var btnSendMessage: View
    private lateinit var btnFillTicket: MaterialButton
    private lateinit var layoutActiveTicketBanner: LinearLayout
    private lateinit var textActiveTicketSubject: TextView
    private lateinit var textActiveTicketCategory: TextView
    private lateinit var textActiveTicketStatus: TextView
    private lateinit var btnTicketHeaderAction: TextView
    private lateinit var layoutSuggestionChips: View

    private var activeTicketId: String? = null
    private var activeTicketSubject: String? = null
    private var activeTicketCategory: String? = null
    private var activeTicketStatus: String? = null
    private var activeTicketRating: Int? = null

    private var messagesListener: ListenerRegistration? = null
    private var ticketDocListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_support)

        chatScrollView = findViewById(R.id.chatScrollView)
        chatDynamicMessagesContainer = findViewById(R.id.chatDynamicMessagesContainer)
        inputChatMessage = findViewById(R.id.inputChatMessage)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnFillTicket = findViewById(R.id.btnFillTicket)
        layoutActiveTicketBanner = findViewById(R.id.layoutActiveTicketBanner)
        textActiveTicketSubject = findViewById(R.id.textActiveTicketSubject)
        textActiveTicketCategory = findViewById(R.id.textActiveTicketCategory)
        textActiveTicketStatus = findViewById(R.id.textActiveTicketStatus)
        btnTicketHeaderAction = findViewById(R.id.btnTicketHeaderAction)
        layoutSuggestionChips = findViewById(R.id.layoutSuggestionChips)

        findViewById<View>(R.id.btnSupportBack).bounceClick {
            finish()
        }

        // Ticket History Button in Top Bar
        findViewById<View>(R.id.btnTicketHistory).bounceClick {
            startActivity(Intent(this, SupportTicketsActivity::class.java))
        }

        // Call Hotline Button
        findViewById<View>(R.id.btnCallHotline).bounceClick {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1500888"))
            try {
                startActivity(callIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "UTOMO Bank Hotline: 1500888", Toast.LENGTH_LONG).show()
            }
        }

        // Suggestion Chips (Pre-fill ticket form when clicked)
        findViewById<TextView>(R.id.chipHelpTransfer).bounceClick {
            showNewTicketBottomSheet(
                prefillCategory = getString(R.string.ticket_category_tx),
                prefillSubject = "Pertanyaan Status Transaksi Terakhir"
            )
        }

        findViewById<TextView>(R.id.chipHelpCard).bounceClick {
            showNewTicketBottomSheet(
                prefillCategory = getString(R.string.ticket_category_card),
                prefillSubject = "Bantuan Kartu Debit / Pengaturan PIN"
            )
        }

        findViewById<TextView>(R.id.chipHelpRates).bounceClick {
            showNewTicketBottomSheet(
                prefillCategory = getString(R.string.ticket_category_other),
                prefillSubject = "Informasi Kurs Valas & Nilai Tukar"
            )
        }

        findViewById<TextView>(R.id.chipHelpHuman).bounceClick {
            showNewTicketBottomSheet(
                prefillCategory = getString(R.string.ticket_category_account),
                prefillSubject = "Bantuan Langsung Staf Customer Service"
            )
        }

        // Button to fill ticket (The primary and sole way for user to start/fill ticket)
        btnFillTicket.bounceClick {
            val prefillCat = intent.getStringExtra(EXTRA_PREFILL_CATEGORY)
            val prefillSub = intent.getStringExtra(EXTRA_PREFILL_SUBJECT)
            showNewTicketBottomSheet(prefillCat, prefillSub)
        }

        // Header Banner Action (Close, Reopen, Rate, or Start New)
        btnTicketHeaderAction.bounceClick {
            handleBannerActionClick()
        }

        // Send Chat Message Button (Enabled once ticket is created)
        btnSendMessage.bounceClick {
            val text = inputChatMessage.text.toString().trim()
            val tId = activeTicketId
            if (tId.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.support_input_hint_disabled), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            if (text.isNotEmpty()) {
                inputChatMessage.setText("")
                btnSendMessage.isEnabled = false
                BackOfficeRepository.sendCustomerMessage(
                    context = this,
                    ticketId = tId,
                    text = text,
                    onSuccess = {
                        btnSendMessage.isEnabled = true
                    },
                    onError = { e ->
                        btnSendMessage.isEnabled = true
                        Toast.makeText(this, "Gagal mengirim: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Check if an existing ticket was requested via Intent
        val explicitTicketId = intent.getStringExtra(EXTRA_TICKET_ID)
        val initialSubject = intent.getStringExtra(EXTRA_TICKET_SUBJECT)
        val initialCategory = intent.getStringExtra(EXTRA_TICKET_CATEGORY)
        val initialStatus = intent.getStringExtra(EXTRA_TICKET_STATUS)

        if (!explicitTicketId.isNullOrEmpty()) {
            loadTicket(explicitTicketId, initialSubject ?: "Tiket Dukungan", initialCategory ?: "-", initialStatus ?: "open")
        } else {
            // Check active ticket in local storage or Firestore
            checkAndLoadActiveTicket()
        }
    }

    private fun checkAndLoadActiveTicket() {
        val storedActiveId = AppStorage.get(this).activeSupportTicketId
        if (!storedActiveId.isNullOrEmpty()) {
            // Check if stored ticket is still valid
            BackOfficeRepository.firestore.collection("tickets").document(storedActiveId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val status = doc.getString("status") ?: "open"
                        if (status != "closed") {
                            loadTicket(
                                ticketId = doc.id,
                                subject = doc.getString("subject") ?: "Tiket Dukungan",
                                category = doc.getString("category") ?: "-",
                                status = status
                            )
                            return@addOnSuccessListener
                        }
                    }
                    // Stored ticket is closed or invalid, query active ticket from server
                    queryActiveTicketFromServer()
                }
                .addOnFailureListener {
                    queryActiveTicketFromServer()
                }
        } else {
            queryActiveTicketFromServer()
        }
    }

    private fun queryActiveTicketFromServer() {
        BackOfficeRepository.getActiveTicket(this) { activeTicket ->
            if (activeTicket != null && activeTicket.status != "closed") {
                loadTicket(
                    ticketId = activeTicket.id,
                    subject = activeTicket.subject,
                    category = activeTicket.category,
                    status = activeTicket.status
                )
            } else {
                // No active ticket: Keep in initial greeting state
                setInputEnabled(false)
                layoutActiveTicketBanner.visibility = View.GONE
                layoutSuggestionChips.visibility = View.VISIBLE
            }
        }
    }

    private fun loadTicket(ticketId: String, subject: String, category: String, status: String) {
        activeTicketId = ticketId
        activeTicketSubject = subject
        activeTicketCategory = category
        activeTicketStatus = status

        AppStorage.get(this).activeSupportTicketId = ticketId

        // Update greeting button state
        btnFillTicket.text = getString(R.string.support_ticket_sent_success)
        btnFillTicket.isEnabled = false
        btnFillTicket.alpha = 0.7f

        // Enable bottom input dock
        setInputEnabled(status != "closed")

        // Update banner
        updateBannerUI(ticketId, subject, category, status)

        // Hide chips to save vertical space for active conversation
        layoutSuggestionChips.visibility = View.GONE

        // Listen for real-time ticket metadata changes (status, rating)
        listenToTicketMetadata(ticketId)

        // Listen for real-time messages from customer & back-office admin
        listenToTicketMessages(ticketId)
    }

    private fun updateBannerUI(ticketId: String, subject: String, category: String, status: String) {
        layoutActiveTicketBanner.visibility = View.VISIBLE
        textActiveTicketSubject.text = "Tiket #${ticketId.take(8).uppercase()}: $subject"
        textActiveTicketCategory.text = "Kategori: $category"

        val statusText = when (status) {
            "open" -> getString(R.string.ticket_status_open)
            "in_progress" -> getString(R.string.ticket_status_in_progress)
            "waiting_customer" -> getString(R.string.ticket_status_waiting)
            "resolved" -> getString(R.string.ticket_status_resolved)
            "closed" -> getString(R.string.ticket_status_closed)
            else -> status
        }
        textActiveTicketStatus.text = statusText

        when (status) {
            "resolved" -> {
                textActiveTicketStatus.setBackgroundResource(R.drawable.bg_badge_success)
                btnTicketHeaderAction.text = if (activeTicketRating == null) "Beri Nilai" else "Selesai"
                btnTicketHeaderAction.visibility = View.VISIBLE
            }
            "closed" -> {
                textActiveTicketStatus.setBackgroundResource(R.drawable.bg_figma_tab_inactive)
                btnTicketHeaderAction.text = getString(R.string.support_new_ticket_session)
                btnTicketHeaderAction.visibility = View.VISIBLE
            }
            else -> {
                textActiveTicketStatus.setBackgroundResource(R.drawable.bg_tile_soft_blue)
                textActiveTicketStatus.setTextColor(ContextCompat.getColor(this, R.color.figma_primary))
                btnTicketHeaderAction.text = "Tutup Tiket"
                btnTicketHeaderAction.visibility = View.VISIBLE
            }
        }
    }

    private fun handleBannerActionClick() {
        val tId = activeTicketId ?: return
        when (activeTicketStatus) {
            "resolved" -> {
                if (activeTicketRating == null) {
                    showRatingBottomSheet(tId)
                } else {
                    promptCloseOrNewSession()
                }
            }
            "closed" -> {
                startNewChatSession()
            }
            else -> {
                AlertDialog.Builder(this)
                    .setTitle("Tutup Tiket?")
                    .setMessage("Apakah Anda ingin menutup tiket bantuan ini sekarang?")
                    .setPositiveButton("Ya, Tutup") { _, _ ->
                        BackOfficeRepository.updateTicketStatus(tId, "closed") {
                            Toast.makeText(this, "Tiket ditutup.", Toast.LENGTH_SHORT).show()
                            activeTicketStatus = "closed"
                            updateBannerUI(tId, activeTicketSubject ?: "", activeTicketCategory ?: "", "closed")
                            setInputEnabled(false)
                        }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
    }

    private fun promptCloseOrNewSession() {
        AlertDialog.Builder(this)
            .setTitle("Sesi Percakapan")
            .setMessage("Tiket ini telah selesai. Ingin memulai sesi pertanyaan baru?")
            .setPositiveButton("Mulai Sesi Baru") { _, _ ->
                startNewChatSession()
            }
            .setNegativeButton("Tetap di Sini", null)
            .show()
    }

    private fun startNewChatSession() {
        activeTicketId = null
        activeTicketSubject = null
        activeTicketCategory = null
        activeTicketStatus = null
        AppStorage.get(this).activeSupportTicketId = null

        messagesListener?.remove()
        ticketDocListener?.remove()

        chatDynamicMessagesContainer.removeAllViews()
        layoutActiveTicketBanner.visibility = View.GONE
        layoutSuggestionChips.visibility = View.VISIBLE

        btnFillTicket.text = getString(R.string.support_btn_fill_ticket)
        btnFillTicket.isEnabled = true
        btnFillTicket.alpha = 1.0f

        setInputEnabled(false)

        Toast.makeText(this, "Silakan isi tiket baru untuk memulai chat.", Toast.LENGTH_SHORT).show()
        scrollToBottom()
    }

    private fun setInputEnabled(enabled: Boolean) {
        inputChatMessage.isEnabled = enabled
        if (enabled) {
            inputChatMessage.hint = getString(R.string.support_input_hint_enabled)
            btnSendMessage.isEnabled = true
            btnSendMessage.alpha = 1.0f
        } else {
            inputChatMessage.hint = getString(R.string.support_input_hint_disabled)
            btnSendMessage.isEnabled = false
            btnSendMessage.alpha = 0.4f
        }
    }

    private fun listenToTicketMetadata(ticketId: String) {
        ticketDocListener?.remove()
        ticketDocListener = BackOfficeRepository.firestore.collection("tickets").document(ticketId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val status = snapshot.getString("status") ?: "open"
                val rating = snapshot.getLong("rating")?.toInt()
                activeTicketStatus = status
                activeTicketRating = rating

                updateBannerUI(
                    ticketId = ticketId,
                    subject = snapshot.getString("subject") ?: (activeTicketSubject ?: ""),
                    category = snapshot.getString("category") ?: (activeTicketCategory ?: ""),
                    status = status
                )

                if (status == "resolved" && rating == null && !isFinishing) {
                    showRatingBottomSheet(ticketId)
                }

                if (status == "closed") {
                    setInputEnabled(false)
                }
            }
    }

    private fun listenToTicketMessages(ticketId: String) {
        messagesListener?.remove()
        messagesListener = BackOfficeRepository.listenToTicketMessages(ticketId) { list ->
            renderMessages(list)
        }
    }

    private fun renderMessages(messages: List<TicketMessageModel>) {
        chatDynamicMessagesContainer.removeAllViews()

        for (msg in messages) {
            val timeStr = if (msg.createdAt != null) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(msg.createdAt.toDate())
            } else {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            }

            val isStaffMessage = msg.senderType == "staff" || msg.senderRole == "staff"
            if (isStaffMessage) {
                val displayName = msg.senderName.ifEmpty { "Customer Service" }
                addIncomingBubble(
                    senderName = displayName,
                    message = msg.text,
                    timeStr = timeStr,
                    isBot = msg.senderId == "system_bot" || msg.senderName.contains("Auto-Reply", ignoreCase = true)
                )
            } else {
                addOutgoingBubble(
                    message = msg.text,
                    timeStr = timeStr
                )
            }
        }

        scrollToBottom()
    }

    private fun addOutgoingBubble(message: String, timeStr: String) {
        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@CustomerSupportActivity, R.drawable.bg_chat_bubble_outgoing)
            setPadding(36, 26, 36, 26)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                setMargins(100, 8, 0, 8)
            }
        }

        val textMsg = TextView(this).apply {
            text = message
            setTextColor(ContextCompat.getColor(this@CustomerSupportActivity, R.color.white))
            textSize = 13f
            typeface = ResourcesCompat.getFont(this@CustomerSupportActivity, R.font.poppins_regular)
            setLineSpacing(6f, 1f)
        }

        val textTime = TextView(this).apply {
            text = timeStr
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 10f
            gravity = Gravity.END
            setPadding(0, 6, 0, 0)
            typeface = ResourcesCompat.getFont(this@CustomerSupportActivity, R.font.poppins_regular)
        }

        bubbleLayout.addView(textMsg)
        bubbleLayout.addView(textTime)

        chatDynamicMessagesContainer.addView(bubbleLayout)
    }

    private fun addIncomingBubble(senderName: String, message: String, timeStr: String, isBot: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        val avatar = ImageView(this).apply {
            val size = (36 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 0, (10 * resources.displayMetrics.density).toInt(), 0)
            }
            setImageResource(R.drawable.ic_support_headset)
            setBackgroundResource(if (isBot) R.drawable.bg_tile_soft_blue else R.drawable.bg_tile_soft_green)
            setPadding(16, 16, 16, 16)
        }

        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@CustomerSupportActivity, R.drawable.bg_chat_bubble_incoming)
            setPadding(36, 26, 36, 26)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 80
            }
        }

        val textSender = TextView(this).apply {
            text = senderName
            setTextColor(ContextCompat.getColor(this@CustomerSupportActivity, if (isBot) R.color.figma_primary else R.color.bank_success))
            textSize = 11f
            typeface = ResourcesCompat.getFont(this@CustomerSupportActivity, R.font.poppins_semibold)
            setPadding(0, 0, 0, 4)
        }

        val textMsg = TextView(this).apply {
            text = message
            setTextColor(ContextCompat.getColor(this@CustomerSupportActivity, R.color.figma_text_title))
            textSize = 13f
            typeface = ResourcesCompat.getFont(this@CustomerSupportActivity, R.font.poppins_regular)
            setLineSpacing(6f, 1f)
        }

        val textTime = TextView(this).apply {
            text = timeStr
            setTextColor(ContextCompat.getColor(this@CustomerSupportActivity, R.color.figma_text_muted))
            textSize = 10f
            gravity = Gravity.END
            setPadding(0, 6, 0, 0)
            typeface = ResourcesCompat.getFont(this@CustomerSupportActivity, R.font.poppins_regular)
        }

        bubbleLayout.addView(textSender)
        bubbleLayout.addView(textMsg)
        bubbleLayout.addView(textTime)

        row.addView(avatar)
        row.addView(bubbleLayout)

        chatDynamicMessagesContainer.addView(row)
    }

    private fun showNewTicketBottomSheet(prefillCategory: String? = null, prefillSubject: String? = null) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_new_ticket, null)
        dialog.setContentView(sheetView)

        val spinnerCategory = sheetView.findViewById<Spinner>(R.id.spinnerTicketCategory)
        val inputSubject = sheetView.findViewById<EditText>(R.id.inputTicketSubject)
        val inputMessage = sheetView.findViewById<EditText>(R.id.inputTicketMessage)
        val btnSubmit = sheetView.findViewById<MaterialButton>(R.id.btnSubmitTicket)

        val categories = listOf(
            getString(R.string.ticket_category_card),
            getString(R.string.ticket_category_tx),
            getString(R.string.ticket_category_account),
            getString(R.string.ticket_category_bill),
            getString(R.string.ticket_category_qris),
            getString(R.string.ticket_category_app),
            getString(R.string.ticket_category_other)
        )

        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = catAdapter

        if (!prefillCategory.isNullOrEmpty()) {
            val idx = categories.indexOfFirst { it.equals(prefillCategory, ignoreCase = true) }
            if (idx >= 0) spinnerCategory.setSelection(idx)
        }

        if (!prefillSubject.isNullOrEmpty()) {
            inputSubject.setText(prefillSubject)
        }

        btnSubmit.bounceClick {
            val subject = inputSubject.text.toString().trim()
            val message = inputMessage.text.toString().trim()
            val category = spinnerCategory.selectedItem?.toString() ?: "Masalah Kartu"

            if (subject.isEmpty()) {
                inputSubject.error = "Subjek tidak boleh kosong"
                inputSubject.requestFocus()
                return@bounceClick
            }

            if (message.isEmpty()) {
                inputMessage.error = "Detail kendala tidak boleh kosong"
                inputMessage.requestFocus()
                return@bounceClick
            }

            btnSubmit.isEnabled = false
            btnSubmit.text = getString(R.string.str_mohon_tunggu)

            BackOfficeRepository.createTicket(
                context = this,
                subject = subject,
                category = category,
                initialMessage = message,
                onSuccess = { ticketId ->
                    dialog.dismiss()
                    Toast.makeText(this, getString(R.string.ticket_created_success), Toast.LENGTH_SHORT).show()
                    loadTicket(ticketId, subject, category, "open")
                },
                onError = { e ->
                    btnSubmit.isEnabled = true
                    btnSubmit.text = getString(R.string.str_kirim)
                    Toast.makeText(this, "Gagal membuat tiket: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun showRatingBottomSheet(ticketId: String) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_ticket_rating, null)
        dialog.setContentView(view)

        var selectedRating = 5
        val stars = listOf(
            view.findViewById<ImageView>(R.id.star1),
            view.findViewById<ImageView>(R.id.star2),
            view.findViewById<ImageView>(R.id.star3),
            view.findViewById<ImageView>(R.id.star4),
            view.findViewById<ImageView>(R.id.star5)
        )

        fun updateStars(rating: Int) {
            selectedRating = rating
            stars.forEachIndexed { index, star ->
                if (index < rating) {
                    star.setImageResource(R.drawable.ic_star_filled)
                } else {
                    star.setImageResource(R.drawable.ic_star_outline)
                }
            }
        }

        updateStars(5)

        stars.forEachIndexed { index, star ->
            star.bounceClick { updateStars(index + 1) }
        }

        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmitRating)
        btnSubmit.bounceClick {
            btnSubmit.isEnabled = false
            BackOfficeRepository.submitTicketRating(ticketId, selectedRating) {
                dialog.dismiss()
                activeTicketRating = selectedRating
                btnTicketHeaderAction.text = "Selesai"
                Toast.makeText(this, getString(R.string.ticket_rating_thanks), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun scrollToBottom() {
        chatScrollView.post {
            chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
        ticketDocListener?.remove()
    }

    companion object {
        const val EXTRA_TICKET_ID = "ticket_id"
        const val EXTRA_TICKET_SUBJECT = "ticket_subject"
        const val EXTRA_TICKET_CATEGORY = "ticket_category"
        const val EXTRA_TICKET_STATUS = "ticket_status"
        const val EXTRA_PREFILL_CATEGORY = "prefill_category"
        const val EXTRA_PREFILL_SUBJECT = "prefill_subject"
    }
}
