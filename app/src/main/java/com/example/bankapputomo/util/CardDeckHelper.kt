package com.example.bankapputomo.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.bankapputomo.CardDetailActivity
import com.example.bankapputomo.R
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog

class CardDeckHelper(
    private val activity: Activity,
    private val deckContainer: ViewGroup,
    private val onCardChanged: ((CardModel?) -> Unit)? = null
) {

    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private val density: Float = activity.resources.displayMetrics.density
    private var isAnimating = false

    fun setup(onlyActiveCards: Boolean = true) {
        renderDeck(animateFromBack = false, onlyActiveCards = onlyActiveCards)
    }

    fun renderDeck(animateFromBack: Boolean = false, onlyActiveCards: Boolean = true) {
        val allCards = AppStorage.get().getCards()
        val cards = if (onlyActiveCards) {
            allCards.filter { (it.status == "active" || it.status.isEmpty()) && !it.isBlocked }
        } else {
            allCards
        }
        deckContainer.removeAllViews()

        if (cards.isEmpty()) {
            onCardChanged?.invoke(null)
            return
        }

        onCardChanged?.invoke(cards.first())

        // Show up to 3 cards in the visible stack (Picture 5 style)
        val visibleCount = kotlin.math.min(cards.size, 3)
        // Add from back to front so front is drawn on top
        for (i in (visibleCount - 1) downTo 0) {
            val card = cards[i]
            val cardView = inflater.inflate(R.layout.item_deck_card, deckContainer, false)
            bindCardView(cardView, card)

            val targetTranslationY = (i * 16 * density)
            val targetScale = 1.0f - (i * 0.05f)
            val targetElevation = ((visibleCount - i) * 6 * density)

            cardView.pivotY = 0f
            cardView.scaleX = targetScale
            cardView.scaleY = targetScale
            cardView.elevation = targetElevation
            cardView.findViewById<View>(R.id.cardBodyContainer)?.elevation = targetElevation

            if (animateFromBack && i == visibleCount - 1) {
                cardView.translationY = -60f * density
                cardView.alpha = 0.5f
                cardView.animate()
                    .translationY(targetTranslationY)
                    .alpha(1.0f)
                    .setDuration(220)
                    .start()
            } else if (animateFromBack && i == 0) {
                cardView.translationY = 16f * density
                cardView.scaleX = 0.95f
                cardView.scaleY = 0.95f
                cardView.animate()
                    .translationY(0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(220)
                    .start()
            } else {
                cardView.translationY = targetTranslationY
            }

            if (i == 0) {
                setupFrontCardTouchAndClick(cardView, card)
            } else {
                cardView.setOnClickListener {
                    cycleCardForward()
                }
            }

            deckContainer.addView(cardView)
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

    private fun setupFrontCardTouchAndClick(frontView: View, card: CardModel) {
        var startY = 0f
        var isDragging = false

        frontView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - startY
                    if (deltaY < -20 * density && !isAnimating) {
                        isDragging = true
                        v.translationY = deltaY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val deltaY = event.rawY - startY
                    if (isDragging && deltaY < -60 * density) {
                        cycleCardForward()
                    } else if (!isDragging && kotlin.math.abs(deltaY) < 15 * density) {
                        // Tapped: cycle card or open detail
                        cycleCardForward()
                    } else {
                        // Spring back to 0
                        v.animate().translationY(0f).setDuration(150).start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun cycleCardForward() {
        if (isAnimating) return
        val cards = AppStorage.get().getCards()
        if (cards.size <= 1) return

        val frontView = deckContainer.getChildAt(deckContainer.childCount - 1) ?: return
        isAnimating = true

        // Up and Go to the Back animation:
        // 1. Move up and shrink slightly
        frontView.animate()
            .translationY(-140f * density)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .alpha(0.7f)
            .setDuration(240)
            .withEndAction {
                // Rotate the list
                val topCard = cards.removeAt(0)
                cards.add(topCard)
                AppStorage.get().saveCards(cards)

                // Re-render
                renderDeck(animateFromBack = true)
                isAnimating = false
            }
            .start()
    }

    fun showAddCardBottomSheet(onAdded: (() -> Unit)? = null) {
        val dialog = BottomSheetDialog(activity)
        val sheetView = activity.layoutInflater.inflate(R.layout.bottom_sheet_add_card, null)
        dialog.setContentView(sheetView)

        val chipDebit = sheetView.findViewById<TextView>(R.id.chipAddCardDebit)
        val chipCredit = sheetView.findViewById<TextView>(R.id.chipAddCardCredit)
        val theme1 = sheetView.findViewById<View>(R.id.colorThemePurpleOrange)
        val theme2 = sheetView.findViewById<View>(R.id.colorThemeNavyTeal)
        val theme3 = sheetView.findViewById<View>(R.id.colorThemeAmber)
        val textPreview = sheetView.findViewById<TextView>(R.id.textPreviewRandomCardNumber)
        val btnSubmit = sheetView.findViewById<View>(R.id.btnSubmitAddCard)
        val btnCancel = sheetView.findViewById<View>(R.id.btnCancelAddCard)

        var selectedType = "Debit"
        var selectedTheme = 1

        fun generateRandomPreview(type: String): String {
            val prefix = if (type == "Debit") "4" else "5"
            val rest = (1..15).map { (0..9).random() }.joinToString("")
            return (prefix + rest).chunked(4).joinToString(" ")
        }

        var currentRandomNumber = generateRandomPreview(selectedType)
        textPreview.text = currentRandomNumber

        chipDebit.bounceClick {
            selectedType = "Debit"
            currentRandomNumber = generateRandomPreview(selectedType)
            textPreview.text = currentRandomNumber
            chipDebit.setBackgroundResource(R.drawable.bg_figma_button_primary)
            chipDebit.setTextColor(ContextCompat.getColor(activity, android.R.color.white))
            chipCredit.setBackgroundResource(R.drawable.bg_figma_card)
            chipCredit.setTextColor(ContextCompat.getColor(activity, R.color.figma_text_title))
        }

        chipCredit.bounceClick {
            selectedType = "Credit"
            currentRandomNumber = generateRandomPreview(selectedType)
            textPreview.text = currentRandomNumber
            chipCredit.setBackgroundResource(R.drawable.bg_figma_button_primary)
            chipCredit.setTextColor(ContextCompat.getColor(activity, android.R.color.white))
            chipDebit.setBackgroundResource(R.drawable.bg_figma_card)
            chipDebit.setTextColor(ContextCompat.getColor(activity, R.color.figma_text_title))
        }

        listOf(theme1 to 1, theme2 to 2, theme3 to 3).forEach { (view, theme) ->
            view.bounceClick {
                selectedTheme = theme
                Toast.makeText(activity, "Tema $theme dipilih", Toast.LENGTH_SHORT).show()
            }
        }

        btnSubmit.bounceClick {
            val newCard = AppStorage.get().addCard(selectedType, selectedTheme)
            dialog.dismiss()
            Toast.makeText(activity, "Kartu baru berhasil diterbitkan: ${newCard.cardNumber}", Toast.LENGTH_SHORT).show()
            renderDeck()
            onAdded?.invoke()
        }

        btnCancel.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun blockCurrentCard() {
        val cards = AppStorage.get().getCards()
        val current = cards.firstOrNull() ?: return
        val isBlocked = AppStorage.get().toggleBlockCard(current.id)
        val statusMsg = if (isBlocked) "Kartu ${current.cardNumber} berhasil DIBLOKIR" else "Blokir kartu ${current.cardNumber} telah DIBUKA"
        Toast.makeText(activity, statusMsg, Toast.LENGTH_SHORT).show()
        renderDeck()
    }

    fun deleteCurrentCard() {
        val cards = AppStorage.get().getCards()
        val current = cards.firstOrNull() ?: return

        CustomConfirmationDialog.show(
            context = activity,
            title = "Hapus Kartu",
            message = "Apakah Anda yakin ingin menghapus kartu ${current.cardNumber} dari akun Anda?",
            positiveText = "Hapus",
            negativeText = "Batal",
            iconRes = R.drawable.ic_card,
            isDestructive = true,
            onConfirm = {
                AppStorage.get().deleteCard(current.id)
                Toast.makeText(activity, "Kartu berhasil dihapus", Toast.LENGTH_SHORT).show()
                renderDeck()
            }
        )
    }
}
