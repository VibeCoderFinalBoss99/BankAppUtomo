package com.example.bankapputomo

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class WithdrawActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var withdrawHeaderTitle: TextView

    private lateinit var stepAmountView: View
    private lateinit var stepPinView: View
    private lateinit var stepCodeView: View

    private lateinit var chipAmount100: TextView
    private lateinit var chipAmount200: TextView
    private lateinit var chipAmount500: TextView
    private lateinit var chipAmount1000: TextView
    private lateinit var chipAmount1500: TextView
    private lateinit var chipAmount2000: TextView
    private lateinit var inputCustomAmount: EditText

    private lateinit var btnSelectAtm: View
    private lateinit var textSelectedAtm: TextView
    private lateinit var btnProceedWithdraw: TextView

    private lateinit var inputWithdrawPin: EditText
    private lateinit var btnConfirmWithdrawPin: TextView

    private lateinit var textWithdrawnAmountSummary: TextView
    private lateinit var textWithdrawalCode: TextView
    private lateinit var btnDoneWithdraw: TextView

    private var selectedAmount: Long = 500000

    private val branchPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val name = result.data?.getStringExtra(BranchPickerActivity.EXTRA_BRANCH_NAME)
            val address = result.data?.getStringExtra(BranchPickerActivity.EXTRA_BRANCH_ADDRESS)
            if (!name.isNullOrEmpty()) {
                textSelectedAtm.text = if (!address.isNullOrEmpty()) "$name - $address" else name
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_withdraw)
        registerBackHandler(::handleBack)

        btnBack = findViewById(R.id.btnBack)
        withdrawHeaderTitle = findViewById(R.id.withdrawHeaderTitle)

        stepAmountView = findViewById(R.id.stepAmountView)
        stepPinView = findViewById(R.id.stepPinView)
        stepCodeView = findViewById(R.id.stepCodeView)

        chipAmount100 = findViewById(R.id.chipAmount100)
        chipAmount200 = findViewById(R.id.chipAmount200)
        chipAmount500 = findViewById(R.id.chipAmount500)
        chipAmount1000 = findViewById(R.id.chipAmount1000)
        chipAmount1500 = findViewById(R.id.chipAmount1500)
        chipAmount2000 = findViewById(R.id.chipAmount2000)
        inputCustomAmount = findViewById(R.id.inputCustomAmount)

        btnSelectAtm = findViewById(R.id.btnSelectAtm)
        textSelectedAtm = findViewById(R.id.textSelectedAtm)
        btnProceedWithdraw = findViewById(R.id.btnProceedWithdraw)

        inputWithdrawPin = findViewById(R.id.inputWithdrawPin)
        btnConfirmWithdrawPin = findViewById(R.id.btnConfirmWithdrawPin)

        textWithdrawnAmountSummary = findViewById(R.id.textWithdrawnAmountSummary)
        textWithdrawalCode = findViewById(R.id.textWithdrawalCode)
        btnDoneWithdraw = findViewById(R.id.btnDoneWithdraw)

        btnBack.bounceClick {
            handleBack()
        }

        // Amount chips
        val chips = listOf(
            chipAmount100 to 100000L,
            chipAmount200 to 200000L,
            chipAmount500 to 500000L,
            chipAmount1000 to 1000000L,
            chipAmount1500 to 1500000L,
            chipAmount2000 to 2000000L
        )

        selectChip(chipAmount500, 500000L, chips)

        chips.forEach { (chip, amount) ->
            chip.bounceClick {
                selectChip(chip, amount, chips)
                inputCustomAmount.setText("")
            }
        }

        inputCustomAmount.addTextChangedListener(IdrFormatter.createWatcher(inputCustomAmount) { raw ->
            if (raw > 0) {
                selectedAmount = raw
                chips.forEach { (c, _) ->
                    c.setBackgroundResource(R.drawable.bg_figma_card)
                    c.setTextColor(resources.getColor(R.color.figma_text_title, theme))
                }
            }
        })

        // ATM / Branch Location selector (Interactive Real Map from Figma)
        val initialBranch = AppStorage.get().getBranches().firstOrNull()
        if (initialBranch != null) {
            textSelectedAtm.text = "${initialBranch.name} - ${initialBranch.address}"
        }

        btnSelectAtm.bounceClick {
            val intent = android.content.Intent(this, BranchPickerActivity::class.java)
            branchPickerLauncher.launch(intent)
        }

        btnProceedWithdraw.bounceClick {
            if (selectedAmount <= 0) {
                Toast.makeText(this, "Please select or enter an amount", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            if (selectedAmount > AppStorage.get().debitBalance) {
                Toast.makeText(this, "Insufficient balance (${IdrFormatter.format(AppStorage.get().debitBalance)})", Toast.LENGTH_LONG).show()
                return@bounceClick
            }
            showStepPin()
        }

        btnConfirmWithdrawPin.bounceClick {
            if (!com.example.bankapputomo.util.CardSecurityHelper.isPrimaryCardActive(this)) {
                com.example.bankapputomo.util.CardSecurityHelper.showCardLockedBottomSheet(this)
                return@bounceClick
            }

            val pin = inputWithdrawPin.text.toString().trim()
            if (pin.length < 6) {
                Toast.makeText(this, "Please enter 6-digit PIN", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val success = AppStorage.get().addTransaction(
                title = "ATM Cash Withdrawal",
                category = "Withdrawal",
                amount = selectedAmount,
                isIncome = false,
                counterparty = textSelectedAtm.text.toString()
            )

            if (success) {
                showStepCode()
            } else {
                com.example.bankapputomo.util.TransactionFeedbackDialog.show(
                    context = this,
                    isSuccess = false,
                    title = "Withdrawal Failed",
                    message = "Insufficient balance in your account to withdraw ${com.example.bankapputomo.util.IdrFormatter.format(selectedAmount)}.",
                    actionButtonText = "Close"
                )
            }
        }

        btnDoneWithdraw.bounceClick {
            ReceiptActivity.start(
                context = this,
                title = "Tarik Tunai Tanpa Kartu",
                category = "Tarik Tunai",
                amount = selectedAmount,
                fee = 0L,
                counterparty = textSelectedAtm.text.toString(),
                recipientAccount = textWithdrawalCode.text.toString(),
                bankName = "ATM UTOMO Bank",
                note = "Kode Tarik Tunai: ${textWithdrawalCode.text}",
                origin = "WITHDRAW"
            )
            finish()
        }
    }

    private fun selectChip(selected: TextView, amount: Long, allChips: List<Pair<TextView, Long>>) {
        selectedAmount = amount
        allChips.forEach { (chip, _) ->
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.bg_figma_button_primary)
                chip.setTextColor(resources.getColor(android.R.color.white, theme))
            } else {
                chip.setBackgroundResource(R.drawable.bg_figma_card)
                chip.setTextColor(resources.getColor(R.color.figma_text_title, theme))
            }
        }
    }

    private fun showStepPin() {
        stepAmountView.visibility = View.GONE
        stepPinView.visibility = View.VISIBLE
        stepCodeView.visibility = View.GONE
        withdrawHeaderTitle.text = "Enter PIN"
    }

    private fun showStepCode() {
        stepAmountView.visibility = View.GONE
        stepPinView.visibility = View.GONE
        stepCodeView.visibility = View.VISIBLE
        withdrawHeaderTitle.text = "Withdrawal Code"

        textWithdrawnAmountSummary.text = IdrFormatter.format(selectedAmount)
        val randomCode = (100000..999999).random().toString()
        val formattedCode = "${randomCode.substring(0, 3)} ${randomCode.substring(3)}"
        textWithdrawalCode.text = formattedCode

        val anim = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieWithdrawSuccessAnim)
        if (anim != null) {
            anim.setAnimation("anim_success.json")
            com.example.bankapputomo.util.LottieMotionHelper.recolor(
                anim,
                androidx.core.content.ContextCompat.getColor(this, R.color.figma_secondary)
            )
            com.example.bankapputomo.util.LottieMotionHelper.playOnce(anim)
        }
    }

    private fun handleBack() {
        if (stepCodeView.visibility == View.VISIBLE) {
            finish()
        } else if (stepPinView.visibility == View.VISIBLE) {
            stepPinView.visibility = View.GONE
            stepAmountView.visibility = View.VISIBLE
            withdrawHeaderTitle.text = "Cash Withdrawal"
        } else {
            finish()
        }
    }

}
