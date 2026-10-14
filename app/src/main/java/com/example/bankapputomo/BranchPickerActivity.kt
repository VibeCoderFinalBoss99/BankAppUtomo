package com.example.bankapputomo

import android.app.Activity
import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BranchModel
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.gson.Gson

class BranchPickerActivity : BaseActivity() {

    companion object {
        const val EXTRA_BRANCH_ID = "EXTRA_BRANCH_ID"
        const val EXTRA_BRANCH_NAME = "EXTRA_BRANCH_NAME"
        const val EXTRA_BRANCH_ADDRESS = "EXTRA_BRANCH_ADDRESS"
        const val EXTRA_BRANCH_FULL = "EXTRA_BRANCH_FULL"
    }

    private lateinit var webViewBranchMap: WebView
    private lateinit var btnBack: ImageView
    private lateinit var btnRecenterMap: ImageView
    private lateinit var inputSearchBranch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var recyclerBranches: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutSelectedAction: LinearLayout
    private lateinit var textSelectedBranchPrompt: TextView
    private lateinit var btnConfirmSelection: TextView

    private lateinit var branchAdapter: BranchAdapter
    private var allBranches: List<BranchModel> = emptyList()
    private var filteredBranches: List<BranchModel> = emptyList()
    private var selectedBranch: BranchModel? = null
    private var isMapReady = false
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_branch_picker)

        btnBack = findViewById(R.id.btnBack)
        btnRecenterMap = findViewById(R.id.btnRecenterMap)
        webViewBranchMap = findViewById(R.id.webViewBranchMap)
        inputSearchBranch = findViewById(R.id.inputSearchBranch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        recyclerBranches = findViewById(R.id.recyclerBranches)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        layoutSelectedAction = findViewById(R.id.layoutSelectedAction)
        textSelectedBranchPrompt = findViewById(R.id.textSelectedBranchPrompt)
        btnConfirmSelection = findViewById(R.id.btnConfirmSelection)

        btnBack.bounceClick {
            finish()
        }

        setupRecyclerView()
        setupWebView()
        setupSearch()
        loadBranchData()

        btnConfirmSelection.bounceClick {
            selectedBranch?.let { confirmAndReturn(it) }
        }

        layoutSelectedAction.bounceClick {
            selectedBranch?.let { confirmAndReturn(it) }
        }

        btnRecenterMap.bounceClick {
            webViewBranchMap.evaluateJavascript("window.setUserLocation(-6.2146, 106.8217);", null)
        }
    }

    private fun setupRecyclerView() {
        branchAdapter = BranchAdapter(emptyList()) { branch ->
            onBranchClicked(branch)
        }
        recyclerBranches.layoutManager = LinearLayoutManager(this)
        recyclerBranches.adapter = branchAdapter
    }

    private fun setupWebView() {
        webViewBranchMap.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            databaseEnabled = true
        }

        webViewBranchMap.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapReady = true
                pushBranchesToMap(filteredBranches)
            }
        }
        webViewBranchMap.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                android.util.Log.e("LeafletMap", "${consoleMessage?.message()} [${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}]")
                return true
            }
        }

        // Two-way Android - JavaScript Bridge
        webViewBranchMap.addJavascriptInterface(object {
            @JavascriptInterface
            fun onBranchSelected(branchId: String) {
                runOnUiThread {
                    val found = allBranches.find { it.id == branchId }
                    if (found != null) {
                        selectBranch(found, centerMap = false)
                    }
                }
            }

            @JavascriptInterface
            fun onBranchConfirmed(branchId: String) {
                runOnUiThread {
                    val found = allBranches.find { it.id == branchId }
                    if (found != null) {
                        confirmAndReturn(found)
                    }
                }
            }

            @JavascriptInterface
            fun onMapReady() {
                runOnUiThread {
                    isMapReady = true
                    pushBranchesToMap(filteredBranches)
                }
            }
        }, "AndroidBridge")

        webViewBranchMap.loadUrl("file:///android_asset/branch_map.html")
    }

    private fun setupSearch() {
        inputSearchBranch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val query = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterBranches(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.bounceClick {
            inputSearchBranch.setText("")
        }
    }

    private fun loadBranchData() {
        allBranches = AppStorage.get().getBranches()
        filteredBranches = allBranches
        branchAdapter.updateData(filteredBranches)

        // Asynchronously sync latest from Firestore (changes made in Admin Panel)
        AppStorage.get().syncBranchesFromFirestore { updated ->
            runOnUiThread {
                if (updated.isNotEmpty()) {
                    allBranches = updated
                    val query = inputSearchBranch.text.toString().trim()
                    filterBranches(query)
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
                it.city.contains(query, ignoreCase = true) ||
                it.type.contains(query, ignoreCase = true)
            }
        }

        branchAdapter.updateData(filteredBranches)
        pushBranchesToMap(filteredBranches)

        if (filteredBranches.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            recyclerBranches.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            recyclerBranches.visibility = View.VISIBLE
        }
    }

    private fun onBranchClicked(branch: BranchModel) {
        if (selectedBranch?.id == branch.id) {
            confirmAndReturn(branch)
        } else {
            selectBranch(branch, centerMap = true)
        }
    }

    private fun selectBranch(branch: BranchModel, centerMap: Boolean) {
        selectedBranch = branch
        branchAdapter.setSelectedId(branch.id)

        if (centerMap) {
            webViewBranchMap.evaluateJavascript("window.focusBranch('${branch.id}');", null)
        }

        // Show selection prompt banner
        textSelectedBranchPrompt.text = "Gunakan: ${branch.name}?"
        layoutSelectedAction.visibility = View.VISIBLE
    }

    private fun pushBranchesToMap(branches: List<BranchModel>) {
        if (!isMapReady) return
        val json = gson.toJson(branches)
        webViewBranchMap.evaluateJavascript("window.setBranches($json);", null)
    }

    private fun confirmAndReturn(branch: BranchModel) {
        val intent = Intent().apply {
            putExtra(EXTRA_BRANCH_ID, branch.id)
            putExtra(EXTRA_BRANCH_NAME, branch.name)
            putExtra(EXTRA_BRANCH_ADDRESS, branch.address)
            putExtra(EXTRA_BRANCH_FULL, "${branch.name} - ${branch.address}")
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
