package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class CardDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_detail)

        val btnBack = findViewById<View>(R.id.btnBack)
        btnBack.bounceClick { finish() }

        val cardDetailHolderName = findViewById<TextView>(R.id.cardDetailHolderName)
        val cardDetailBalance = findViewById<TextView>(R.id.cardDetailBalance)

        val storage = AppStorage.get()
        val cardVariant = intent.getIntExtra("card_variant", 1)
        cardDetailHolderName.text = storage.userName.ifEmpty { "John Smith" }
        cardDetailBalance.text = IdrFormatter.format(storage.debitBalance)

        val textCvv = findViewById<TextView>(R.id.textCvv)
        var isCvvRevealed = false
        textCvv.bounceClick {
            isCvvRevealed = !isCvvRevealed
            textCvv.text = if (isCvvRevealed) "789" else "•••"
        }

        val switchOnline = findViewById<SwitchCompat>(R.id.switchOnlineTx)
        switchOnline.isChecked = storage.cardOnlineTransactionsEnabled
        switchOnline.setOnCheckedChangeListener { _, isChecked ->
            storage.cardOnlineTransactionsEnabled = isChecked
        }

        val cards = storage.getCards()
        val card = cards.getOrNull(cardVariant - 1) ?: cards.firstOrNull()

        val switchFreeze = findViewById<SwitchCompat>(R.id.switchFreeze)
        val layoutStaffBlocked = findViewById<View>(R.id.layoutStaffBlockedNotice)
        val btnContactSupport = findViewById<View>(R.id.btnContactSupportForCard)

        val isLockedByStaff = card?.lockedByStaff == true || card?.status == "blocked"
        if (isLockedByStaff) {
            switchFreeze.isChecked = true
            switchFreeze.isEnabled = false
            layoutStaffBlocked?.visibility = View.VISIBLE
            btnContactSupport?.bounceClick {
                val last4 = if (!card?.cardNumber.isNullOrEmpty()) card!!.cardNumber.takeLast(4) else "0000"
                val intent = Intent(this, CustomerSupportActivity::class.java).apply {
                    putExtra("prefill_category", getString(R.string.ticket_category_card))
                    putExtra("prefill_subject", "Bantuan Pembukaan Blokir Kartu **** $last4")
                }
                startActivity(intent)
            }
        } else {
            layoutStaffBlocked?.visibility = View.GONE
            switchFreeze.isEnabled = true
            switchFreeze.isChecked = card?.status == "frozen" || cardVariant !in storage.getActiveCardVariants()
            switchFreeze.setOnCheckedChangeListener { _, isChecked ->
                storage.setCardVariantActive(cardVariant, !isChecked)
                card?.status = if (isChecked) "frozen" else "active"
                card?.isBlocked = isChecked
                if (card != null) {
                    val list = storage.getCards()
                    val idx = list.indexOfFirst { it.id == card.id }
                    if (idx >= 0) {
                        list[idx] = card
                        storage.saveCards(list)
                    }
                }
            }
        }

        findViewById<View>(R.id.btnChangePin).bounceClick {
            showCardPinSheet()
        }
    }

    private fun showCardPinSheet() {
        val dialog = BottomSheetDialog(this)
        val sheet = layoutInflater.inflate(R.layout.bottom_sheet_change_pin, null)
        dialog.setContentView(sheet)

        val input = sheet.findViewById<TextView>(R.id.inputNewPin)
        sheet.findViewById<View>(R.id.btnSaveNewPin).bounceClick {
            val pin = input.text.toString().trim()
            if (pin.length == 6 && pin.all(Char::isDigit)) {
                AppStorage.get().userPin = pin
                dialog.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.pin_must_be_six_digits), Toast.LENGTH_SHORT).show()
            }
        }
        sheet.findViewById<View>(R.id.btnCancelPin).bounceClick { dialog.dismiss() }
        dialog.show()
    }
}
