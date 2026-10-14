package com.example.bankapputomo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.DecimalFormat

class ExchangeActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var inputFromAmount: EditText
    private lateinit var btnFromCurrency: LinearLayout
    private lateinit var textFromCurrencyCode: TextView

    private lateinit var btnSwapCurrencies: View

    private lateinit var textToAmount: TextView
    private lateinit var textFromBalance: TextView
    private lateinit var textToBalance: TextView
    private lateinit var btnToCurrency: LinearLayout
    private lateinit var textToCurrencyCode: TextView

    private lateinit var textLiveRate: TextView
    private lateinit var textCurrencyHoldings: TextView
    private lateinit var btnExchangeAction: TextView

    private var fromCurrency = "USD"
    private var toCurrency = "IDR"

    // Currency values relative to 1 USD
    private val ratesToUsd = mapOf(
        "USD" to 1.0,
        "IDR" to 15650.0,
        "KRW" to 1122.0,
        "VND" to 24500.0,
        "HKD" to 7.82,
        "NTD" to 32.1,
        "JMD" to 155.0
    )

    private val currencyLabels = listOf(
        "VND" to "VND ( Viet Nam Dong )",
        "HKD" to "HK$ ( Hong Kong Dollar )",
        "USD" to "USD ( Dollar )",
        "NTD" to "NT$ ( Taiwan Dollar )",
        "JMD" to "J$ ( Jamaika Dollar )",
        "KRW" to "KRW ( Korean Won )",
        "IDR" to "IDR ( Indonesian Rupiah )"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange)

        btnBack = findViewById(R.id.btnBack)
        inputFromAmount = findViewById(R.id.inputFromAmount)
        btnFromCurrency = findViewById(R.id.btnFromCurrency)
        textFromCurrencyCode = findViewById(R.id.textFromCurrencyCode)
        textFromBalance = findViewById(R.id.textFromBalance)

        btnSwapCurrencies = findViewById(R.id.btnSwapCurrencies)

        textToAmount = findViewById(R.id.textToAmount)
        textToBalance = findViewById(R.id.textToBalance)
        btnToCurrency = findViewById(R.id.btnToCurrency)
        textToCurrencyCode = findViewById(R.id.textToCurrencyCode)

        textLiveRate = findViewById(R.id.textLiveRate)
        textCurrencyHoldings = findViewById(R.id.textCurrencyHoldings)
        btnExchangeAction = findViewById(R.id.btnExchangeAction)

        btnBack.bounceClick { finish() }

        // Initial default: 1000 USD matching Figma Exchange #2
        inputFromAmount.setText("1000")
        updateRateDisplay()
        updateBalanceDisplay()
        recalculate()

        inputFromAmount.addTextChangedListener {
            recalculate()
        }

        btnFromCurrency.bounceClick {
            showCurrencySelector(isFrom = true)
        }

        btnToCurrency.bounceClick {
            showCurrencySelector(isFrom = false)
        }

        btnSwapCurrencies.bounceClick {
            val temp = fromCurrency
            fromCurrency = toCurrency
            toCurrency = temp
            textFromCurrencyCode.text = fromCurrency
            textToCurrencyCode.text = toCurrency
            updateRateDisplay()
            updateBalanceDisplay()
            recalculate()
        }

        btnExchangeAction.bounceClick {
            val amountStr = inputFromAmount.text.toString().trim()
            val fromVal = amountStr.toDoubleOrNull() ?: 0.0
            if (fromVal <= 0.0) {
                Toast.makeText(this, "Please enter an amount to exchange", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val toVal = calculateConversion(fromVal)
            val toValFormatted = formatCurrency(toVal, toCurrency)

            if (!AppStorage.get().executeCurrencyExchange(fromCurrency, toCurrency, fromVal, toVal)) {
                Toast.makeText(this, "Saldo $fromCurrency tidak mencukupi untuk pertukaran ini.", Toast.LENGTH_LONG).show()
                return@bounceClick
            }

            ReceiptActivity.start(
                context = this,
                title = "Konversi Valas $fromCurrency ke $toCurrency",
                category = "Valuta Asing",
                amount = toVal.toLong(),
                fee = 0L,
                counterparty = "UTOMO Bank Treasury FX",
                recipientAccount = "Portofolio Valas",
                bankName = "UTOMO Bank",
                note = "Penukaran $fromCurrency ke $toCurrency senilai $toValFormatted",
                origin = "EXCHANGE",
                customRefNo = "FX-${(10000000..99999999).random()}"
            )
            finish()
        }
    }

    private fun updateRateDisplay() {
        val rateFrom = ratesToUsd[fromCurrency] ?: 1.0
        val rateTo = ratesToUsd[toCurrency] ?: 1.0
        val crossRate = rateTo / rateFrom
        val formatter = DecimalFormat("#,##0.##")
        textLiveRate.text = "1 $fromCurrency = ${formatter.format(crossRate)} $toCurrency"
    }

    private fun updateBalanceDisplay() {
        val storage = AppStorage.get()
        textFromBalance.text = "Saldo tersedia: ${formatCurrency(storage.getCurrencyBalance(fromCurrency), fromCurrency)} $fromCurrency"
        textToBalance.text = "Saldo saat ini: ${formatCurrency(storage.getCurrencyBalance(toCurrency), toCurrency)} $toCurrency"

        val holdings = storage.getCurrencyBalances().toMutableMap().apply {
            put("IDR", storage.debitBalance.toDouble())
        }
            .filterValues { it > 0.000001 }
            .toSortedMap()
        if (holdings.isEmpty()) {
            textCurrencyHoldings.visibility = View.GONE
        } else {
            textCurrencyHoldings.visibility = View.VISIBLE
            textCurrencyHoldings.text = buildString {
                append("Saldo mata uang tersimpan")
                holdings.forEach { (code, amount) ->
                    append("\n${formatCurrency(amount, code)} $code")
                }
            }
        }
    }

    private fun calculateConversion(amount: Double): Double {
        val rateFrom = ratesToUsd[fromCurrency] ?: 1.0
        val rateTo = ratesToUsd[toCurrency] ?: 1.0
        return (amount / rateFrom) * rateTo
    }

    private fun recalculate() {
        val amountStr = inputFromAmount.text.toString().trim()
        val fromVal = amountStr.toDoubleOrNull() ?: 0.0
        if (fromVal <= 0.0) {
            textToAmount.text = ""
            return
        }
        val converted = calculateConversion(fromVal)
        textToAmount.text = formatCurrency(converted, toCurrency)
    }

    private fun formatCurrency(amount: Double, currency: String): String {
        return if (currency == "IDR" || currency == "KRW" || currency == "VND") {
            DecimalFormat("#,###").format(amount.toLong())
        } else {
            DecimalFormat("#,##0.00").format(amount)
        }
    }

    private fun showCurrencySelector(isFrom: Boolean) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_select_currency, null)
        bottomSheet.setContentView(view)

        val container = view.findViewById<LinearLayout>(R.id.containerCurrencyList)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseCurrencyModal)

        btnClose.bounceClick { bottomSheet.dismiss() }

        val currentSelected = if (isFrom) fromCurrency else toCurrency

        currencyLabels.forEach { (code, label) ->
            val row = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 28, 16, 28)
                isClickable = true
                isFocusable = true
            }

            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = label
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ExchangeActivity, R.font.poppins_medium)
                setTextColor(resources.getColor(if (code == currentSelected) R.color.figma_primary else R.color.figma_text_title, theme))
                textSize = 14f
            }
            row.addView(tv)

            if (code == currentSelected) {
                val check = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(40, 40)
                    setImageResource(R.drawable.ic_figma_check)
                    setColorFilter(resources.getColor(R.color.figma_primary, theme))
                }
                row.addView(check)
            }

            row.bounceClick {
                if (isFrom) {
                    fromCurrency = code
                    textFromCurrencyCode.text = code
                } else {
                    toCurrency = code
                    textToCurrencyCode.text = code
                }
                updateRateDisplay()
                updateBalanceDisplay()
                recalculate()
                bottomSheet.dismiss()
            }

            container.addView(row)
        }

        bottomSheet.show()
    }
}
