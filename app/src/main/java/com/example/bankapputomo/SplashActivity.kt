package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.FirebaseSync
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

open class SplashActivity : AppCompatActivity() {

    private var isMaintenanceActive = false
    private var maintenanceTitle = "Pemeliharaan Sistem"
    private var maintenanceMsg = "Sistem perbankan sedang dalam peningkatan layanan berkala demi kenyamanan transaksi Anda."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        setupSplashAnimation()

        // Periksa status Maintenance Mode secara instan ke Firebase Realtime Database
        try {
            FirebaseDatabase.getInstance(FirebaseSync.FIREBASE_DATABASE_URL)
                .getReference("config/maintenance")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val enabled = snapshot.child("enabled").getValue(Boolean::class.java) ?: false
                            if (enabled) {
                                isMaintenanceActive = true
                                val lang = AppStorage.get(this@SplashActivity).language
                                maintenanceTitle = snapshot.child("title").child(lang).getValue(String::class.java)
                                    ?: snapshot.child("title").child("id").getValue(String::class.java)
                                    ?: "Pemeliharaan Sistem"
                                maintenanceMsg = snapshot.child("message").child(lang).getValue(String::class.java)
                                    ?: snapshot.child("message").child("id").getValue(String::class.java)
                                    ?: "Sistem perbankan sedang dalam peningkatan berkala."
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        } catch (e: Exception) {
            // Silently handle offline fallback
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // Jika Maintenance Mode Aktif, alihkan seketika ke MaintenanceActivity
            if (isMaintenanceActive) {
                val intent = Intent(this, MaintenanceActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("title", maintenanceTitle)
                    putExtra("message", maintenanceMsg)
                }
                startActivity(intent)
                finish()
                return@postDelayed
            }

            val storage = AppStorage.get(this)
            val openNextScreen = {
                if (!storage.hasSeenOnboarding) {
                    startActivity(Intent(this, OnboardingActivity::class.java))
                } else {
                    startActivity(Intent(this, DashboardActivity::class.java))
                }
                finish()
            }

            if (storage.isLoggedIn) {
                // A persisted session is not a persisted business cache.
                // Rehydrate the account before rendering any balance/card UI.
                BackOfficeRepository.hydrateUserData(this) { openNextScreen() }
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }, 3000)
    }

    private fun setupSplashAnimation() {
        val container = findViewById<FrameLayout>(R.id.splashAnimationContainer)
        val preview = findViewById<ImageView>(R.id.splashAnimationPreview)
        val animationView = LottieAnimationView(this)

        animationView.setAnimation(R.raw.utomo_splash)
        animationView.repeatCount = 0
        animationView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        animationView.contentDescription = getString(R.string.app_name)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        preview.visibility = View.GONE
        container.addView(animationView, params)
        animationView.playAnimation()
    }
}
