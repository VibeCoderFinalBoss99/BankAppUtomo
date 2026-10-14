package com.example.bankapputomo

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

open class BaseActivity : AppCompatActivity() {

    private var initialRootPaddingTop: Int = -1
    private var initialRootPaddingBottom: Int = -1

    protected fun registerBackHandler(handler: () -> Unit) {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handler()
            }
        })
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.bankapputomo.util.LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        com.example.bankapputomo.util.LocaleHelper.applyAppLocale(this)
        val isDarkMode = com.example.bankapputomo.util.AppStorage.get(this).isDarkMode
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyEdgeToEdgeInsets()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyEdgeToEdgeInsets()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applyEdgeToEdgeInsets()
    }

    protected open fun applyEdgeToEdgeInsets() {
        val root = findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) ?: return

        // Set status bar icons light/dark appearance based on night mode
        val isDarkMode = com.example.bankapputomo.util.AppStorage.get(this).isDarkMode
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isDarkMode

        val insetTarget = if (root is androidx.drawerlayout.widget.DrawerLayout) {
            root.getChildAt(0) as? ViewGroup ?: root
        } else {
            root
        }

        ViewCompat.setOnApplyWindowInsetsListener(insetTarget) { view, windowInsets ->
            val statusBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            if (initialRootPaddingTop == -1) {
                initialRootPaddingTop = view.paddingTop
            }
            if (initialRootPaddingBottom == -1) {
                initialRootPaddingBottom = view.paddingBottom
            }
            val navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(navigationBars.bottom, ime.bottom)
            view.setPadding(
                view.paddingLeft,
                initialRootPaddingTop + statusBars.top,
                view.paddingRight,
                initialRootPaddingBottom + bottomInset
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(insetTarget)
    }

    private var maintenanceListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var accountStatusListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onResume() {
        super.onResume()
        checkAppSecurityState()
    }

    override fun onPause() {
        super.onPause()
        maintenanceListener?.remove()
        accountStatusListener?.remove()
    }

    private fun checkAppSecurityState() {
        if (javaClass.simpleName in setOf("MaintenanceActivity", "SuspendedAccountActivity", "SplashActivity")) {
            return
        }

        val storage = com.example.bankapputomo.util.AppStorage.get(this)
        if (storage.accountStatus == "suspended") {
            val intent = android.content.Intent(this, SuspendedAccountActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("reason", storage.accountSuspendedReason)
            }
            startActivity(intent)
            finish()
            return
        }

        maintenanceListener = com.example.bankapputomo.util.BackOfficeRepository.listenToMaintenanceMode(this) { isUnderMaintenance, title, message, endsAt ->
            if (isUnderMaintenance) {
                val intent = android.content.Intent(this, MaintenanceActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("title", title)
                    putExtra("message", message)
                    if (endsAt != null) putExtra("endsAt", endsAt)
                }
                startActivity(intent)
                finish()
            }
        }

        if (storage.isLoggedIn) {
            accountStatusListener = com.example.bankapputomo.util.BackOfficeRepository.listenToAccountStatus(this) { isSuspended, reason ->
                if (isSuspended) {
                    val intent = android.content.Intent(this, SuspendedAccountActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("reason", reason)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
