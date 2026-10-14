package com.example.bankapputomo

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BranchModel
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.gson.Gson

class ExchangeRateActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var exchangeHeaderTitle: TextView

    private lateinit var tabRates: TextView
    private lateinit var tabConverter: TextView
    private lateinit var tabBranches: TextView

    private lateinit var viewTabRates: View
    private lateinit var viewTabConverter: View
    private lateinit var viewTabBranches: View

    private lateinit var inputExchangePay: EditText
    private lateinit var textExchangeReceive: TextView
    private lateinit var btnSubmitExchange: TextView

    // Tab 3: Interactive Leaflet Map & Branches
    private lateinit var webViewTabBranchMap: WebView
    private lateinit var btnTabRecenterMap: ImageView
    private lateinit var inputTabSearchBranch: EditText
    private lateinit var btnTabClearSearch: ImageView
    private lateinit var recyclerTabBranches: RecyclerView
    private lateinit var layoutTabEmptyBranches: LinearLayout

    private lateinit var branchAdapter: BranchAdapter
    private var allBranches: List<BranchModel> = emptyList()
    private var filteredBranches: List<BranchModel> = emptyList()
    private var isBranchMapReady = false
    private val gson = Gson()

    private val rateUsdToIdr = 15650.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_rate)

        btnBack = findViewById(R.id.btnBack)
        exchangeHeaderTitle = findViewById(R.id.exchangeHeaderTitle)

        tabRates = findViewById(R.id.tabRates)
        tabConverter = findViewById(R.id.tabConverter)
        tabBranches = findViewById(R.id.tabBranches)

        viewTabRates = findViewById(R.id.viewTabRates)
        viewTabConverter = findViewById(R.id.viewTabConverter)
        viewTabBranches = findViewById(R.id.viewTabBranches)

        inputExchangePay = findViewById(R.id.inputExchangePay)
        textExchangeReceive = findViewById(R.id.textExchangeReceive)
        btnSubmitExchange = findViewById(R.id.btnSubmitExchange)

        // Initialize Tab 3 Views
        webViewTabBranchMap = findViewById(R.id.webViewTabBranchMap)
        btnTabRecenterMap = findViewById(R.id.btnTabRecenterMap)
        inputTabSearchBranch = findViewById(R.id.inputTabSearchBranch)
        btnTabClearSearch = findViewById(R.id.btnTabClearSearch)
        recyclerTabBranches = findViewById(R.id.recyclerTabBranches)
        layoutTabEmptyBranches = findViewById(R.id.layoutTabEmptyBranches)

        btnBack.bounceClick {
            finish()
        }

        tabRates.bounceClick {
            selectTab(0)
        }

        tabConverter.bounceClick {
            selectTab(1)
        }

        tabBranches.bounceClick {
            selectTab(2)
        }

        inputExchangePay.addTextChangedListener { text ->
            val payStr = text?.toString()?.trim() ?: "0"
            val payVal = payStr.toDoubleOrNull() ?: 0.0
            val receiveIdr = (payVal * rateUsdToIdr).toLong()
            textExchangeReceive.text = IdrFormatter.format(receiveIdr)
        }

        btnSubmitExchange.bounceClick {
            val payStr = inputExchangePay.text.toString().trim()
            val payVal = payStr.toDoubleOrNull() ?: 0.0
            if (payVal <= 0) {
                Toast.makeText(this, "Please enter an amount to exchange", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            val receiveIdr = (payVal * rateUsdToIdr).toLong()
            val exchanged = AppStorage.get().executeCurrencyExchange(
                fromCurrency = "USD",
                toCurrency = "IDR",
                fromAmount = payVal,
                toAmount = receiveIdr.toDouble()
            )
            if (!exchanged) {
                Toast.makeText(this, "Saldo USD tidak mencukupi untuk pertukaran ini.", Toast.LENGTH_LONG).show()
                return@bounceClick
            }

            ReceiptActivity.start(
                context = this,
                title = "Penukaran Valas USD ke IDR",
                category = "Valuta Asing",
                amount = receiveIdr,
                fee = 0L,
                counterparty = "UTOMO Bank Forex",
                recipientAccount = "Rekening Utama Utomo",
                bankName = "UTOMO Bank",
                note = "Jual $payVal USD ke IDR",
                origin = "EXCHANGE",
                customRefNo = "FX-${(10000000..99999999).random()}"
            )
            selectTab(0)
        }

        setupBranchTab()
    }

    private fun setupBranchTab() {
        branchAdapter = BranchAdapter(emptyList()) { branch ->
            branchAdapter.setSelectedId(branch.id)
            webViewTabBranchMap.evaluateJavascript("window.focusBranch('${branch.id}');", null)
        }
        recyclerTabBranches.layoutManager = LinearLayoutManager(this)
        recyclerTabBranches.adapter = branchAdapter

        webViewTabBranchMap.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            databaseEnabled = true
        }

        webViewTabBranchMap.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isBranchMapReady = true
                pushBranchesToMap(filteredBranches)
            }
        }
        webViewTabBranchMap.webChromeClient = WebChromeClient()

        webViewTabBranchMap.addJavascriptInterface(object {
            @JavascriptInterface
            fun onBranchSelected(branchId: String) {
                runOnUiThread {
                    branchAdapter.setSelectedId(branchId)
                    val pos = filteredBranches.indexOfFirst { it.id == branchId }
                    if (pos != -1) {
                        recyclerTabBranches.smoothScrollToPosition(pos)
                    }
                }
            }

            @JavascriptInterface
            fun onBranchConfirmed(branchId: String) {
                runOnUiThread {
                    val branch = allBranches.find { it.id == branchId }
                    if (branch != null) {
                        Toast.makeText(this@ExchangeRateActivity, "${branch.name}\n${branch.address}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            @JavascriptInterface
            fun onMapReady() {
                runOnUiThread {
                    isBranchMapReady = true
                    pushBranchesToMap(filteredBranches)
                }
            }
        }, "AndroidBridge")

        webViewTabBranchMap.loadUrl("file:///android_asset/branch_map.html")

        // Search text watcher
        inputTabSearchBranch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val q = s?.toString()?.trim() ?: ""
                btnTabClearSearch.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
                filterBranches(q)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnTabClearSearch.bounceClick {
            inputTabSearchBranch.setText("")
        }

        btnTabRecenterMap.bounceClick {
            webViewTabBranchMap.evaluateJavascript("window.setUserLocation(-6.2146, 106.8217);", null)
        }

        loadBranches()
    }

    private fun loadBranches() {
        allBranches = AppStorage.get().getBranches()
        filteredBranches = allBranches
        branchAdapter.updateData(filteredBranches)

        AppStorage.get().syncBranchesFromFirestore { updated ->
            runOnUiThread {
                if (updated.isNotEmpty()) {
                    allBranches = updated
                    val q = inputTabSearchBranch.text.toString().trim()
                    filterBranches(q)
                }
            }
        }
    }

    private fun filterBranches(query: String) {
        filteredBranches = if (query.isEmpty()) {
            allBranches
        } else {
            allBranches.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true)
            }
        }

        branchAdapter.updateData(filteredBranches)
        pushBranchesToMap(filteredBranches)

        if (filteredBranches.isEmpty()) {
            layoutTabEmptyBranches.visibility = View.VISIBLE
            recyclerTabBranches.visibility = View.GONE
        } else {
            layoutTabEmptyBranches.visibility = View.GONE
            recyclerTabBranches.visibility = View.VISIBLE
        }
    }

    private fun pushBranchesToMap(branches: List<BranchModel>) {
        if (!isBranchMapReady) return
        val json = gson.toJson(branches)
        webViewTabBranchMap.evaluateJavascript("window.setBranches($json);", null)
    }

    private fun selectTab(index: Int) {
        val tabs = listOf(tabRates, tabConverter, tabBranches)
        val views = listOf(viewTabRates, viewTabConverter, viewTabBranches)

        tabs.forEachIndexed { i, tab ->
            if (i == index) {
                tab.setBackgroundResource(R.drawable.bg_figma_tab_active)
                tab.setTextColor(getColor(android.R.color.white))
            } else {
                tab.setBackgroundResource(android.R.color.transparent)
                tab.setTextColor(getColor(R.color.figma_text_muted))
            }
        }

        views.forEachIndexed { i, view ->
            view.visibility = if (i == index) View.VISIBLE else View.GONE
        }

        if (index == 2 && isBranchMapReady) {
            pushBranchesToMap(filteredBranches)
        }
    }
}
