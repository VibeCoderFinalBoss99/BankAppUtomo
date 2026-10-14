package com.example.bankapputomo

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.bankapputomo.util.FileShareHelper
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog

class TransactionReportActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnExportReport: ImageView
    private lateinit var btnSelectMonth: View
    private lateinit var textSelectedMonth: TextView
    private lateinit var cardReportCaptureArea: View

    private val availableMonths = listOf(
        "Oktober 2026",
        "September 2026",
        "Agustus 2026",
        "Juli 2026",
        "Juni 2026",
        "Mei 2026"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_report)

        btnBack = findViewById(R.id.btnBack)
        btnExportReport = findViewById(R.id.btnExportReport)
        btnSelectMonth = findViewById(R.id.btnSelectMonth)
        textSelectedMonth = findViewById(R.id.textSelectedMonth)
        cardReportCaptureArea = findViewById(R.id.cardReportCaptureArea)

        textSelectedMonth.text = availableMonths.first()

        btnBack.bounceClick {
            finish()
        }

        btnExportReport.bounceClick {
            val currentMonth = textSelectedMonth.text.toString().replace(" ", "_")
            val fileName = "BankUtomo_EStatement_${currentMonth}.pdf"
            FileShareHelper.shareViewAsPdf(this, cardReportCaptureArea, fileName)
        }

        btnSelectMonth.bounceClick {
            showMonthPickerBottomSheet()
        }
    }

    private fun showMonthPickerBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_month_picker, null)
        dialog.setContentView(sheetView)

        val container = sheetView.findViewById<LinearLayout>(R.id.containerMonthItems)
        val btnClose = sheetView.findViewById<View>(R.id.btnCloseMonthPicker)

        availableMonths.forEach { month ->
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                radius = 16 * resources.displayMetrics.density
                strokeWidth = (1.2 * resources.displayMetrics.density).toInt()
                strokeColor = ContextCompat.getColor(this@TransactionReportActivity, R.color.glass_card_stroke)
                setCardBackgroundColor(ContextCompat.getColor(this@TransactionReportActivity, R.color.figma_surface))
                elevation = 0f
                isClickable = true
                isFocusable = true

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * resources.displayMetrics.density).toInt()
                }
                layoutParams = lp

                val tv = TextView(this@TransactionReportActivity).apply {
                    text = month
                    textSize = 14f
                    typeface = androidx.core.content.res.ResourcesCompat.getFont(this@TransactionReportActivity, R.font.poppins_semibold)
                    val isSelected = month == textSelectedMonth.text.toString()
                    setTextColor(
                        if (isSelected) ContextCompat.getColor(this@TransactionReportActivity, R.color.figma_primary)
                        else ContextCompat.getColor(this@TransactionReportActivity, R.color.figma_text_title)
                    )
                    val p = (16 * resources.displayMetrics.density).toInt()
                    setPadding(p, p, p, p)
                }
                addView(tv)

                bounceClick {
                    textSelectedMonth.text = month
                    dialog.dismiss()
                    Toast.makeText(this@TransactionReportActivity, "Menampilkan laporan $month", Toast.LENGTH_SHORT).show()
                }
            }
            container.addView(card)
        }

        btnClose.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }
}
