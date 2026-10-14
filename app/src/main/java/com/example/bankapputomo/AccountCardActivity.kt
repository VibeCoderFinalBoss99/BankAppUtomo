package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.AvatarHelper
import com.example.bankapputomo.util.BottomNavLottieHelper
import com.example.bankapputomo.util.CardDeckHelper
import com.example.bankapputomo.util.CardModel
import com.example.bankapputomo.util.EditProfileDialogFragment
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class AccountCardActivity : BaseActivity() {

    override fun onResume() {
        super.onResume()
        val storage = AppStorage.get()
        findViewById<TextView>(R.id.textAccountUserName)?.text = storage.userName.ifEmpty { "Nasabah" }
        findViewById<TextView>(R.id.textAccount1Balance)?.text = IdrFormatter.format(storage.debitBalance)
        findViewById<TextView>(R.id.textAccount2Balance)?.text = IdrFormatter.format(storage.savingsBalance)
        findViewById<ImageView>(R.id.accountAvatar)?.let { AvatarHelper.loadAvatar(this, it) }
        renderCards()
        updateSpendingLimitUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_card)

        val btnBack = findViewById<View>(R.id.btnBack)
        val tabAccount = findViewById<TextView>(R.id.tabAccount)
        val tabCard = findViewById<TextView>(R.id.tabCard)
        val layoutAccountView = findViewById<LinearLayout>(R.id.layoutAccountView)
        val layoutCardView = findViewById<LinearLayout>(R.id.layoutCardView)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.accountBottomNavigation)

        val textAccountUserName = findViewById<TextView>(R.id.textAccountUserName)
        val textAccount1Balance = findViewById<TextView>(R.id.textAccount1Balance)
        val textAccount2Balance = findViewById<TextView>(R.id.textAccount2Balance)
        val accountAvatar = findViewById<ImageView>(R.id.accountAvatar)

        val storage = AppStorage.get()
        textAccountUserName?.text = storage.userName.ifEmpty { "Nasabah" }
        textAccount1Balance?.text = IdrFormatter.format(storage.debitBalance)
        textAccount2Balance?.text = IdrFormatter.format(storage.savingsBalance)
        if (accountAvatar != null) {
            AvatarHelper.loadAvatar(this, accountAvatar)
        }

        // Profile edit trigger on avatar & header
        val openProfileEditor = {
            val dialog = EditProfileDialogFragment()
            dialog.onProfileUpdated = {
                val s = AppStorage.get()
                textAccountUserName?.text = s.userName.ifEmpty { "Nasabah" }
                if (accountAvatar != null) {
                    AvatarHelper.loadAvatar(this, accountAvatar)
                }
            }
            dialog.show(supportFragmentManager, "EditProfileDialog")
        }
        findViewById<View>(R.id.layoutAccountProfileHeader)?.bounceClick { openProfileEditor() }
        accountAvatar?.bounceClick { openProfileEditor() }

        btnBack.bounceClick { finish() }

        // Tab toggle logic
        val showAccountView = {
            tabAccount.setBackgroundResource(R.drawable.bg_figma_tab_active)
            tabAccount.setTextColor(getColor(R.color.white))
            tabCard.setBackgroundResource(R.drawable.bg_figma_tab_inactive)
            tabCard.setTextColor(getColor(R.color.figma_text_muted))
            layoutAccountView.visibility = View.VISIBLE
            layoutCardView.visibility = View.GONE
        }
        val showCardView = {
            tabCard.setBackgroundResource(R.drawable.bg_figma_tab_active)
            tabCard.setTextColor(getColor(R.color.white))
            tabAccount.setBackgroundResource(R.drawable.bg_figma_tab_inactive)
            tabAccount.setTextColor(getColor(R.color.figma_text_muted))
            layoutCardView.visibility = View.VISIBLE
            layoutAccountView.visibility = View.GONE
            renderCards()
        }

        tabAccount.bounceClick {
            showAccountView()
        }

        tabCard.bounceClick {
            showCardView()
        }

        // Header '+' button opens Add Card bottom sheet directly & switches to card view
        findViewById<View>(R.id.btnHeaderAddCard).bounceClick {
            showCardView()
            CardDeckHelper(this, findViewById(android.R.id.content)).showAddCardBottomSheet {
                renderCards()
            }
        }

        renderCards()
        updateSpendingLimitUI()

        findViewById<View>(R.id.cardAccountSpendingLimit)?.bounceClick {
            showEditSpendingLimitBottomSheet()
        }
        findViewById<View>(R.id.btnEditAccountSpendingLimit)?.bounceClick {
            showEditSpendingLimitBottomSheet()
        }

        // Bottom Navigation with matching animated feedback & QR FAB
        val bottomNavLottie = BottomNavLottieHelper(bottomNavigation)
        bottomNavLottie.setup()

        val handleBottomNav = { itemId: Int ->
            bottomNavLottie.onItemSelected(itemId)
            when (itemId) {
                R.id.bottom_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                }
                R.id.bottom_history -> {
                    startActivity(Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("open_history", true)
                    })
                    finish()
                }
                R.id.bottom_qr -> {
                    startActivity(Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("open_qr", true)
                    })
                    finish()
                }
                R.id.bottom_statistics -> {
                    startActivity(Intent(this, StatisticsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                }
                R.id.bottom_setting -> {
                    startActivity(Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("open_settings", true)
                    })
                    finish()
                }
            }
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            handleBottomNav(item.itemId)
            true
        }
        bottomNavigation.setOnItemReselectedListener { item ->
            handleBottomNav(item.itemId)
        }

        // Elevated QRIS FAB button
        findViewById<View>(R.id.fabQr)?.bounceClick {
            startActivity(Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_qr", true)
            })
            finish()
        }

        if (intent.getBooleanExtra("open_cards", false)) {
            showCardView()
        }

        // Account management opens the existing Settings surface
        findViewById<View>(R.id.btnAccountManagement).bounceClick {
            startActivity(Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_settings", true)
            })
            finish()
        }

        // Currency Exchange
        findViewById<View>(R.id.btnGoToExchange).bounceClick {
            startActivity(Intent(this, ExchangeActivity::class.java))
        }

        // Add Card Button
        findViewById<View>(R.id.btnAddCard).bounceClick {
            CardDeckHelper(this, findViewById(android.R.id.content)).showAddCardBottomSheet {
                renderCards()
            }
        }

        // Credit Card Services Button
        findViewById<View>(R.id.btnCreditCardServices).bounceClick {
            startActivity(Intent(this, CreditCardActivity::class.java))
        }

        // Monthly Spending Limit
        findViewById<View>(R.id.btnEditAccountSpendingLimit)?.bounceClick {
            showEditSpendingLimitBottomSheet()
        }
        findViewById<View>(R.id.textAccountSpendingAmount)?.bounceClick {
            showEditSpendingLimitBottomSheet()
        }
        findViewById<View>(R.id.progressAccountSpending)?.bounceClick {
            showEditSpendingLimitBottomSheet()
        }
        updateSpendingLimitUI()

        if (intent.getBooleanExtra("open_cards", false)) {
            findViewById<View>(R.id.accountCardTabs).visibility = View.GONE
            findViewById<TextView>(R.id.headerTitle).text =
                getString(R.string.my_cards_title)
            showCardView()
        }
    }

    private fun renderCards() {
        val container = findViewById<LinearLayout>(R.id.containerAccountCards) ?: return
        container.removeAllViews()
        val cards = AppStorage.get().getCards()
        val inflater = LayoutInflater.from(this)
        val density = resources.displayMetrics.density

        for ((index, card) in cards.withIndex()) {
            val cardView = inflater.inflate(R.layout.item_deck_card, container, false)
            val params = LinearLayout.LayoutParams(
                (335 * density).toInt(),
                (210 * density).toInt()
            ).apply {
                marginEnd = (12 * density).toInt()
            }
            cardView.layoutParams = params

            bindCardView(cardView, card)

            cardView.bounceClick {
                startActivity(Intent(this, CardDetailActivity::class.java).putExtra("card_variant", index + 1))
            }

            container.addView(cardView)
        }
    }

    private fun bindCardView(cardView: View, card: CardModel) {
        val bodyContainer = cardView.findViewById<RelativeLayout>(R.id.cardBodyContainer)
        val textBalance = cardView.findViewById<TextView>(R.id.textDeckCardBalance)
        val textNumber = cardView.findViewById<TextView>(R.id.textDeckCardNumber)
        val textHolder = cardView.findViewById<TextView>(R.id.textDeckCardHolder)
        val textExpiry = cardView.findViewById<TextView>(R.id.textDeckCardExpiry)
        val iconLogo = cardView.findViewById<ImageView>(R.id.iconDeckNetworkLogo)
        val blockedOverlay = cardView.findViewById<View>(R.id.layoutDeckBlockedOverlay)
        val textBlockedLabel = cardView.findViewById<TextView>(R.id.textDeckBlockedLabel)
        val textBlockedReason = cardView.findViewById<TextView>(R.id.textDeckBlockedReason)

        when (card.gradientType) {
            1 -> bodyContainer.setBackgroundResource(R.drawable.bg_card_gradient_purple_orange)
            2 -> bodyContainer.setBackgroundResource(R.drawable.bg_card_gradient_navy_teal)
            3 -> bodyContainer.setBackgroundResource(R.drawable.bg_card_gradient_amber_orange)
            else -> bodyContainer.setBackgroundResource(R.drawable.bg_card_gradient_purple_orange)
        }

        textBalance.text = IdrFormatter.format(card.balance)
        textNumber.text = card.cardNumber
        textHolder.text = card.cardHolder
        textExpiry.text = card.expiry

        if (card.cardType.equals("Credit", ignoreCase = true)) {
            iconLogo.setImageResource(R.drawable.ic_figma_mastercard)
        } else {
            iconLogo.setImageResource(R.drawable.ic_figma_visa)
        }

        val isCardLocked = card.status == "blocked" || card.status == "frozen" || card.isBlocked
        blockedOverlay.visibility = if (isCardLocked) View.VISIBLE else View.GONE

        if (isCardLocked) {
            val isFrozen = card.status == "frozen"
            val lang = AppStorage.get().language
            textBlockedLabel?.text = if (isFrozen) {
                if (lang == "en") "CARD FROZEN" else "KARTU DIBEKUKAN"
            } else {
                if (lang == "en") "CARD BLOCKED" else "KARTU DIBLOKIR"
            }

            val reasonDesc = when (card.statusReasonCode) {
                "lost" -> if (lang == "en") "Lost Card" else "Laporan Kartu Hilang"
                "stolen" -> if (lang == "en") "Stolen Card" else "Laporan Kartu Dicuri"
                "fraud_suspected" -> if (lang == "en") "Fraud Protection" else "Proteksi Transaksi Mencurigakan"
                "customer_request" -> if (lang == "en") "Customer Request" else "Permintaan Nasabah"
                else -> if (card.lockedByStaff) (if (lang == "en") "Administrative Hold" else "Pengamanan Sistem Staf") else ""
            }
            if (reasonDesc.isNotEmpty()) {
                textBlockedReason?.visibility = View.VISIBLE
                textBlockedReason?.text = reasonDesc
            } else {
                textBlockedReason?.visibility = View.GONE
            }
        }
    }

    private fun updateSpendingLimitUI() {
        val storage = AppStorage.get()
        val limit = storage.monthlySpendingLimit
        val spent = storage.getMonthlyExpense()

        val percent = if (limit > 0) ((spent.toDouble() / limit.toDouble()) * 100).toInt().coerceIn(0, 100) else 0

        findViewById<TextView>(R.id.textAccountSpendingAmount)?.text =
            "${IdrFormatter.format(spent)} / ${IdrFormatter.format(limit)}"
        findViewById<ProgressBar>(R.id.progressAccountSpending)?.progress = percent
    }

    private fun showEditSpendingLimitBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_edit_spending_limit, null)
        dialog.setContentView(sheetView)

        val storage = AppStorage.get()
        val currentLimit = storage.monthlySpendingLimit
        val currentSpent = storage.getMonthlyExpense()

        val textCurrentSpentSubtitle = sheetView.findViewById<TextView>(R.id.textCurrentSpentSubtitle)
        val inputSpendingLimit = sheetView.findViewById<android.widget.EditText>(R.id.inputSpendingLimit)
        val chipLimit5M = sheetView.findViewById<TextView>(R.id.chipLimit5M)
        val chipLimit10M = sheetView.findViewById<TextView>(R.id.chipLimit10M)
        val chipLimit15M = sheetView.findViewById<TextView>(R.id.chipLimit15M)
        val chipLimit25M = sheetView.findViewById<TextView>(R.id.chipLimit25M)
        val btnSave = sheetView.findViewById<View>(R.id.btnSaveSpendingLimit)
        val btnCancel = sheetView.findViewById<View>(R.id.btnCancelSpendingLimit)

        textCurrentSpentSubtitle.text = "Pengeluaran bulan ini: ${IdrFormatter.format(currentSpent)}"
        inputSpendingLimit.setText(currentLimit.toString())

        val chips = listOf(
            chipLimit5M to 5000000L,
            chipLimit10M to 10000000L,
            chipLimit15M to 15000000L,
            chipLimit25M to 25000000L
        )

        fun updateChipsSelection(selectedVal: Long) {
            chips.forEach { (chip, amount) ->
                if (amount == selectedVal) {
                    chip.setBackgroundResource(R.drawable.bg_figma_button_primary)
                    chip.setTextColor(getColor(android.R.color.white))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_figma_card)
                    chip.setTextColor(getColor(R.color.figma_text_title))
                }
            }
        }

        updateChipsSelection(currentLimit)

        chips.forEach { (chip, amount) ->
            chip.bounceClick {
                inputSpendingLimit.setText(amount.toString())
                updateChipsSelection(amount)
            }
        }

        btnSave.bounceClick {
            val entered = inputSpendingLimit.text.toString().trim().toLongOrNull() ?: currentLimit
            if (entered <= 0) {
                Toast.makeText(this, "Batas pengeluaran harus lebih besar dari 0", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            storage.monthlySpendingLimit = entered
            updateSpendingLimitUI()
            dialog.dismiss()
            Toast.makeText(this, "Batas pengeluaran berhasil disimpan: ${IdrFormatter.format(entered)}", Toast.LENGTH_SHORT).show()
        }

        btnCancel.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }
}
