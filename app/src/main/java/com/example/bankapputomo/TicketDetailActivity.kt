package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle

class TicketDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forwardIntent = Intent(this, CustomerSupportActivity::class.java).apply {
            intent.extras?.let { putExtras(it) }
        }
        startActivity(forwardIntent)
        finish()
    }

    companion object {
        const val EXTRA_TICKET_ID = "ticket_id"
        const val EXTRA_TICKET_SUBJECT = "ticket_subject"
        const val EXTRA_TICKET_CATEGORY = "ticket_category"
        const val EXTRA_TICKET_STATUS = "ticket_status"
    }
}
