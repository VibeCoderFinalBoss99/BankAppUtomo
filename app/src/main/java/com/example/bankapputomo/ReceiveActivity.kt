package com.example.bankapputomo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.FileShareHelper
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.QrCodeGenerator
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class ReceiveActivity : AppCompatActivity() {

    private lateinit var cardQrCaptureArea: View
    private lateinit var imgReceiveQr: ImageView
    private lateinit var textReceiveAccountHolder: TextView
    private lateinit var textReceiveAccountNumber: TextView
    private lateinit var iconCopyState: ImageView
    private lateinit var textCopiedFeedback: TextView
    private lateinit var containerRequestedAmount: LinearLayout
    private lateinit var textRequestedAmount: TextView
    private lateinit var textRequestedNote: TextView

    // Expandable inputs
    private lateinit var btnToggleRequestAmount: View
    private lateinit var arrowToggleRequest: ImageView
    private lateinit var containerAmountInputs: LinearLayout
    private lateinit var inputRequestAmount: EditText
    private lateinit var inputRequestNote: EditText

    private var currentAmount: Long = 0L
    private var currentNote: String = ""
    private var isExpanded = false

    private val accountNumber = "12788989390"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive)

        initViews()
        setupUserData()
        renderQrCode()
        setupListeners()
    }

    private fun initViews() {
        cardQrCaptureArea = findViewById(R.id.cardQrCaptureArea)
        imgReceiveQr = findViewById(R.id.imgReceiveQr)
        textReceiveAccountHolder = findViewById(R.id.textReceiveAccountHolder)
        textReceiveAccountNumber = findViewById(R.id.textReceiveAccountNumber)
        iconCopyState = findViewById(R.id.iconCopyState)
        textCopiedFeedback = findViewById(R.id.textCopiedFeedback)
        containerRequestedAmount = findViewById(R.id.containerRequestedAmount)
        textRequestedAmount = findViewById(R.id.textRequestedAmount)
        textRequestedNote = findViewById(R.id.textRequestedNote)

        btnToggleRequestAmount = findViewById(R.id.btnToggleRequestAmount)
        arrowToggleRequest = findViewById(R.id.arrowToggleRequest)
        containerAmountInputs = findViewById(R.id.containerAmountInputs)
        inputRequestAmount = findViewById(R.id.inputRequestAmount)
        inputRequestNote = findViewById(R.id.inputRequestNote)
    }

    private fun setupUserData() {
        val storage = AppStorage.get()
        val user = storage.userName.ifEmpty { "Nasabah" }
        textReceiveAccountHolder.text = user
        textReceiveAccountNumber.text = formatAccountNumber(accountNumber)
    }

    private fun formatAccountNumber(raw: String): String {
        return raw.chunked(4).joinToString(" ")
    }

    private fun renderQrCode() {
        val user = AppStorage.get().userName.ifEmpty { "NASABAH" }
        val payload = buildQrisPayload(user, accountNumber, currentAmount, currentNote)
        val qrBitmap = QrCodeGenerator.generateQrBitmap(
            content = payload,
            width = 512,
            height = 512,
            darkColor = ContextCompat.getColor(this, R.color.figma_text_title),
            lightColor = Color.WHITE
        )
        imgReceiveQr.setImageBitmap(qrBitmap)

        if (currentAmount > 0) {
            containerRequestedAmount.visibility = View.VISIBLE
            textRequestedAmount.text = "Nominal Ditentukan: ${IdrFormatter.format(currentAmount)}"
            if (currentNote.isNotEmpty()) {
                textRequestedNote.visibility = View.VISIBLE
                textRequestedNote.text = "Catatan: $currentNote"
            } else {
                textRequestedNote.visibility = View.GONE
            }
        } else {
            containerRequestedAmount.visibility = View.GONE
        }
    }

    private fun buildQrisPayload(name: String, acc: String, amount: Long, note: String): String {
        val base = "00020101021226600016ID.CO.BANKUTOMO.WWW0118$acc" +
                "5204581253033605802ID59${name.length.toString().padStart(2, '0')}$name" +
                "6007JAKARTA"
        return if (amount > 0) {
            val amtStr = amount.toString()
            "$base" + "54${amtStr.length.toString().padStart(2, '0')}$amtStr" +
                    (if (note.isNotEmpty()) "62${note.length.toString().padStart(2, '0')}$note" else "")
        } else {
            base
        }
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnReceiveBack).bounceClick {
            finish()
        }

        // Tap to copy account number
        findViewById<View>(R.id.btnQuickCopyAccount).bounceClick {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("No. Rekening UTOMO Bank", accountNumber))

            iconCopyState.setImageResource(R.drawable.ic_figma_chip)
            textCopiedFeedback.text = "Tersalin!"
            textCopiedFeedback.setTextColor(ContextCompat.getColor(this, R.color.bank_success))

            Toast.makeText(this, "Nomor rekening berhasil disalin", Toast.LENGTH_SHORT).show()

            findViewById<View>(R.id.btnQuickCopyAccount).postDelayed({
                iconCopyState.setImageResource(R.drawable.ic_figma_copy)
                textCopiedFeedback.text = "Salin"
                textCopiedFeedback.setTextColor(ContextCompat.getColor(this, R.color.figma_primary))
            }, 2000)
        }

        // Toggle request amount section
        btnToggleRequestAmount.bounceClick {
            isExpanded = !isExpanded
            containerAmountInputs.visibility = if (isExpanded) View.VISIBLE else View.GONE
            arrowToggleRequest.animate().rotation(if (isExpanded) 180f else 0f).setDuration(200).start()
        }

        // Auto dot formatter on amount input
        inputRequestAmount.addTextChangedListener(
            IdrFormatter.createWatcher(inputRequestAmount) { raw ->
                currentAmount = raw
            }
        )

        // Quick amount chips
        findViewById<View>(R.id.chipAmount50k).bounceClick {
            currentAmount = 50000L
            inputRequestAmount.setText(IdrFormatter.format(50000L).replace("Rp ", ""))
        }
        findViewById<View>(R.id.chipAmount100k).bounceClick {
            currentAmount = 100000L
            inputRequestAmount.setText(IdrFormatter.format(100000L).replace("Rp ", ""))
        }
        findViewById<View>(R.id.chipAmount250k).bounceClick {
            currentAmount = 250000L
            inputRequestAmount.setText(IdrFormatter.format(250000L).replace("Rp ", ""))
        }

        // Apply request amount to QR
        findViewById<View>(R.id.btnApplyRequestAmount).bounceClick {
            currentNote = inputRequestNote.text.toString().trim()
            renderQrCode()
            isExpanded = false
            containerAmountInputs.visibility = View.GONE
            arrowToggleRequest.animate().rotation(0f).setDuration(200).start()
            Toast.makeText(this, "Kode QR diperbarui dengan nominal!", Toast.LENGTH_SHORT).show()
        }

        // Reset request amount
        findViewById<View>(R.id.btnResetRequestAmount).bounceClick {
            currentAmount = 0L
            currentNote = ""
            inputRequestAmount.setText("")
            inputRequestNote.setText("")
            renderQrCode()
            Toast.makeText(this, "Nominal dihapus dari QR", Toast.LENGTH_SHORT).show()
        }

        // Share QR Code Image Sheet
        findViewById<View>(R.id.btnShareQrImage).bounceClick {
            val bitmap = FileShareHelper.captureViewToBitmap(cardQrCaptureArea)
            FileShareHelper.shareBitmap(this, bitmap, "Bagikan QR Terima Dana UTOMO Bank")
        }

        // Save QR Image to Gallery
        findViewById<View>(R.id.btnSaveQrGallery).bounceClick {
            val bitmap = FileShareHelper.captureViewToBitmap(cardQrCaptureArea)
            FileShareHelper.saveBitmapToGallery(this, bitmap, "BankUtomo_QRIS_Receive")
        }
    }
}
