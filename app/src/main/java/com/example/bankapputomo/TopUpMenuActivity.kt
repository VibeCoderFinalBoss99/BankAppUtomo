package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class TopUpMenuActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_up_menu)

        findViewById<View>(R.id.btnBack).bounceClick {
            finish()
        }

        findViewById<View>(R.id.cardTopupPulsa).bounceClick {
            startActivity(Intent(this, MobilePrepaidActivity::class.java))
        }

        findViewById<View>(R.id.cardTopupEWallet).bounceClick {
            startActivity(Intent(this, EWalletTopUpActivity::class.java))
        }

        findViewById<View>(R.id.cardTopupElectricity).bounceClick {
            startActivity(Intent(this, BillPaymentActivity::class.java))
        }
    }
}
