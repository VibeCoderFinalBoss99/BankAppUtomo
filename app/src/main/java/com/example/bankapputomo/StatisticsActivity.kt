package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.TransactionCategoryHelper
import com.example.bankapputomo.util.TransactionModel
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.example.bankapputomo.view.StatisticsChartView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsActivity : BaseActivity() {

    private lateinit var chartView: StatisticsChartView
    private lateinit var chipLine: TextView
    private lateinit var chipBar: TextView
    private lateinit var chipPie: TextView
    private lateinit var textStatBalance: TextView
    private lateinit var containerStatTransactions: LinearLayout

    // Month Selector Bar
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private lateinit var btnMonthPicker: LinearLayout
    private lateinit var textSelectedMonth: TextView

    private var selectedMonthIndex = -1

    data class MonthStatInfo(
        val label: String,
        val shortLabel: String,
        val spent: Long,
        val balance: Long,
        val transactions: List<TransactionModel>
    )

    private var monthStatList: List<MonthStatInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        findViewById<View>(R.id.btnBack).bounceClick { finish() }
        findViewById<View>(R.id.btnStatNotification).bounceClick {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        chartView = findViewById(R.id.statisticsChartView)
        chipLine = findViewById(R.id.chipChartLine)
        chipBar = findViewById(R.id.chipChartBar)
        chipPie = findViewById(R.id.chipChartPie)
        textStatBalance = findViewById(R.id.textStatBalance)
        containerStatTransactions = findViewById(R.id.containerStatTransactions)

        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        btnMonthPicker = findViewById(R.id.btnMonthPicker)
        textSelectedMonth = findViewById(R.id.textSelectedMonth)

        rebuildMonthStats()

        // Month Selector Bar Click Listeners
        btnPrevMonth.bounceClick {
            if (selectedMonthIndex > 0) {
                selectMonth(selectedMonthIndex - 1)
            }
        }

        btnNextMonth.bounceClick {
            if (selectedMonthIndex < monthStatList.size - 1) {
                selectMonth(selectedMonthIndex + 1)
            }
        }

        btnMonthPicker.bounceClick {
            showMonthPickerDialog()
        }

        // Chart touch callback: when user touches any month/bar/point on the chart, update screen
        chartView.onMonthSelectedListener = { index, _ ->
            selectMonth(index)
        }

        chipLine.bounceClick {
            selectChartType(StatisticsChartView.ChartType.LINE)
        }

        chipBar.bounceClick {
            selectChartType(StatisticsChartView.ChartType.BAR)
        }

        chipPie.bounceClick {
            selectChartType(StatisticsChartView.ChartType.PIE)
        }

        findViewById<View>(R.id.btnStatSeeAll).bounceClick {
            startActivity(Intent(this, TransactionReportActivity::class.java))
        }

        findViewById<View>(R.id.cardMonthlySpendingLimit)?.bounceClick {
            showEditSpendingLimitBottomSheet()
        }
        findViewById<View>(R.id.btnEditSpendingLimit)?.bounceClick {
            showEditSpendingLimitBottomSheet()
        }

        selectMonth(selectedMonthIndex)
    }

    override fun onResume() {
        super.onResume()
        rebuildMonthStats()
        selectedMonthIndex = selectedMonthIndex.coerceIn(0, monthStatList.lastIndex)
        selectMonth(selectedMonthIndex)
    }

    private fun rebuildMonthStats() {
        val storage = AppStorage.get()
        val allTransactions = storage.getTransactions()
        val now = Calendar.getInstance()
        val language = storage.language
        val monthLocale = if (language == "en") Locale.ENGLISH else Locale("id", "ID")
        val monthFormat = SimpleDateFormat("MMMM yyyy", monthLocale)
        val shortMonthFormat = SimpleDateFormat("MMM", monthLocale)

        monthStatList = (5 downTo 0).map { offset ->
            val month = (now.clone() as Calendar).apply {
                add(Calendar.MONTH, -offset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val nextMonth = (month.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val transactions = allTransactions
                .filter { tx ->
                    val timestamp = storage.getTransactionTimestamp(tx)
                    timestamp >= month.timeInMillis && timestamp < nextMonth.timeInMillis
                }
                .sortedByDescending { storage.getTransactionTimestamp(it) }
            val spent = transactions
                .filter { !it.isIncome && it.status.equals("SUCCESS", ignoreCase = true) }
                .sumOf { it.amount }

            MonthStatInfo(
                label = monthFormat.format(month.time),
                shortLabel = shortMonthFormat.format(month.time),
                spent = spent,
                balance = storage.debitBalance,
                transactions = transactions
            )
        }

        chartView.setMonthlyExpenses(
            monthStatList.map { it.shortLabel },
            monthStatList.map { it.spent }
        )
        if (selectedMonthIndex < 0) selectedMonthIndex = monthStatList.lastIndex
    }

    private fun getMonthSpent(index: Int): Long {
        return monthStatList.getOrNull(index)?.spent ?: 0L
    }

    private fun getMonthBalance(index: Int): Long {
        return monthStatList.getOrNull(index)?.balance ?: AppStorage.get().debitBalance
    }

    private fun selectMonth(index: Int) {
        selectedMonthIndex = index.coerceIn(0, monthStatList.size - 1)
        chartView.activeMonthIndex = selectedMonthIndex

        val currentMonth = monthStatList[selectedMonthIndex]
        textSelectedMonth.text = currentMonth.label

        // Arrow button states
        btnPrevMonth.isEnabled = selectedMonthIndex > 0
        btnPrevMonth.alpha = if (selectedMonthIndex > 0) 1.0f else 0.35f

        btnNextMonth.isEnabled = selectedMonthIndex < monthStatList.size - 1
        btnNextMonth.alpha = if (selectedMonthIndex < monthStatList.size - 1) 1.0f else 0.35f

        val balance = getMonthBalance(selectedMonthIndex)
        textStatBalance.text = IdrFormatter.format(balance)

        updateSpendingLimitUI()
        updatePieChartLegend()
        renderTransactions()
    }

    private fun updateSpendingLimitUI() {
        val storage = AppStorage.get()
        val limit = storage.monthlySpendingLimit
        val spent = getMonthSpent(selectedMonthIndex)

        val percent = if (limit > 0) ((spent.toDouble() / limit.toDouble()) * 100).toInt().coerceIn(0, 100) else 0

        findViewById<TextView>(R.id.textSpendingLimit)?.text =
            "${IdrFormatter.format(spent)} terpakai dari ${IdrFormatter.format(limit)}"
        findViewById<ProgressBar>(R.id.progressSpendingLimit)?.progress = percent
        findViewById<TextView>(R.id.textSpendingPercent)?.text = "$percent%"
        findViewById<TextView>(R.id.textSpendingMax)?.text = IdrFormatter.format(limit)
    }

    private fun updatePieChartLegend() {
        val storage = AppStorage.get()
        val limit = storage.monthlySpendingLimit
        val spent = getMonthSpent(selectedMonthIndex)
        val transactions = monthStatList.getOrNull(selectedMonthIndex)?.transactions.orEmpty()
            .filter { !it.isIncome && it.status.equals("SUCCESS", ignoreCase = true) }

        val buckets = longArrayOf(0L, 0L, 0L, 0L)
        transactions.forEach { tx ->
            when (TransactionCategoryHelper.resolveCategory(tx).id) {
                "FOOD" -> buckets[0] += tx.amount
                "ENTERTAINMENT" -> buckets[1] += tx.amount
                "PLN", "PDAM", "INTERNET", "BPJS", "FEE" -> buckets[2] += tx.amount
                else -> buckets[3] += tx.amount
            }
        }

        val amountShopping = buckets[0]
        val amountEntertainment = buckets[1]
        val amountBills = buckets[2]
        val amountFood = buckets[3]

        findViewById<TextView>(R.id.textStatAmountShopping)?.text = IdrFormatter.format(amountShopping)
        findViewById<TextView>(R.id.textStatAmountEntertainment)?.text = IdrFormatter.format(amountEntertainment)
        findViewById<TextView>(R.id.textStatAmountBills)?.text = IdrFormatter.format(amountBills)
        findViewById<TextView>(R.id.textStatAmountFood)?.text = IdrFormatter.format(amountFood)

        val percent = if (limit > 0) ((spent.toDouble() / limit.toDouble()) * 100).toInt().coerceIn(0, 100) else 0
        chartView.setPieSummary(percent, getString(R.string.stat_spent_this_month))
        chartView.setPiePercentages(buckets.map { it.toFloat() })
    }

    private fun selectChartType(type: StatisticsChartView.ChartType) {
        chartView.chartType = type

        val activeBg = R.drawable.bg_figma_tab_active
        val inactiveBg = R.drawable.bg_figma_tab_inactive
        val activeTextColor = getColor(R.color.white)
        val inactiveTextColor = getColor(R.color.figma_text_muted)

        chipLine.setBackgroundResource(if (type == StatisticsChartView.ChartType.LINE) activeBg else inactiveBg)
        chipLine.setTextColor(if (type == StatisticsChartView.ChartType.LINE) activeTextColor else inactiveTextColor)

        chipBar.setBackgroundResource(if (type == StatisticsChartView.ChartType.BAR) activeBg else inactiveBg)
        chipBar.setTextColor(if (type == StatisticsChartView.ChartType.BAR) activeTextColor else inactiveTextColor)

        chipPie.setBackgroundResource(if (type == StatisticsChartView.ChartType.PIE) activeBg else inactiveBg)
        chipPie.setTextColor(if (type == StatisticsChartView.ChartType.PIE) activeTextColor else inactiveTextColor)

        val legendContainer = findViewById<View>(R.id.layoutPieLegendContainer)
        if (type == StatisticsChartView.ChartType.PIE) {
            legendContainer?.visibility = View.VISIBLE
            updatePieChartLegend()
        } else {
            legendContainer?.visibility = View.GONE
        }
    }

    private fun showMonthPickerDialog() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_month_picker, null)
        dialog.setContentView(sheetView)

        val container = sheetView.findViewById<LinearLayout>(R.id.containerMonthItems)
        container?.removeAllViews()

        monthStatList.forEachIndexed { index, info ->
            val row = layoutInflater.inflate(R.layout.item_month_picker_row, container, false)
            val title = row.findViewById<TextView>(R.id.textMonthRowTitle)
            val subtitle = row.findViewById<TextView>(R.id.textMonthRowSubtitle)
            val checkIcon = row.findViewById<ImageView>(R.id.iconMonthRowSelected)

            title.text = info.label
            val spent = getMonthSpent(index)
            subtitle.text = "Pengeluaran: ${IdrFormatter.format(spent)}"

            val isSelected = index == selectedMonthIndex
            if (isSelected) {
                checkIcon.visibility = View.VISIBLE
                row.setBackgroundResource(R.drawable.bg_tile_soft_blue)
            } else {
                checkIcon.visibility = View.GONE
                row.setBackgroundResource(R.drawable.bg_figma_card)
            }

            row.bounceClick {
                selectMonth(index)
                dialog.dismiss()
            }

            container?.addView(row)
        }

        dialog.show()
    }

    private fun showEditSpendingLimitBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_edit_spending_limit, null)
        dialog.setContentView(sheetView)

        val storage = AppStorage.get()
        val currentLimit = storage.monthlySpendingLimit
        val currentSpent = getMonthSpent(selectedMonthIndex)

        val textCurrentSpentSubtitle = sheetView.findViewById<TextView>(R.id.textCurrentSpentSubtitle)
        val inputSpendingLimit = sheetView.findViewById<android.widget.EditText>(R.id.inputSpendingLimit)
        val chipLimit5M = sheetView.findViewById<TextView>(R.id.chipLimit5M)
        val chipLimit10M = sheetView.findViewById<TextView>(R.id.chipLimit10M)
        val chipLimit15M = sheetView.findViewById<TextView>(R.id.chipLimit15M)
        val chipLimit25M = sheetView.findViewById<TextView>(R.id.chipLimit25M)
        val btnSave = sheetView.findViewById<View>(R.id.btnSaveSpendingLimit)
        val btnCancel = sheetView.findViewById<View>(R.id.btnCancelSpendingLimit)

        textCurrentSpentSubtitle.text = "Pengeluaran bulan ini: ${IdrFormatter.format(currentSpent)}"
        inputSpendingLimit.setText(currentLimit.toString())

        val chips = listOf(
            chipLimit5M to 5000000L,
            chipLimit10M to 10000000L,
            chipLimit15M to 15000000L,
            chipLimit25M to 25000000L
        )

        fun updateChipsSelection(selectedVal: Long) {
            chips.forEach { (chip, amount) ->
                if (amount == selectedVal) {
                    chip.setBackgroundResource(R.drawable.bg_figma_button_primary)
                    chip.setTextColor(getColor(android.R.color.white))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_figma_card)
                    chip.setTextColor(getColor(R.color.figma_text_title))
                }
            }
        }

        updateChipsSelection(currentLimit)

        chips.forEach { (chip, amount) ->
            chip.bounceClick {
                inputSpendingLimit.setText(amount.toString())
                updateChipsSelection(amount)
            }
        }

        btnSave.bounceClick {
            val entered = inputSpendingLimit.text.toString().trim().toLongOrNull() ?: currentLimit
            if (entered <= 0) {
                android.widget.Toast.makeText(this, "Batas pengeluaran harus lebih besar dari 0", android.widget.Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            storage.monthlySpendingLimit = entered
            updateSpendingLimitUI()
            updatePieChartLegend()
            dialog.dismiss()
            android.widget.Toast.makeText(this, "Batas pengeluaran berhasil disimpan: ${IdrFormatter.format(entered)}", android.widget.Toast.LENGTH_SHORT).show()
        }

        btnCancel.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun renderTransactions() {
        containerStatTransactions.removeAllViews()

        val currentTransactions = monthStatList.getOrNull(selectedMonthIndex)?.transactions?.take(4) ?: emptyList()

        currentTransactions.forEach { item ->
            val row = layoutInflater.inflate(R.layout.item_quick_history_row, containerStatTransactions, false)
            val iconView = row.findViewById<ImageView>(R.id.quickHistoryIcon)
            val titleView = row.findViewById<TextView>(R.id.quickHistoryTitle)
            val subtitleView = row.findViewById<TextView>(R.id.quickHistorySubtitle)
            val amountView = row.findViewById<TextView>(R.id.quickHistoryAmount)

            titleView.text = item.counterparty.ifEmpty { item.title }
            val category = TransactionCategoryHelper.resolveCategory(item)
            subtitleView.text = "${if (AppStorage.get().language == "id") category.nameId else category.nameEn} • ${item.date}"

            val isIncome = item.isIncome
            val absAmount = item.amount

            if (isIncome) {
                amountView.text = "+ ${IdrFormatter.format(absAmount)}"
                amountView.setTextColor(getColor(R.color.bank_success))
                iconView.setImageResource(R.drawable.ic_figma_received)
                iconView.setBackgroundResource(R.drawable.bg_tile_soft_green)
            } else {
                amountView.text = "- ${IdrFormatter.format(absAmount)}"
                amountView.setTextColor(getColor(R.color.figma_text_title))
                iconView.setImageResource(R.drawable.ic_figma_sent)
                iconView.setBackgroundResource(R.drawable.bg_tile_soft_blue)
            }

            row.bounceClick {
                ReceiptActivity.start(
                    context = this,
                    title = item.title,
                    category = if (isIncome) "Income" else "Expense",
                    amount = absAmount,
                    status = item.status,
                    counterparty = item.counterparty,
                    bankName = "UTOMO Bank",
                    note = item.date,
                    origin = "STATISTICS"
                )
            }

            containerStatTransactions.addView(row)
        }
    }
}
