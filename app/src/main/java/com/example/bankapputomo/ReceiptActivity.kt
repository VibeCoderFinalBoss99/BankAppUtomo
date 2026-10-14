package com.example.bankapputomo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.FileShareHelper
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.LottieMotionHelper
import com.example.bankapputomo.util.TransactionModel
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class ReceiptActivity : AppCompatActivity() {

    private lateinit var cardReceiptCaptureArea: View
    private lateinit var lottieReceiptStatus: LottieAnimationView
    private lateinit var textReceiptStatus: TextView
    private lateinit var textReceiptTotalAmount: TextView
    private lateinit var textReceiptDateTime: TextView
    private lateinit var textReceiptType: TextView
    private lateinit var textReceiptRefNo: TextView
    private lateinit var textReceiptSender: TextView
    private lateinit var textReceiptRecipient: TextView
    private lateinit var textReceiptRecipientAccount: TextView
    private lateinit var textReceiptBank: TextView
    private lateinit var textReceiptNote: TextView
    private lateinit var textReceiptBaseAmount: TextView
    private lateinit var textReceiptAdminFee: TextView
    private lateinit var textReceiptBreakdownTotal: TextView
    private lateinit var rowReceiptQrisDetails: RelativeLayout
    private lateinit var textReceiptQrisDetails: TextView

    private var originFeature: String? = null
    private var referenceNo: String = ""

    companion object {
        const val EXTRA_TX_ID = "extra_tx_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_FEE = "extra_fee"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_REF_NO = "extra_ref_no"
        const val EXTRA_SENDER_NAME = "extra_sender_name"
        const val EXTRA_SENDER_ACCOUNT = "extra_sender_account"
        const val EXTRA_RECIPIENT_NAME = "extra_recipient_name"
        const val EXTRA_RECIPIENT_ACCOUNT = "extra_recipient_account"
        const val EXTRA_BANK_NAME = "extra_bank_name"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_MERCHANT_DETAILS = "extra_merchant_details"
        const val EXTRA_ORIGIN = "extra_origin"

        fun start(
            context: Context,
            title: String,
            category: String,
            amount: Long,
            fee: Long = 0L,
            status: String = "SUCCESS",
            counterparty: String = "",
            recipientAccount: String = "",
            bankName: String = "UTOMO Bank",
            note: String = "",
            merchantDetails: String = "",
            origin: String? = null,
            customRefNo: String? = null
        ) {
            val intent = Intent(context, ReceiptActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_FEE, fee)
                putExtra(EXTRA_STATUS, status)
                putExtra(EXTRA_RECIPIENT_NAME, counterparty)
                putExtra(EXTRA_RECIPIENT_ACCOUNT, recipientAccount)
                putExtra(EXTRA_BANK_NAME, bankName)
                putExtra(EXTRA_NOTE, note)
                putExtra(EXTRA_MERCHANT_DETAILS, merchantDetails)
                putExtra(EXTRA_ORIGIN, origin)
                if (customRefNo != null) putExtra(EXTRA_REF_NO, customRefNo)
            }
            context.startActivity(intent)
        }

        fun startFromTransaction(context: Context, tx: TransactionModel) {
            val intent = Intent(context, ReceiptActivity::class.java).apply {
                putExtra(EXTRA_TX_ID, tx.id)
                putExtra(EXTRA_TITLE, tx.title)
                putExtra(EXTRA_CATEGORY, tx.category)
                putExtra(EXTRA_AMOUNT, tx.amount)
                putExtra(EXTRA_DATE, tx.date)
                putExtra(EXTRA_REF_NO, tx.referenceNo)
                putExtra(EXTRA_RECIPIENT_NAME, tx.counterparty)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        initViews()
        bindData()
        setupListeners()
    }

    private fun initViews() {
        cardReceiptCaptureArea = findViewById(R.id.cardReceiptCaptureArea)
        lottieReceiptStatus = findViewById(R.id.lottieReceiptStatus)
        textReceiptStatus = findViewById(R.id.textReceiptStatus)
        textReceiptTotalAmount = findViewById(R.id.textReceiptTotalAmount)
        textReceiptDateTime = findViewById(R.id.textReceiptDateTime)
        textReceiptType = findViewById(R.id.textReceiptType)
        textReceiptRefNo = findViewById(R.id.textReceiptRefNo)
        textReceiptSender = findViewById(R.id.textReceiptSender)
        textReceiptRecipient = findViewById(R.id.textReceiptRecipient)
        textReceiptRecipientAccount = findViewById(R.id.textReceiptRecipientAccount)
        textReceiptBank = findViewById(R.id.textReceiptBank)
        textReceiptNote = findViewById(R.id.textReceiptNote)
        textReceiptBaseAmount = findViewById(R.id.textReceiptBaseAmount)
        textReceiptAdminFee = findViewById(R.id.textReceiptAdminFee)
        textReceiptBreakdownTotal = findViewById(R.id.textReceiptBreakdownTotal)
        rowReceiptQrisDetails = findViewById(R.id.rowReceiptQrisDetails)
        textReceiptQrisDetails = findViewById(R.id.textReceiptQrisDetails)
    }

    private fun bindData() {
        val storage = AppStorage.get()
        val user = storage.userName.ifEmpty { "Nasabah" }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Transaksi UTOMO Bank"
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Transfer"
        val amount = intent.getLongExtra(EXTRA_AMOUNT, 0L)
        val fee = intent.getLongExtra(EXTRA_FEE, 0L)
        val total = amount + fee
        val status = intent.getStringExtra(EXTRA_STATUS) ?: "SUCCESS"

        val date = intent.getStringExtra(EXTRA_DATE) ?: java.text.SimpleDateFormat(
            "dd MMM yyyy, HH:mm 'WIB'",
            java.util.Locale("id", "ID")
        ).format(java.util.Date())

        referenceNo = intent.getStringExtra(EXTRA_REF_NO)
            ?: "TRX-${System.currentTimeMillis().toString().takeLast(8)}"

        val senderName = intent.getStringExtra(EXTRA_SENDER_NAME) ?: user
        val senderAccount = intent.getStringExtra(EXTRA_SENDER_ACCOUNT) ?: "12788989390"
        val recipientName = intent.getStringExtra(EXTRA_RECIPIENT_NAME).orEmpty().ifEmpty { "Penerima" }
        val recipientAccount = intent.getStringExtra(EXTRA_RECIPIENT_ACCOUNT).orEmpty()
        val bankName = intent.getStringExtra(EXTRA_BANK_NAME) ?: "UTOMO Bank"
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val merchantDetails = intent.getStringExtra(EXTRA_MERCHANT_DETAILS).orEmpty()
        originFeature = intent.getStringExtra(EXTRA_ORIGIN)

        // Set Lottie and Status
        if (status.equals("SUCCESS", ignoreCase = true)) {
            textReceiptStatus.text = "TRANSAKSI BERHASIL"
            textReceiptStatus.setBackgroundResource(R.drawable.bg_tile_soft_green)
            textReceiptStatus.setTextColor(ContextCompat.getColor(this, R.color.bank_success))

            lottieReceiptStatus.setAnimation("anim_success.json")
            LottieMotionHelper.recolor(lottieReceiptStatus, ContextCompat.getColor(this, R.color.bank_success))
            LottieMotionHelper.playOnce(lottieReceiptStatus)
        } else {
            textReceiptStatus.text = "TRANSAKSI GAGAL"
            textReceiptStatus.setBackgroundResource(R.drawable.bg_tile_soft_red)
            textReceiptStatus.setTextColor(ContextCompat.getColor(this, R.color.bank_red))

            lottieReceiptStatus.setAnimation("anim_failed.json")
            LottieMotionHelper.recolor(lottieReceiptStatus, ContextCompat.getColor(this, R.color.bank_red))
            LottieMotionHelper.playOnce(lottieReceiptStatus)
        }

        textReceiptTotalAmount.text = IdrFormatter.format(total)
        textReceiptDateTime.text = date
        textReceiptType.text = "$category • $title"
        textReceiptRefNo.text = referenceNo

        // Masked sender account (e.g., Push Puttichai • **** 7890)
        val maskedSender = if (senderAccount.length >= 4) "**** ${senderAccount.takeLast(4)}" else senderAccount
        textReceiptSender.text = "$senderName ($maskedSender)"

        textReceiptRecipient.text = recipientName

        if (recipientAccount.isNotEmpty()) {
            findViewById<View>(R.id.rowReceiptRecipientAccount).visibility = View.VISIBLE
            val maskedRecipient = if (recipientAccount.length >= 4) {
                "${recipientAccount.take(4)} **** ${recipientAccount.takeLast(4)}"
            } else recipientAccount
            textReceiptRecipientAccount.text = maskedRecipient
        } else {
            findViewById<View>(R.id.rowReceiptRecipientAccount).visibility = View.GONE
        }

        textReceiptBank.text = bankName

        val imageReceiptBankLogo = findViewById<ImageView>(R.id.imageReceiptBankLogo)
        val ewalletMatch = com.example.bankapputomo.util.EWalletSelectionBottomSheet.ALL_EWALLETS.firstOrNull {
            bankName.contains(it.name, ignoreCase = true) ||
            title.contains(it.name, ignoreCase = true) ||
            recipientName.contains(it.name, ignoreCase = true)
        }
        if (ewalletMatch != null) {
            imageReceiptBankLogo?.setImageResource(ewalletMatch.logoRes)
            imageReceiptBankLogo?.visibility = View.VISIBLE
        } else {
            imageReceiptBankLogo?.visibility = View.GONE
        }

        // QRIS details
        if (merchantDetails.isNotEmpty() || category.contains("QRIS", ignoreCase = true)) {
            rowReceiptQrisDetails.visibility = View.VISIBLE
            textReceiptQrisDetails.text = merchantDetails.ifEmpty { "ID1020021982901 • Standar QRIS MPM" }
        } else {
            rowReceiptQrisDetails.visibility = View.GONE
        }

        if (note.isNotEmpty()) {
            findViewById<View>(R.id.rowReceiptNote).visibility = View.VISIBLE
            textReceiptNote.text = note
        } else {
            findViewById<View>(R.id.rowReceiptNote).visibility = View.GONE
        }

        textReceiptBaseAmount.text = IdrFormatter.format(amount)
        textReceiptAdminFee.text = if (fee > 0) IdrFormatter.format(fee) else "Gratis (Rp 0)"
        textReceiptBreakdownTotal.text = IdrFormatter.format(total)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnReceiptBack).bounceClick {
            finish()
        }

        findViewById<View>(R.id.btnReceiptCopyRef).bounceClick {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("No Referensi", referenceNo))
            Toast.makeText(this, "Nomor referensi disalin", Toast.LENGTH_SHORT).show()
        }

        // 1. Share as Image
        findViewById<View>(R.id.btnShareReceiptImage).bounceClick {
            val bitmap = FileShareHelper.captureViewToBitmap(cardReceiptCaptureArea)
            FileShareHelper.shareBitmap(this, bitmap, "Bagikan Bukti Transaksi")
        }

        // 2. Share as PDF
        findViewById<View>(R.id.btnShareReceiptPdf).bounceClick {
            FileShareHelper.shareViewAsPdf(this, cardReceiptCaptureArea, "Bagikan Bukti PDF")
        }

        // 3. Save to Gallery
        findViewById<View>(R.id.btnSaveReceiptGallery).bounceClick {
            val bitmap = FileShareHelper.captureViewToBitmap(cardReceiptCaptureArea)
            FileShareHelper.saveBitmapToGallery(this, bitmap, "BankUtomo_Receipt")
        }

        // 4. Selesai (Done)
        findViewById<View>(R.id.btnReceiptDone).bounceClick {
            finish()
        }

        // 5. Transaksi Lagi (Make another transaction)
        val btnAnother = findViewById<TextView>(R.id.btnReceiptAnother)
        btnAnother.bounceClick {
            when (originFeature) {
                "TRANSFER" -> startActivity(Intent(this, TransferActivity::class.java))
                "BILL" -> startActivity(Intent(this, BillPaymentActivity::class.java))
                "PULSA" -> startActivity(Intent(this, MobilePrepaidActivity::class.java))
                "WITHDRAW" -> startActivity(Intent(this, WithdrawActivity::class.java))
                "SAVE" -> startActivity(Intent(this, SaveOnlineActivity::class.java))
                "EXCHANGE" -> startActivity(Intent(this, ExchangeActivity::class.java))
                else -> finish()
            }
            finish()
        }
    }
}
