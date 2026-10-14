package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import com.airbnb.lottie.LottieAnimationView
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.LottieMotionHelper
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.button.MaterialButton
import java.util.Locale

class MaintenanceActivity : AppCompatActivity() {

    private lateinit var textTitle: TextView
    private lateinit var textMessage: TextView
    private lateinit var layoutCountdown: View
    private lateinit var textCountdown: TextView
    private lateinit var btnRetry: MaterialButton
    private var btnSupport: MaterialButton? = null
    private var lottieAnim: LottieAnimationView? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintenance)

        textTitle = findViewById(R.id.textMaintenanceTitle)
        textMessage = findViewById(R.id.textMaintenanceMessage)
        layoutCountdown = findViewById(R.id.layoutMaintenanceCountdown)
        textCountdown = findViewById(R.id.textMaintenanceCountdown)
        btnRetry = findViewById(R.id.btnRetryMaintenance)
        btnSupport = findViewById(R.id.btnMaintenanceSupport)
        lottieAnim = findViewById(R.id.lottieMaintenanceAnim)

        // Initialize 404 Lottie Animation
        lottieAnim?.let {
            it.setAnimation(R.raw.anim_maintenance_404)
            LottieMotionHelper.playLoop(it)
        }

        val intentTitle = intent.getStringExtra("title")
        val intentMessage = intent.getStringExtra("message")
        val endsAt = intent.getLongExtra("endsAt", -1L)

        val defaultTitle = getString(R.string.maintenance_title_default)
        val defaultMessage = getString(R.string.maintenance_desc_default)

        textTitle.text = if (!intentTitle.isNullOrEmpty()) {
            if (!intentTitle.contains("404")) "404 - $intentTitle" else intentTitle
        } else {
            defaultTitle
        }

        textMessage.text = if (!intentMessage.isNullOrEmpty()) {
            intentMessage
        } else {
            defaultMessage
        }

        if (endsAt > System.currentTimeMillis()) {
            layoutCountdown.visibility = View.VISIBLE
            startCountdown(endsAt - System.currentTimeMillis())
        } else {
            layoutCountdown.visibility = View.GONE
        }

        btnRetry.bounceClick {
            checkAndProceed()
        }

        btnSupport?.bounceClick {
            val intent = Intent(this, CustomerSupportActivity::class.java)
            startActivity(intent)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
    }

    private fun startCountdown(millisRemaining: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millisRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60))
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                textCountdown.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                layoutCountdown.visibility = View.GONE
                checkAndProceed()
            }
        }.start()
    }

    private fun checkAndProceed() {
        btnRetry.isEnabled = false
        btnRetry.text = getString(R.string.str_mohon_tunggu)

        BackOfficeRepository.listenToMaintenanceMode(this) { isUnderMaintenance, title, message, endsAt ->
            btnRetry.isEnabled = true
            btnRetry.text = getString(R.string.maintenance_try_again)

            if (!isUnderMaintenance) {
                Toast.makeText(this, "Layanan perbankan telah kembali normal.", Toast.LENGTH_SHORT).show()
                val targetActivity = if (AppStorage.get(this).isLoggedIn) {
                    DashboardActivity::class.java
                } else {
                    LoginActivity::class.java
                }
                val intent = Intent(this, targetActivity).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } else {
                if (title.isNotEmpty()) {
                    textTitle.text = if (!title.contains("404")) "404 - $title" else title
                }
                if (message.isNotEmpty()) {
                    textMessage.text = message
                }
                if (endsAt != null && endsAt > System.currentTimeMillis()) {
                    layoutCountdown.visibility = View.VISIBLE
                    startCountdown(endsAt - System.currentTimeMillis())
                }
                Toast.makeText(this, "Pemeliharaan masih berlangsung.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

}
