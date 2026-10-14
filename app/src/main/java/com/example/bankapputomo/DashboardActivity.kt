package com.example.bankapputomo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class DashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.dashboardSwipeRefresh)

        // Load the requested destination, otherwise open Home.
        if (savedInstanceState == null) {
            handleDestinationIntent(intent, bottomNavigation)
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> {
                    loadFragment(HomeFragment())
                }
                R.id.nav_exchange -> {
                    startActivity(Intent(this, ExchangeActivity::class.java))
                }
                R.id.nav_statistics -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                }
                R.id.nav_language -> {
                    startActivity(Intent(this, LanguageActivity::class.java))
                }
                R.id.nav_profil -> {
                    loadFragment(ProfileFragment())
                }
                R.id.nav_notifikasi -> {
                    startActivity(Intent(this, NotificationActivity::class.java))
                }
                R.id.nav_logout -> {
                    AppStorage.get().isLoggedIn = false
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        val fabQr = findViewById<View>(R.id.fabQr)
        fabQr.bounceClick {
            bottomNavigation.selectedItemId = R.id.bottom_qr
        }

        swipeRefresh.setColorSchemeResources(R.color.figma_primary, R.color.figma_secondary)
        swipeRefresh.setOnRefreshListener {
            val loading = com.example.bankapputomo.util.LoadingDialog.create(this, getString(R.string.syncing_accounts))
            loading.show()
            swipeRefresh.postDelayed({
                loading.dismiss()
                swipeRefresh.isRefreshing = false
        refreshSelectedTab(bottomNavigation.selectedItemId)
                Toast.makeText(this, getString(R.string.refreshed), Toast.LENGTH_SHORT).show()
            }, 750)
        }

        val bottomNavLottie = com.example.bankapputomo.util.BottomNavLottieHelper(bottomNavigation)
        bottomNavLottie.setup()

        bottomNavigation.setOnItemSelectedListener { item ->
            bottomNavLottie.onItemSelected(item.itemId)
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.bottom_home -> selectedFragment = HomeFragment()
                R.id.bottom_history -> selectedFragment = HistoryFragment()
                R.id.bottom_qr -> selectedFragment = QrFragment()
                R.id.bottom_statistics -> startActivity(Intent(this, StatisticsActivity::class.java))
                R.id.bottom_setting -> selectedFragment = ProfileFragment()
            }
            if (selectedFragment != null) {
                loadFragment(selectedFragment)
            }
            true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (bottomNavigation != null) {
            handleDestinationIntent(intent, bottomNavigation)
        }
    }

    private fun handleDestinationIntent(destIntent: Intent, bottomNav: BottomNavigationView) {
        when {
            destIntent.getBooleanExtra("open_settings", false) -> {
                loadFragment(ProfileFragment())
                bottomNav.selectedItemId = R.id.bottom_setting
            }
            destIntent.getBooleanExtra("open_history", false) -> {
                loadFragment(HistoryFragment())
                bottomNav.selectedItemId = R.id.bottom_history
            }
            destIntent.getBooleanExtra("open_qr", false) -> {
                loadFragment(QrFragment())
                bottomNav.selectedItemId = R.id.bottom_qr
            }
            else -> {
                loadFragment(HomeFragment())
                bottomNav.selectedItemId = R.id.bottom_home
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun refreshSelectedTab(itemId: Int) {
        if (itemId == R.id.bottom_statistics) {
            startActivity(Intent(this, StatisticsActivity::class.java))
            return
        }
        val fragment = when (itemId) {
            R.id.bottom_history -> HistoryFragment()
            R.id.bottom_qr -> QrFragment()
            R.id.bottom_setting -> ProfileFragment()
            else -> HomeFragment()
        }
        loadFragment(fragment)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_beranda -> {
                loadFragment(HomeFragment())
                return true
            }
            R.id.menu_bagikan -> {
                val intentWeb = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://elearning.poliupg.ac.id")
                )
                startActivity(intentWeb)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun applyEdgeToEdgeInsets() {
        val root = findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) ?: return
        val isDarkMode = AppStorage.get(this).isDarkMode
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isDarkMode
        val bottomNavContainer = findViewById<View>(R.id.bottomNavContainer)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val baseBottomNavHeight = (68 * resources.displayMetrics.density).toInt()

        // The container owns the system-bar space. Prevent Material NavigationBarView
        // from applying the same inset a second time to its menu items.
        bottomNavigation?.let { nav ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(nav) { _, windowInsets ->
                androidx.core.view.WindowInsetsCompat.Builder(windowInsets)
                    .setInsets(
                        androidx.core.view.WindowInsetsCompat.Type.navigationBars(),
                        androidx.core.graphics.Insets.NONE
                    )
                    .setInsets(
                        androidx.core.view.WindowInsetsCompat.Type.ime(),
                        androidx.core.graphics.Insets.NONE
                    )
                    .build()
            }
        }

        val applyInsets: (androidx.core.view.WindowInsetsCompat) -> Unit = { windowInsets ->
            val statusBars = windowInsets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout()
            )
            val navigationBars = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            val ime = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(navigationBars.bottom, ime.bottom)

            findViewById<View>(R.id.dashboardSwipeRefresh)?.setPadding(0, statusBars.top, 0, 0)
            bottomNavigation?.visibility = View.VISIBLE
            bottomNavContainer?.visibility = View.VISIBLE
            bottomNavigation?.let { nav ->
                val navParams = nav.layoutParams
                navParams.height = baseBottomNavHeight
                nav.layoutParams = navParams
                nav.setPadding(0, 0, 0, 0)
            }
            bottomNavContainer?.let { container ->
                val containerParams = container.layoutParams
                containerParams.height = baseBottomNavHeight + bottomInset
                container.layoutParams = containerParams
                container.setPadding(0, 0, 0, 0)
            }

            val fab = findViewById<View>(R.id.fabQr)
            if (fab != null) {
                val baseMargin = (28 * resources.displayMetrics.density).toInt()
                val lp = fab.layoutParams as? android.widget.FrameLayout.LayoutParams
                if (lp != null) {
                    lp.bottomMargin = baseMargin + bottomInset
                    fab.layoutParams = lp
                }
            }
        }

        // Apply immediately if insets are already present
        androidx.core.view.ViewCompat.getRootWindowInsets(window.decorView)?.let {
            applyInsets(it)
        }

        // Apply to the activity root so portrait and landscape use the same inset contract.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            applyInsets(windowInsets)
            windowInsets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(root)
    }

    override fun onResume() {
        super.onResume()
        androidx.core.view.ViewCompat.requestApplyInsets(window.decorView)
    }
}
