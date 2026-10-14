package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.TransactionCategoryHelper
import com.example.bankapputomo.util.TransactionModel
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Production-grade Payment History screen:
 * - Dynamic category chips (17 categories)
 * - Debounced real-time search across counterparties, categories, refs, notes, and amounts
 * - Filter Bottom Sheet with date range, in/out flow, status, and multi-category selection
 * - Date group headers (Today, Yesterday, Months)
 * - Removable active filter chips & clear all
 * - Real-time filtered Total In / Total Out calculations
 * - Tapping any transaction opens the official shareable ReceiptActivity
 * - Clean top header with analytics icon only (55% prototype banner completely removed)
 */
class HistoryFragment : Fragment() {

    private lateinit var btnHistoryBack: ImageView
    private lateinit var btnHistoryAnalytics: ImageView
    private lateinit var inputHistorySearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var btnOpenHistoryFilter: FrameLayout
    private lateinit var indicatorFilterActive: View

    private lateinit var layoutActiveFiltersRow: LinearLayout
    private lateinit var textFilterResultCount: TextView
    private lateinit var btnClearAllFilters: TextView
    private lateinit var containerActiveFilterChips: LinearLayout

    private lateinit var textHistoryTotalIncome: TextView
    private lateinit var textHistoryTotalExpense: TextView

    private lateinit var containerCategoryChips: LinearLayout
    private lateinit var containerPaymentHistoryCards: LinearLayout
    private lateinit var layoutHistoryEmptyState: LinearLayout

    // State
    private var selectedTopCategoryId: String = "ALL"
    private var searchQuery: String = ""
    private var filterDateRange: String = "ALL" // "ALL", "TODAY", "7DAYS", "30DAYS", "THIS_MONTH", "THIS_YEAR"
    private var filterFlowType: String = "ALL" // "ALL", "IN", "OUT"
    private var filterStatus: String = "ALL" // "ALL", "SUCCESS", "FAILED"
    private val filterCategoryIds = mutableSetOf<String>()

    private val searchDebounceHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        initViews(view)
        setupActions()
        renderCategoryChips()
        applyFiltersAndRender()

        return view
    }

    override fun onResume() {
        super.onResume()
        applyFiltersAndRender()
    }

    private fun initViews(view: View) {
        btnHistoryBack = view.findViewById(R.id.btnHistoryBack)
        btnHistoryAnalytics = view.findViewById(R.id.btnHistoryAnalytics)
        inputHistorySearch = view.findViewById(R.id.inputHistorySearch)
        btnClearSearch = view.findViewById(R.id.btnClearSearch)
        btnOpenHistoryFilter = view.findViewById(R.id.btnOpenHistoryFilter)
        indicatorFilterActive = view.findViewById(R.id.indicatorFilterActive)

        layoutActiveFiltersRow = view.findViewById(R.id.layoutActiveFiltersRow)
        textFilterResultCount = view.findViewById(R.id.textFilterResultCount)
        btnClearAllFilters = view.findViewById(R.id.btnClearAllFilters)
        containerActiveFilterChips = view.findViewById(R.id.containerActiveFilterChips)

        textHistoryTotalIncome = view.findViewById(R.id.textHistoryTotalIncome)
        textHistoryTotalExpense = view.findViewById(R.id.textHistoryTotalExpense)

        containerCategoryChips = view.findViewById(R.id.containerCategoryChips)
        containerPaymentHistoryCards = view.findViewById(R.id.containerPaymentHistoryCards)
        layoutHistoryEmptyState = view.findViewById(R.id.layoutHistoryEmptyState)
    }

    private fun setupActions() {
        btnHistoryBack.bounceClick {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnHistoryAnalytics.bounceClick {
            startActivity(Intent(requireContext(), StatisticsActivity::class.java))
        }

        btnOpenHistoryFilter.bounceClick {
            showFilterBottomSheet()
        }

        btnClearAllFilters.bounceClick {
            resetAllFilters()
        }

        btnClearSearch.bounceClick {
            inputHistorySearch.setText("")
        }

        inputHistorySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                searchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    searchQuery = s?.toString()?.trim() ?: ""
                    applyFiltersAndRender()
                }
                searchDebounceHandler.postDelayed(searchRunnable!!, 250)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderCategoryChips() {
        containerCategoryChips.removeAllViews()
        val isIndo = AppStorage.get().language == "id"

        TransactionCategoryHelper.CATEGORIES.forEach { categoryMeta ->
            val chip = TextView(requireContext()).apply {
                val name = if (isIndo) categoryMeta.nameId else categoryMeta.nameEn
                text = name
                textSize = 12.5f
                includeFontPadding = false
                val padH = (16 * resources.displayMetrics.density).toInt()
                val padV = (8 * resources.displayMetrics.density).toInt()
                setPadding(padH, padV, padH, padV)

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
                layoutParams = lp

                val isSelected = categoryMeta.id == selectedTopCategoryId
                if (isSelected) {
                    setBackgroundResource(R.drawable.bg_figma_button_primary)
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                } else {
                    setBackgroundResource(R.drawable.bg_figma_card)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.figma_text_body))
                }

                bounceClick {
                    selectedTopCategoryId = categoryMeta.id
                    renderCategoryChips()
                    applyFiltersAndRender()
                }
            }
            containerCategoryChips.addView(chip)
        }
    }

    private fun isFilterActive(): Boolean {
        return filterDateRange != "ALL" ||
                filterFlowType != "ALL" ||
                filterStatus != "ALL" ||
                filterCategoryIds.isNotEmpty() ||
                searchQuery.isNotEmpty() ||
                selectedTopCategoryId != "ALL"
    }

    private fun resetAllFilters() {
        selectedTopCategoryId = "ALL"
        searchQuery = ""
        filterDateRange = "ALL"
        filterFlowType = "ALL"
        filterStatus = "ALL"
        filterCategoryIds.clear()
        inputHistorySearch.setText("")
        renderCategoryChips()
        applyFiltersAndRender()
    }

    private fun updateActiveFilterBar(resultCount: Int) {
        val hasCustomFilter = filterDateRange != "ALL" ||
                filterFlowType != "ALL" ||
                filterStatus != "ALL" ||
                filterCategoryIds.isNotEmpty()

        indicatorFilterActive.visibility = if (hasCustomFilter) View.VISIBLE else View.GONE

        if (hasCustomFilter || searchQuery.isNotEmpty() || selectedTopCategoryId != "ALL") {
            layoutActiveFiltersRow.visibility = View.VISIBLE
            val countStr = getString(R.string.history_filter_active_count, resultCount)
            textFilterResultCount.text = countStr

            containerActiveFilterChips.removeAllViews()
            val isIndo = AppStorage.get().language == "id"

            if (selectedTopCategoryId != "ALL") {
                val meta = TransactionCategoryHelper.CATEGORIES.find { it.id == selectedTopCategoryId }
                val label = meta?.let { if (isIndo) it.nameId else it.nameEn } ?: selectedTopCategoryId
                addActivePill("$label x") {
                    selectedTopCategoryId = "ALL"
                    renderCategoryChips()
                    applyFiltersAndRender()
                }
            }

            if (filterDateRange != "ALL") {
                val label = when (filterDateRange) {
                    "TODAY" -> if (isIndo) "Hari Ini" else "Today"
                    "7DAYS" -> if (isIndo) "7 Hari" else "7 Days"
                    "30DAYS" -> if (isIndo) "30 Hari" else "30 Days"
                    "THIS_MONTH" -> if (isIndo) "Bulan Ini" else "This Month"
                    "THIS_YEAR" -> if (isIndo) "Tahun Ini" else "This Year"
                    else -> filterDateRange
                }
                addActivePill("$label x") {
                    filterDateRange = "ALL"
                    applyFiltersAndRender()
                }
            }

            if (filterFlowType != "ALL") {
                val label = if (filterFlowType == "IN") {
                    if (isIndo) "Masuk (+)" else "Money In (+)"
                } else {
                    if (isIndo) "Keluar (-)" else "Money Out (-)"
                }
                addActivePill("$label x") {
                    filterFlowType = "ALL"
                    applyFiltersAndRender()
                }
            }

            if (filterStatus != "ALL") {
                val label = if (filterStatus == "SUCCESS") {
                    if (isIndo) "Berhasil" else "Success"
                } else {
                    if (isIndo) "Gagal" else "Failed"
                }
                addActivePill("$label x") {
                    filterStatus = "ALL"
                    applyFiltersAndRender()
                }
            }

            filterCategoryIds.forEach { catId ->
                val meta = TransactionCategoryHelper.CATEGORIES.find { it.id == catId }
                val label = meta?.let { if (isIndo) it.nameId else it.nameEn } ?: catId
                addActivePill("$label x") {
                    filterCategoryIds.remove(catId)
                    applyFiltersAndRender()
                }
            }
        } else {
            layoutActiveFiltersRow.visibility = View.GONE
        }
    }

    private fun addActivePill(text: String, onRemove: () -> Unit) {
        val pill = TextView(requireContext()).apply {
            this.text = text
            textSize = 11.5f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.figma_primary))
            setBackgroundResource(R.drawable.bg_tile_soft_blue)
            val padH = (10 * resources.displayMetrics.density).toInt()
            val padV = (4 * resources.displayMetrics.density).toInt()
            setPadding(padH, padV, padH, padV)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = (6 * resources.displayMetrics.density).toInt()
            }
            layoutParams = lp
            bounceClick { onRemove() }
        }
        containerActiveFilterChips.addView(pill)
    }

    private fun applyFiltersAndRender() {
        val allTx = AppStorage.get().getTransactions()
        val isIndo = AppStorage.get().language == "id"

        val filtered = allTx.filter { tx ->
            val meta = TransactionCategoryHelper.resolveCategory(tx)

            // 1. Top category selection
            if (selectedTopCategoryId != "ALL" && meta.id != selectedTopCategoryId) {
                return@filter false
            }

            // 2. Filter Bottom Sheet Category multi-select
            if (filterCategoryIds.isNotEmpty() && !filterCategoryIds.contains(meta.id)) {
                return@filter false
            }

            // 3. Flow type filter
            if (filterFlowType == "IN" && !tx.isIncome) return@filter false
            if (filterFlowType == "OUT" && tx.isIncome) return@filter false

            // 4. Status filter
            val isSuccess = tx.status.equals("SUCCESS", ignoreCase = true)
            if (filterStatus == "SUCCESS" && !isSuccess) return@filter false
            if (filterStatus == "FAILED" && isSuccess) return@filter false

            // 5. Date range filter
            val txTimestamp = AppStorage.get().getTransactionTimestamp(tx)
            val now = System.currentTimeMillis()
            when (filterDateRange) {
                "TODAY" -> if (!isSameDay(txTimestamp, now)) return@filter false
                "7DAYS" -> if (txTimestamp <= 0L || txTimestamp < now - 7L * 24L * 60L * 60L * 1000L) return@filter false
                "30DAYS" -> if (txTimestamp <= 0L || txTimestamp < now - 30L * 24L * 60L * 60L * 1000L) return@filter false
                "THIS_MONTH" -> if (!isSameMonth(txTimestamp, now)) return@filter false
                "THIS_YEAR" -> if (!isSameYear(txTimestamp, now)) return@filter false
            }

            // 6. Search query
            if (searchQuery.isNotEmpty()) {
                val q = searchQuery.lowercase()
                val match = tx.counterparty.lowercase().contains(q) ||
                        tx.title.lowercase().contains(q) ||
                        tx.referenceNo.lowercase().contains(q) ||
                        meta.nameId.lowercase().contains(q) ||
                        meta.nameEn.lowercase().contains(q) ||
                        IdrFormatter.format(tx.amount).lowercase().contains(q) ||
                        tx.amount.toString().contains(q)
                if (!match) return@filter false
            }

            true
        }.sortedByDescending { AppStorage.get().getTransactionTimestamp(it) }

        // Calculate Totals for summary strip
        var totalIn = 0L
        var totalOut = 0L
        filtered.forEach {
            if (it.isIncome) totalIn += it.amount else totalOut += it.amount
        }
        textHistoryTotalIncome.text = "+ ${IdrFormatter.format(totalIn)}"
        textHistoryTotalExpense.text = "- ${IdrFormatter.format(totalOut)}"

        updateActiveFilterBar(filtered.size)
        renderCardsWithDateHeaders(filtered)
    }

    private fun renderCardsWithDateHeaders(transactions: List<TransactionModel>) {
        containerPaymentHistoryCards.removeAllViews()
        val isIndo = AppStorage.get().language == "id"

        if (transactions.isEmpty()) {
            layoutHistoryEmptyState.visibility = View.VISIBLE
            return
        }
        layoutHistoryEmptyState.visibility = View.GONE

        // Group by Date Section
        val dateGroups = LinkedHashMap<String, MutableList<TransactionModel>>()
        transactions.forEach { tx ->
            val headerKey = categorizeDateHeader(tx, isIndo)
            dateGroups.getOrPut(headerKey) { mutableListOf() }.add(tx)
        }

        dateGroups.forEach { (dateHeader, txList) ->
            // Date Header
            val headerText = TextView(requireContext()).apply {
                text = dateHeader
                textSize = 13.5f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.figma_text_muted))
                val padTop = (14 * resources.displayMetrics.density).toInt()
                val padBottom = (6 * resources.displayMetrics.density).toInt()
                setPadding(0, padTop, 0, padBottom)
            }
            containerPaymentHistoryCards.addView(headerText)

            // Items in group
            txList.forEach { tx ->
                val cardView = layoutInflater.inflate(R.layout.item_payment_history_card, containerPaymentHistoryCards, false)
                val categoryMeta = TransactionCategoryHelper.resolveCategory(tx)

                val containerIcon = cardView.findViewById<FrameLayout>(R.id.containerTxCategoryIcon)
                val imgIcon = cardView.findViewById<ImageView>(R.id.imgTxCategoryIcon)
                val textCompany = cardView.findViewById<TextView>(R.id.textHistoryCompany)
                val textCatAndDate = cardView.findViewById<TextView>(R.id.textHistoryCategoryAndDate)
                val textAmount = cardView.findViewById<TextView>(R.id.textHistoryAmount)
                val textStatus = cardView.findViewById<TextView>(R.id.textHistoryStatus)

                val ewalletMatch = com.example.bankapputomo.util.EWalletSelectionBottomSheet.ALL_EWALLETS.firstOrNull {
                    tx.title.contains(it.name, ignoreCase = true) || tx.counterparty.contains(it.name, ignoreCase = true)
                }
                if (ewalletMatch != null) {
                    containerIcon.setBackgroundResource(R.drawable.bg_figma_card)
                    imgIcon.setImageResource(ewalletMatch.logoRes)
                    imgIcon.imageTintList = null
                } else {
                    containerIcon.setBackgroundResource(categoryMeta.bgRes)
                    imgIcon.setImageResource(categoryMeta.iconRes)
                    imgIcon.imageTintList = ContextCompat.getColorStateList(requireContext(), categoryMeta.colorRes)
                }

                textCompany.text = tx.counterparty.ifEmpty { tx.title }
                val catName = if (isIndo) categoryMeta.nameId else categoryMeta.nameEn
                textCatAndDate.text = "$catName • ${tx.date}"

                val sign = if (tx.isIncome) "+ " else "- "
                textAmount.text = sign + IdrFormatter.format(tx.amount)
                if (tx.isIncome) {
                    textAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.bank_success))
                } else {
                    textAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.figma_text_title))
                }

                val isSuccess = tx.status.equals("SUCCESS", ignoreCase = true)
                if (isSuccess) {
                    textStatus.text = if (isIndo) "Berhasil" else "Success"
                    textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.bank_success))
                } else {
                    textStatus.text = if (isIndo) "Gagal" else "Failed"
                    textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.bank_red))
                }

                cardView.bounceClick {
                    ReceiptActivity.startFromTransaction(requireContext(), tx)
                }

                containerPaymentHistoryCards.addView(cardView)
            }
        }
    }

    private fun categorizeDateHeader(transaction: TransactionModel, isIndo: Boolean): String {
        val timestamp = AppStorage.get().getTransactionTimestamp(transaction)
        val now = System.currentTimeMillis()
        if (timestamp > 0L) {
            if (isSameDay(timestamp, now)) return if (isIndo) "Hari Ini" else "Today"
            if (isSameDay(timestamp, now - 24L * 60L * 60L * 1000L)) return if (isIndo) "Kemarin" else "Yesterday"
            val locale = if (isIndo) Locale("id", "ID") else Locale.ENGLISH
            return SimpleDateFormat("MMMM yyyy", locale).format(java.util.Date(timestamp))
        }
        return if (isIndo) "Transaksi Lainnya" else "Earlier Transactions"
    }

    private fun isSameDay(firstMillis: Long, secondMillis: Long): Boolean {
        if (firstMillis <= 0L) return false
        val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
        val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
                first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameMonth(firstMillis: Long, secondMillis: Long): Boolean {
        if (firstMillis <= 0L) return false
        val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
        val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
                first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
    }

    private fun isSameYear(firstMillis: Long, secondMillis: Long): Boolean {
        if (firstMillis <= 0L) return false
        val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
        val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
    }

    private fun showFilterBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_history_filter, null)
        dialog.setContentView(sheetView)

        val isIndo = AppStorage.get().language == "id"

        val chipGroupDateRange = sheetView.findViewById<ChipGroup>(R.id.chipGroupDateRange)
        val chipGroupFlowType = sheetView.findViewById<ChipGroup>(R.id.chipGroupFlowType)
        val chipGroupStatus = sheetView.findViewById<ChipGroup>(R.id.chipGroupStatus)
        val chipGroupCategories = sheetView.findViewById<ChipGroup>(R.id.chipGroupCategories)
        val btnFilterReset = sheetView.findViewById<TextView>(R.id.btnFilterReset)
        val btnFilterApply = sheetView.findViewById<TextView>(R.id.btnFilterApply)

        // Set Date Range Selection
        when (filterDateRange) {
            "TODAY" -> sheetView.findViewById<Chip>(R.id.chipDateToday)?.isChecked = true
            "7DAYS" -> sheetView.findViewById<Chip>(R.id.chipDate7Days)?.isChecked = true
            "30DAYS" -> sheetView.findViewById<Chip>(R.id.chipDate30Days)?.isChecked = true
            "THIS_MONTH" -> sheetView.findViewById<Chip>(R.id.chipDateThisMonth)?.isChecked = true
            "THIS_YEAR" -> sheetView.findViewById<Chip>(R.id.chipDateThisYear)?.isChecked = true
            else -> sheetView.findViewById<Chip>(R.id.chipDateAll)?.isChecked = true
        }

        // Set Flow Type Selection
        when (filterFlowType) {
            "IN" -> sheetView.findViewById<Chip>(R.id.chipFlowIn)?.isChecked = true
            "OUT" -> sheetView.findViewById<Chip>(R.id.chipFlowOut)?.isChecked = true
            else -> sheetView.findViewById<Chip>(R.id.chipFlowAll)?.isChecked = true
        }

        // Set Status Selection
        when (filterStatus) {
            "SUCCESS" -> sheetView.findViewById<Chip>(R.id.chipStatusSuccess)?.isChecked = true
            "FAILED" -> sheetView.findViewById<Chip>(R.id.chipStatusFailed)?.isChecked = true
            else -> sheetView.findViewById<Chip>(R.id.chipStatusAll)?.isChecked = true
        }

        // Populate Category Multi-Select Chips
        chipGroupCategories.removeAllViews()
        TransactionCategoryHelper.CATEGORIES.filter { it.id != "ALL" }.forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = if (isIndo) cat.nameId else cat.nameEn
                isCheckable = true
                isChecked = filterCategoryIds.contains(cat.id)
                tag = cat.id
            }
            chipGroupCategories.addView(chip)
        }

        btnFilterReset.bounceClick {
            sheetView.findViewById<Chip>(R.id.chipDateAll)?.isChecked = true
            sheetView.findViewById<Chip>(R.id.chipFlowAll)?.isChecked = true
            sheetView.findViewById<Chip>(R.id.chipStatusAll)?.isChecked = true
            for (i in 0 until chipGroupCategories.childCount) {
                (chipGroupCategories.getChildAt(i) as? Chip)?.isChecked = false
            }
        }

        btnFilterApply.bounceClick {
            // Read Date Range
            filterDateRange = when (chipGroupDateRange.checkedChipId) {
                R.id.chipDateToday -> "TODAY"
                R.id.chipDate7Days -> "7DAYS"
                R.id.chipDate30Days -> "30DAYS"
                R.id.chipDateThisMonth -> "THIS_MONTH"
                R.id.chipDateThisYear -> "THIS_YEAR"
                else -> "ALL"
            }

            // Read Flow Type
            filterFlowType = when (chipGroupFlowType.checkedChipId) {
                R.id.chipFlowIn -> "IN"
                R.id.chipFlowOut -> "OUT"
                else -> "ALL"
            }

            // Read Status
            filterStatus = when (chipGroupStatus.checkedChipId) {
                R.id.chipStatusSuccess -> "SUCCESS"
                R.id.chipStatusFailed -> "FAILED"
                else -> "ALL"
            }

            // Read Categories Multi-select
            filterCategoryIds.clear()
            for (i in 0 until chipGroupCategories.childCount) {
                val chip = chipGroupCategories.getChildAt(i) as? Chip
                if (chip != null && chip.isChecked) {
                    val id = chip.tag as? String
                    if (id != null) filterCategoryIds.add(id)
                }
            }

            dialog.dismiss()
            applyFiltersAndRender()
        }

        dialog.show()
    }
}
