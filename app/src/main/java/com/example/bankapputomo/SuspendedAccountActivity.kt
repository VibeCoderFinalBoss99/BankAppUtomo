package com.example.bankapputomo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.button.MaterialButton

class SuspendedAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suspended_account)

        val storage = AppStorage.get(this)
        val textReason = findViewById<TextView>(R.id.textSuspendedReason)
        val btnContactSupport = findViewById<MaterialButton>(R.id.btnSuspendedContactSupport)
        val btnSignOut = findViewById<MaterialButton>(R.id.btnSuspendedSignOut)

        val reason = intent.getStringExtra("reason") ?: storage.accountSuspendedReason
        if (reason.isNotEmpty()) {
            textReason.visibility = View.VISIBLE
            textReason.text = "Alasan: $reason"
        } else {
            textReason.visibility = View.GONE
        }

        btnContactSupport.bounceClick {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1500888"))
            try {
                startActivity(callIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Hotline UTOMO Bank: 1500888", Toast.LENGTH_LONG).show()
            }
        }

        btnSignOut.bounceClick {
            storage.isLoggedIn = false
            storage.accountStatus = "active"
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
    }
}
