package com.example.bankapputomo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.bankapputomo.guidance.InAppGuidanceManager
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.CardDeckHelper
import com.example.bankapputomo.util.HomeMenuAnimationHelper
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class HomeFragment : Fragment() {

    private var hasTriggeredGuidanceInSession = false
    private lateinit var greetingView: TextView
    private lateinit var containerQuickHistory: LinearLayout
    private var cardDeckHelper: CardDeckHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        greetingView = view.findViewById(R.id.homeGreeting)
        containerQuickHistory = view.findViewById(R.id.containerQuickHistory)

        setupCardDeck(view)

        refreshUserData()
        renderQuickHistory()

        val homeScrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.homeScrollView)
        homeScrollView?.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val swipeRefresh = activity?.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.dashboardSwipeRefresh)
            swipeRefresh?.isEnabled = (scrollY <= 0)
        })

        // First-Time User In-App Guidance Tour (Guarded against duplicate launches)
        if (!hasTriggeredGuidanceInSession) {
            hasTriggeredGuidanceInSession = true
            view.postDelayed({
                if (isAdded && activity != null && !requireActivity().isFinishing) {
                    InAppGuidanceManager.checkAndStartGuidance(requireActivity())
                }
            }, 600)
        }

        // Avatar Tap -> Account & Card
        view.findViewById<View>(R.id.homeAvatar)?.bounceClick {
            startActivity(Intent(requireContext(), AccountCardActivity::class.java))
        }

        // Notification Bell -> NotificationActivity
        view.findViewById<View>(R.id.btnHomeNotification)?.bounceClick {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        // Search Button -> ExchangeRateActivity (Branch & Rates)
        view.findViewById<View>(R.id.btnHomeSearch)?.bounceClick {
            startActivity(Intent(requireContext(), ExchangeRateActivity::class.java))
        }

        // Card Deck Container Tap -> Account & Cards
        view.findViewById<View>(R.id.homeDeckContainer)?.setOnClickListener {
            cardDeckHelper?.cycleCardForward()
        }

        // Quick Categorize Menu with useAnimations micro-interactions:
        // 1. Sent -> TransferActivity
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnQuickSent),
            iconContainer = view.findViewById(R.id.iconContainerQuickSent),
            staticIcon = view.findViewById(R.id.staticIconQuickSent),
            assetName = "anim_sent.json",
            iconTintRes = R.color.figma_primary
        ) {
            startActivity(Intent(requireContext(), TransferActivity::class.java))
        }

        // 2. Receive -> Production Receive Screen (QRIS & Account)
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnQuickReceive),
            iconContainer = view.findViewById(R.id.iconContainerQuickReceive),
            staticIcon = view.findViewById(R.id.staticIconQuickReceive),
            assetName = "anim_receive.json",
            iconTintRes = R.color.bank_success
        ) {
            startActivity(Intent(requireContext(), ReceiveActivity::class.java))
        }

        // 3. Loan -> BillPaymentActivity
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnQuickLoan),
            iconContainer = view.findViewById(R.id.iconContainerQuickLoan),
            staticIcon = view.findViewById(R.id.staticIconQuickLoan),
            assetName = "anim_loan.json",
            iconTintRes = R.color.figma_warning
        ) {
            startActivity(Intent(requireContext(), BillPaymentActivity::class.java))
        }

        // 4. Topup -> TopUpMenuActivity
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnQuickTopup),
            iconContainer = view.findViewById(R.id.iconContainerQuickTopup),
            staticIcon = view.findViewById(R.id.staticIconQuickTopup),
            assetName = "anim_topup.json",
            iconTintRes = R.color.bank_purple
        ) {
            startActivity(Intent(requireContext(), TopUpMenuActivity::class.java))
        }

        // 9 Action Tiles with useAnimations micro-interactions:
        // 1. Account and Card
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnTileAccountCard),
            iconContainer = view.findViewById(R.id.iconContainerTileAccountCard),
            staticIcon = view.findViewById(R.id.staticIconTileAccountCard),
            assetName = "anim_account_card.json",
            iconTintRes = R.color.figma_primary
        ) {
            startActivity(Intent(requireContext(), AccountCardActivity::class.java))
        }

        // 2. Transfer
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnTileTransfer),
            iconContainer = view.findViewById(R.id.iconContainerTileTransfer),
            staticIcon = view.findViewById(R.id.staticIconTileTransfer),
            assetName = "anim_transfer.json",
            iconTintRes = R.color.bank_red
        ) {
            startActivity(Intent(requireContext(), TransferActivity::class.java))
        }

        // 3. Withdraw
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnTileWithdraw),
            iconContainer = view.findViewById(R.id.iconContainerTileWithdraw),
            staticIcon = view.findViewById(R.id.staticIconTileWithdraw),
            assetName = "anim_withdraw.json",
            iconTintRes = R.color.bank_blue
        ) {
            startActivity(Intent(requireContext(), WithdrawActivity::class.java))
        }

        // 4. Top Up Hub (Mobile & E-Wallet)
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnTileMobilePrepaid),
            iconContainer = view.findViewById(R.id.iconContainerTileMobilePrepaid),
            staticIcon = view.findViewById(R.id.staticIconTileMobilePrepaid),
            assetName = "anim_phone.json",
            iconTintRes = R.color.bank_accent
        ) {
            startActivity(Intent(requireContext(), TopUpMenuActivity::class.java))
        }

        // 5. Pay the Bill
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnTilePayBill),
            iconContainer = view.findViewById(R.id.iconContainerTilePayBill),
            staticIcon = view.findViewById(R.id.staticIconTilePayBill),
            assetName = "anim_topup.json",
            iconTintRes = R.color.bank_success
        ) {
            startActivity(Intent(requireContext(), BillPaymentActivity::class.java))
        }

        // 6. Save Online
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnTileSaveOnline),
            iconContainer = view.findViewById(R.id.iconContainerTileSaveOnline),
            staticIcon = view.findViewById(R.id.staticIconTileSaveOnline),
            assetName = "anim_piggy.json",
            iconTintRes = R.color.bank_purple
        ) {
            startActivity(Intent(requireContext(), SaveOnlineActivity::class.java))
        }

        // 7. Credit Card
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnTileCreditCard),
            iconContainer = view.findViewById(R.id.iconContainerTileCreditCard),
            staticIcon = view.findViewById(R.id.staticIconTileCreditCard),
            assetName = "anim_account_card.json",
            iconTintRes = R.color.bank_accent
        ) {
            startActivity(Intent(requireContext(), CreditCardActivity::class.java))
        }

        // 8. Transaction Report
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnTileTransactionReport),
            iconContainer = view.findViewById(R.id.iconContainerTileTransactionReport),
            staticIcon = view.findViewById(R.id.staticIconTileTransactionReport),
            assetName = "anim_transfer.json",
            iconTintRes = R.color.bank_success
        ) {
            startActivity(Intent(requireContext(), TransactionReportActivity::class.java))
        }

        // 9. Beneficiary
        HomeMenuAnimationHelper.setupAnimatedMenu(
            buttonView = view.findViewById(R.id.btnTileBeneficiary),
            iconContainer = view.findViewById(R.id.iconContainerTileBeneficiary),
            staticIcon = view.findViewById(R.id.staticIconTileBeneficiary),
            assetName = "anim_beneficiary.json",
            iconTintRes = R.color.bank_purple
        ) {
            startActivity(Intent(requireContext(), BeneficiaryActivity::class.java))
        }

        // Currency Exchange (Exchange #1 & #2)
        view.findViewById<View>(R.id.btnHomeCurrencyExchange)?.bounceClick {
            startActivity(Intent(requireContext(), ExchangeActivity::class.java))
        }

        // Transaction "See All" -> the full History screen from Source B.
        view.findViewById<View>(R.id.btnQuickHistorySeeAll)?.bounceClick {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HistoryFragment())
                .addToBackStack("history")
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserData()
        renderQuickHistory()
        cardDeckHelper?.renderDeck()
    }

    override fun onDestroyView() {
        cardDeckHelper = null
        super.onDestroyView()
    }

    private fun refreshUserData() {
        val storage = AppStorage.get()
        val user = storage.userName.ifEmpty { "Nasabah" }
        greetingView.text = user
        view?.findViewById<ImageView>(R.id.homeAvatar)?.let { avatar ->
            com.example.bankapputomo.util.AvatarHelper.loadAvatar(requireContext(), avatar)
        }
    }

    private fun setupCardDeck(view: View) {
        val deckContainer = view.findViewById<ViewGroup>(R.id.homeDeckContainer) ?: return
        cardDeckHelper = CardDeckHelper(requireActivity(), deckContainer)

        view.findViewById<View>(R.id.btnDeckAddCard)?.bounceClick {
            cardDeckHelper?.showAddCardBottomSheet()
        }

        view.findViewById<View>(R.id.btnDeckToggleBlock)?.bounceClick {
            cardDeckHelper?.blockCurrentCard()
        }

        view.findViewById<View>(R.id.btnDeckDeleteCard)?.bounceClick {
            cardDeckHelper?.deleteCurrentCard()
        }

        cardDeckHelper?.setup(onlyActiveCards = true)

        // Real-time listener for cards: if staff blocks or freezes a card, update Home deck immediately
        BackOfficeRepository.listenToUserCards(requireContext()) {
            if (isAdded) {
                cardDeckHelper?.renderDeck(animateFromBack = true, onlyActiveCards = true)
            }
        }
    }

    private fun renderQuickHistory() {
        containerQuickHistory.removeAllViews()
        val transactions = AppStorage.get().getTransactions().take(3)

        transactions.forEach { tx ->
            val row = layoutInflater.inflate(R.layout.item_quick_history_row, containerQuickHistory, false)
            val iconView = row.findViewById<ImageView>(R.id.quickHistoryIcon)
            val titleView = row.findViewById<TextView>(R.id.quickHistoryTitle)
            val subtitleView = row.findViewById<TextView>(R.id.quickHistorySubtitle)
            val amountView = row.findViewById<TextView>(R.id.quickHistoryAmount)

            titleView.text = tx.title
            subtitleView.text = "${tx.category} • ${tx.date}"

            val ewalletMatch = com.example.bankapputomo.util.EWalletSelectionBottomSheet.ALL_EWALLETS.firstOrNull {
                tx.title.contains(it.name, ignoreCase = true) || tx.counterparty.contains(it.name, ignoreCase = true)
            }

            if (ewalletMatch != null) {
                amountView.text = "- ${IdrFormatter.format(tx.amount)}"
                amountView.setTextColor(requireContext().getColor(R.color.figma_text_title))
                iconView.setImageResource(ewalletMatch.logoRes)
                iconView.setBackgroundResource(R.drawable.bg_figma_card)
                iconView.imageTintList = null
                iconView.setPadding(0, 0, 0, 0)
            } else if (tx.isIncome) {
                amountView.text = "+ ${IdrFormatter.format(tx.amount)}"
                amountView.setTextColor(requireContext().getColor(R.color.bank_success))
                iconView.setImageResource(R.drawable.ic_figma_received)
                iconView.setBackgroundResource(R.drawable.bg_tile_soft_green)
                iconView.imageTintList = androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.bank_success)
            } else {
                amountView.text = "- ${IdrFormatter.format(tx.amount)}"
                amountView.setTextColor(requireContext().getColor(R.color.figma_text_title))
                iconView.setImageResource(R.drawable.ic_figma_sent)
                iconView.setBackgroundResource(R.drawable.bg_tile_soft_blue)
                iconView.imageTintList = androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.figma_primary)
            }

            row.bounceClick {
                showTransactionDetailDialog(tx)
            }
            containerQuickHistory.addView(row)
        }

        if (transactions.isEmpty()) {
            val emptyView = layoutInflater.inflate(R.layout.view_lottie_empty_state, containerQuickHistory, false)
            val anim = emptyView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieEmptyAnimation)
            if (anim != null) {
                anim.setAnimation("anim_empty_state.json")
                com.example.bankapputomo.util.LottieMotionHelper.recolor(
                    anim,
                    requireContext().getColor(R.color.figma_primary)
                )
                com.example.bankapputomo.util.LottieMotionHelper.playOnce(anim)
            }
            containerQuickHistory.addView(emptyView)
        }
    }

    private fun showTransactionDetailDialog(tx: com.example.bankapputomo.util.TransactionModel) {
        ReceiptActivity.startFromTransaction(requireContext(), tx)
    }
}
