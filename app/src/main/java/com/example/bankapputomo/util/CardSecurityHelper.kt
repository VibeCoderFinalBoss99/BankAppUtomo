package com.example.bankapputomo.util

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.TextView
import com.example.bankapputomo.R
import com.example.bankapputomo.CustomerSupportActivity
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

object CardSecurityHelper {

    fun isPrimaryCardActive(context: Context): Boolean {
        val storage = AppStorage.get(context)
        val cards = storage.getCards()
        val primaryCard = cards.firstOrNull() ?: return true
        return (primaryCard.status == "active" || primaryCard.status.isEmpty()) && !primaryCard.isBlocked
    }

    fun getPrimaryCard(context: Context): CardModel? {
        val storage = AppStorage.get(context)
        return storage.getCards().firstOrNull()
    }

    fun showCardLockedBottomSheet(context: Context, card: CardModel? = null) {
        val targetCard = card ?: getPrimaryCard(context)
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_card_locked_error, null)
        dialog.setContentView(view)

        val textTitle = view.findViewById<TextView>(R.id.textLockedErrorTitle)
        val textDesc = view.findViewById<TextView>(R.id.textLockedErrorDesc)
        val btnSupport = view.findViewById<MaterialButton>(R.id.btnLockedContactSupport)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnLockedCancel)

        val isBlocked = targetCard?.status == "blocked" || targetCard?.isBlocked == true
        if (isBlocked) {
            textTitle?.text = context.getString(R.string.card_blocked_error_title)
            textDesc?.text = context.getString(R.string.card_blocked_error_desc)
        } else {
            textTitle?.text = context.getString(R.string.card_status_frozen)
            textDesc?.text = context.getString(R.string.card_frozen_error_desc)
        }

        btnSupport?.bounceClick {
            dialog.dismiss()
            val last4 = if (!targetCard?.cardNumber.isNullOrEmpty()) targetCard!!.cardNumber.takeLast(4) else "0000"
            val intent = Intent(context, CustomerSupportActivity::class.java).apply {
                putExtra("prefill_category", context.getString(R.string.ticket_category_card))
                putExtra("prefill_subject", "Bantuan Status Kartu **** $last4")
            }
            context.startActivity(intent)
        }

        btnCancel?.bounceClick { dialog.dismiss() }
        dialog.show()
    }
}
