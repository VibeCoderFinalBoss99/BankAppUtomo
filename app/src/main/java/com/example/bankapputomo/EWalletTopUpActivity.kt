package com.example.bankapputomo

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class EWalletTopUpActivity : BaseActivity() {

    enum class EWalletProvider(val displayName: String, val logoRes: Int, val brandColorHex: String) {
        SHOPEEPAY("ShopeePay", R.drawable.ic_ewallet_shopeepay, "#EE4D2D"),
        DANA("DANA", R.drawable.ic_ewallet_dana, "#118EEA"),
        GOPAY("GoPay", R.drawable.ic_ewallet_gopay, "#00AED6"),
        OVO("OVO", R.drawable.ic_ewallet_ovo, "#4C3494"),
        LINKAJA("LinkAja", R.drawable.ic_ewallet_linkaja, "#ED1C24")
    }

    private var selectedProvider = EWalletProvider.SHOPEEPAY
    private var selectedNominalAmount: Long = 100000L
    private val adminFee: Long = 1000L

    private lateinit var inputPhoneNumber: EditText
    private lateinit var textSourceBalance: TextView
    private lateinit var textTotalPayment: TextView
    private lateinit var btnContinuePayment: TextView

    // Provider buttons
    private lateinit var btnShopeePay: LinearLayout
    private lateinit var btnDana: LinearLayout
    private lateinit var btnGopay: LinearLayout
    private lateinit var btnOvo: LinearLayout
    private lateinit var btnLinkAja: LinearLayout

    // Nominal cards
    private lateinit var cardNominal20: LinearLayout
    private lateinit var cardNominal50: LinearLayout
    private lateinit var cardNominal100: LinearLayout
    private lateinit var cardNominal200: LinearLayout
    private lateinit var cardNominal300: LinearLayout
    private lateinit var cardNominal500: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ewallet_top_up)

        findViewById<View>(R.id.btnBack).bounceClick { finish() }

        inputPhoneNumber = findViewById(R.id.inputPhoneNumber)
        textSourceBalance = findViewById(R.id.textSourceBalance)
        textTotalPayment = findViewById(R.id.textTotalPayment)
        btnContinuePayment = findViewById(R.id.btnContinuePayment)

        btnShopeePay = findViewById(R.id.btnProviderShopeePay)
        btnDana = findViewById(R.id.btnProviderDana)
        btnGopay = findViewById(R.id.btnProviderGopay)
        btnOvo = findViewById(R.id.btnProviderOvo)
        btnLinkAja = findViewById(R.id.btnProviderLinkAja)

        cardNominal20 = findViewById(R.id.cardNominal20)
        cardNominal50 = findViewById(R.id.cardNominal50)
        cardNominal100 = findViewById(R.id.cardNominal100)
        cardNominal200 = findViewById(R.id.cardNominal200)
        cardNominal300 = findViewById(R.id.cardNominal300)
        cardNominal500 = findViewById(R.id.cardNominal500)

        // Setup E-Wallet provider click listeners
        btnShopeePay.bounceClick { selectProvider(EWalletProvider.SHOPEEPAY) }
        btnDana.bounceClick { selectProvider(EWalletProvider.DANA) }
        btnGopay.bounceClick { selectProvider(EWalletProvider.GOPAY) }
        btnOvo.bounceClick { selectProvider(EWalletProvider.OVO) }
        btnLinkAja.bounceClick { selectProvider(EWalletProvider.LINKAJA) }

        // Setup Recent Contacts
        findViewById<View>(R.id.recentContact1).bounceClick {
            inputPhoneNumber.setText("08123456789")
            inputPhoneNumber.setSelection(inputPhoneNumber.text.length)
        }
        findViewById<View>(R.id.recentContact2).bounceClick {
            inputPhoneNumber.setText("08567891234")
            inputPhoneNumber.setSelection(inputPhoneNumber.text.length)
        }
        findViewById<View>(R.id.btnPickContact).bounceClick {
            inputPhoneNumber.setText("081398765432")
            inputPhoneNumber.setSelection(inputPhoneNumber.text.length)
        }

        // Setup Nominal click listeners

        cardNominal20.bounceClick { selectNominal(20000L) }
        cardNominal50.bounceClick { selectNominal(50000L) }
        cardNominal100.bounceClick { selectNominal(100000L) }
        cardNominal200.bounceClick { selectNominal(200000L) }
        cardNominal300.bounceClick { selectNominal(300000L) }
        cardNominal500.bounceClick { selectNominal(500000L) }

        updateBalanceDisplay()
        selectProvider(EWalletProvider.SHOPEEPAY)
        selectNominal(100000L)

        btnContinuePayment.bounceClick {
            processEWalletTopUp()
        }
    }

    override fun onResume() {
        super.onResume()
        updateBalanceDisplay()
    }

    private fun updateBalanceDisplay() {
        val balance = AppStorage.get().debitBalance
        textSourceBalance.text = "Saldo: ${IdrFormatter.format(balance)}"
    }

    private fun selectProvider(provider: EWalletProvider) {
        selectedProvider = provider

        val providers = listOf(
            Triple(btnShopeePay, findViewById<TextView>(R.id.textProviderShopeePay), provider == EWalletProvider.SHOPEEPAY),
            Triple(btnDana, findViewById<TextView>(R.id.textProviderDana), provider == EWalletProvider.DANA),
            Triple(btnGopay, findViewById<TextView>(R.id.textProviderGopay), provider == EWalletProvider.GOPAY),
            Triple(btnOvo, findViewById<TextView>(R.id.textProviderOvo), provider == EWalletProvider.OVO),
            Triple(btnLinkAja, findViewById<TextView>(R.id.textProviderLinkAja), provider == EWalletProvider.LINKAJA)
        )

        val primaryColor = getColor(R.color.figma_primary)
        val defaultColor = getColor(R.color.figma_text_title)

        providers.forEach { (btn, label, isSelected) ->
            if (isSelected) {
                btn.setBackgroundResource(R.drawable.bg_ewallet_selected)
                label.setTextColor(primaryColor)
            } else {
                btn.setBackgroundResource(R.drawable.bg_ewallet_unselected)
                label.setTextColor(defaultColor)
            }
        }
    }

    private fun selectNominal(amount: Long) {
        selectedNominalAmount = amount
        val total = amount + adminFee
        textTotalPayment.text = IdrFormatter.format(total)

        val nominalViews = listOf(
            cardNominal20 to 20000L,
            cardNominal50 to 50000L,
            cardNominal100 to 100000L,
            cardNominal200 to 200000L,
            cardNominal300 to 300000L,
            cardNominal500 to 500000L
        )

        nominalViews.forEach { (card, nominalVal) ->
            val isSelected = nominalVal == amount
            if (isSelected) {
                card.setBackgroundResource(R.drawable.bg_figma_button_primary)
                val title = card.getChildAt(0) as? TextView
                val sub = card.getChildAt(1) as? TextView
                title?.setTextColor(getColor(android.R.color.white))
                sub?.setTextColor(android.graphics.Color.parseColor("#CCFFFFFF"))
            } else {
                card.setBackgroundResource(R.drawable.bg_figma_card)
                val title = card.getChildAt(0) as? TextView
                val sub = card.getChildAt(1) as? TextView
                title?.setTextColor(getColor(R.color.figma_text_title))
                sub?.setTextColor(getColor(R.color.figma_text_muted))
            }
        }
    }

    private fun processEWalletTopUp() {
        val rawPhone = inputPhoneNumber.text.toString().trim()
        if (rawPhone.length < 9) {
            Toast.makeText(this, getString(R.string.mobile_error_phone), Toast.LENGTH_SHORT).show()
            return
        }

        val totalAmount = selectedNominalAmount + adminFee
        val storage = AppStorage.get()
        if (storage.debitBalance < totalAmount) {
            Toast.makeText(this, getString(R.string.mobile_error_transaction), Toast.LENGTH_LONG).show()
            return
        }

        val txTitle = "Top Up ${selectedProvider.displayName}"
        val txCategory = "Top Up E-Wallet"

        val success = storage.addTransaction(
            title = txTitle,
            category = txCategory,
            amount = totalAmount,
            isIncome = false,
            counterparty = "${selectedProvider.displayName} ($rawPhone)"
        )

        if (!success) {
            Toast.makeText(this, getString(R.string.mobile_error_transaction), Toast.LENGTH_LONG).show()
            return
        }

        // Launch ReceiptActivity
        ReceiptActivity.start(
            context = this,
            title = txTitle,
            category = "E-Wallet",
            amount = selectedNominalAmount,
            fee = adminFee,
            status = "SUCCESS",
            counterparty = "${selectedProvider.displayName} - $rawPhone",
            recipientAccount = rawPhone,
            bankName = "UTOMO Bank",
            note = "Top Up saldo ${selectedProvider.displayName} berhasil",
            origin = "EWALLET"
        )

        finish()
    }
}
