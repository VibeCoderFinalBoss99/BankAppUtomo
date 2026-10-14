package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BankSelectionBottomSheet
import com.example.bankapputomo.util.BiometricHelper
import com.example.bankapputomo.util.EWalletSelectionBottomSheet
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog

class TransferActivity : BaseActivity() {

    private lateinit var viewForm: View
    private lateinit var viewSuccess: View
    private lateinit var inputRecipient: EditText
    private lateinit var inputAccount: EditText
    private lateinit var inputAmount: EditText
    private lateinit var inputContent: EditText
    private lateinit var checkSaveBeneficiary: CheckBox

    // Destination Pickers
    private lateinit var btnSelectBankDestination: View
    private lateinit var textSelectedBankDestination: TextView
    private lateinit var btnSelectEWalletDestination: View
    private lateinit var textSelectedEWalletDestination: TextView
    private lateinit var imageSelectedEWalletLogo: ImageView

    private var selectedBank = ""
    private var selectedEWallet = ""
    private var selectedAmount = 0L
    private var selectedTransactionType = "SameBank"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)

        findViewById<View>(R.id.btnBack).bounceClick { finish() }

        viewForm = findViewById(R.id.viewTransferForm)
        viewSuccess = findViewById(R.id.viewTransferSuccess)
        inputRecipient = findViewById(R.id.inputRecipientName)
        inputAccount = findViewById(R.id.inputAccountNumber)
        inputAmount = findViewById(R.id.inputTransferAmount)
        inputContent = findViewById(R.id.inputTransferContent)
        checkSaveBeneficiary = findViewById(R.id.checkSaveBeneficiary)

        btnSelectBankDestination = findViewById(R.id.btnSelectBankDestination)
        textSelectedBankDestination = findViewById(R.id.textSelectedBankDestination)
        btnSelectEWalletDestination = findViewById(R.id.btnSelectEWalletDestination)
        textSelectedEWalletDestination = findViewById(R.id.textSelectedEWalletDestination)
        imageSelectedEWalletLogo = findViewById(R.id.imageSelectedEWalletLogo)

        // Dropdown actions
        btnSelectBankDestination.bounceClick {
            BankSelectionBottomSheet.show(this, selectedBank) { chosen ->
                selectedBank = chosen
                textSelectedBankDestination.text = chosen
                textSelectedBankDestination.setTextColor(ContextCompat.getColor(this, R.color.figma_text_title))
            }
        }

        btnSelectEWalletDestination.bounceClick {
            EWalletSelectionBottomSheet.show(this, selectedEWallet) { chosen ->
                updateEWalletSelection(chosen)
            }
        }

        // Transaction Types selection (E-Wallet, Same Bank, Other Bank)
        val typeEWallet = findViewById<LinearLayout>(R.id.typeEWallet)
        val typeSame = findViewById<LinearLayout>(R.id.typeSameBank)
        val typeOther = findViewById<LinearLayout>(R.id.typeOtherBank)

        fun updateTypeSelection(type: String) {
            selectedTransactionType = type
            val typeItems = listOf(
                typeEWallet to "EWallet",
                typeSame to "SameBank",
                typeOther to "OtherBank"
            )
            typeItems.forEach { (layout, t) ->
                val isSelected = (t == type)
                layout.setBackgroundResource(if (isSelected) R.drawable.bg_figma_tab_active else R.drawable.bg_figma_card)
                val icon = layout.getChildAt(0) as? ImageView
                val text = layout.getChildAt(1) as? TextView
                icon?.setColorFilter(getColor(if (isSelected) android.R.color.white else R.color.figma_primary))
                text?.setTextColor(getColor(if (isSelected) android.R.color.white else R.color.figma_text_title))
            }

            when (type) {
                "EWallet" -> {
                    btnSelectBankDestination.visibility = View.GONE
                    btnSelectEWalletDestination.visibility = View.VISIBLE
                    inputAccount.hint = "Nomor HP / ID E-Wallet"
                }
                "SameBank" -> {
                    btnSelectBankDestination.visibility = View.GONE
                    btnSelectEWalletDestination.visibility = View.GONE
                    inputAccount.hint = "Nomor Rekening Utomo Bank"
                }
                "OtherBank" -> {
                    btnSelectBankDestination.visibility = View.VISIBLE
                    btnSelectEWalletDestination.visibility = View.GONE
                    inputAccount.hint = "Nomor Rekening Tujuan"
                }
            }
        }

        typeEWallet.bounceClick { updateTypeSelection("EWallet") }
        typeSame.bounceClick { updateTypeSelection("SameBank") }
        typeOther.bounceClick { updateTypeSelection("OtherBank") }

        // Read prefill from Intent ONLY if provided, otherwise leave fields empty
        val prefillName = intent.getStringExtra("prefill_name")
        val prefillAccount = intent.getStringExtra("prefill_account")
        val prefillBank = intent.getStringExtra("prefill_bank")
        if (!prefillName.isNullOrEmpty()) {
            inputRecipient.setText(prefillName)
            inputAccount.setText(prefillAccount ?: "")
            if (!prefillBank.isNullOrEmpty() && !prefillBank.equals("Utomo Bank", ignoreCase = true)) {
                val isEWallet = EWalletSelectionBottomSheet.ALL_EWALLETS.any { it.name.equals(prefillBank, ignoreCase = true) }
                if (isEWallet) {
                    updateEWalletSelection(prefillBank)
                    updateTypeSelection("EWallet")
                } else {
                    selectedBank = prefillBank
                    textSelectedBankDestination.text = prefillBank
                    textSelectedBankDestination.setTextColor(ContextCompat.getColor(this, R.color.figma_text_title))
                    updateTypeSelection("OtherBank")
                }
            } else {
                updateTypeSelection("SameBank")
            }
        } else {
            // Fields are left empty initially
            inputRecipient.setText("")
            inputAccount.setText("")
            updateTypeSelection("SameBank")
        }

        // IDR text watcher - leave amount empty initially
        IdrFormatter.applyAmountInputWatcher(inputAmount) { rawVal ->
            selectedAmount = rawVal
        }

        // Quick Beneficiary picks
        findViewById<View>(R.id.beneficiaryEmma).bounceClick {
            inputRecipient.setText("Emma Watson")
            inputAccount.setText("0987654321")
        }
        findViewById<View>(R.id.beneficiaryJustin).bounceClick {
            inputRecipient.setText("Justin Bieber")
            inputAccount.setText("1122334455")
        }
        findViewById<View>(R.id.beneficiaryOlivia).bounceClick {
            inputRecipient.setText("Olivia Rodrigo")
            inputAccount.setText("0345976331")
        }
        findViewById<View>(R.id.beneficiaryAlexander).bounceClick {
            inputRecipient.setText("Alexander")
            inputAccount.setText("5544332211")
        }

        // Find beneficiary link -> Open Beneficiary Activity
        findViewById<View>(R.id.btnFindBeneficiary).bounceClick {
            startActivity(Intent(this, BeneficiaryActivity::class.java))
        }

        findViewById<View>(R.id.btnAddBeneficiaryQuick).bounceClick {
            startActivity(Intent(this, BeneficiaryActivity::class.java))
        }


        // Confirm button -> Validate inputs and Open Confirmation Bottom Sheet
        findViewById<View>(R.id.btnConfirmTransfer).bounceClick {
            val recipientName = inputRecipient.text.toString().trim()
            val accountNumber = inputAccount.text.toString().trim()

            if (recipientName.isEmpty()) {
                Toast.makeText(this, "Silakan masukkan nama penerima", Toast.LENGTH_SHORT).show()
                inputRecipient.requestFocus()
                return@bounceClick
            }

            if (selectedTransactionType == "OtherBank" && selectedBank.isEmpty()) {
                Toast.makeText(this, getString(R.string.select_bank_prompt), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            if (selectedTransactionType == "EWallet" && selectedEWallet.isEmpty()) {
                Toast.makeText(this, getString(R.string.ewallet_select_provider), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            if (accountNumber.isEmpty()) {
                val msg = if (selectedTransactionType == "EWallet") "Silakan masukkan nomor HP / ID e-wallet" else "Silakan masukkan nomor rekening"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                inputAccount.requestFocus()
                return@bounceClick
            }

            if (selectedAmount <= 0L) {
                Toast.makeText(this, "Silakan masukkan nominal transfer", Toast.LENGTH_SHORT).show()
                inputAmount.requestFocus()
                return@bounceClick
            }

            if (selectedAmount > AppStorage.get().debitBalance) {
                Toast.makeText(this, "Saldo tidak mencukupi (${IdrFormatter.format(AppStorage.get().debitBalance)})", Toast.LENGTH_LONG).show()
                return@bounceClick
            }

            showConfirmBottomSheet()
        }

        // Success Done button
        findViewById<View>(R.id.btnDoneTransfer).bounceClick {
            finish()
        }
    }

    private fun showConfirmBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_transfer_confirm, null)
        bottomSheet.setContentView(sheetView)

        val recipientName = inputRecipient.text.toString().trim()
        val accountNumber = inputAccount.text.toString().trim()

        val destBankName = when (selectedTransactionType) {
            "OtherBank" -> selectedBank.ifEmpty { "Bank Lain" }
            "EWallet" -> selectedEWallet.ifEmpty { "E-Wallet" }
            else -> "Utomo Bank"
        }

        sheetView.findViewById<TextView>(R.id.textConfirmTo).text = "$recipientName ($destBankName)"
        val imageConfirmDestLogo = sheetView.findViewById<ImageView>(R.id.imageConfirmDestLogo)
        val ewalletItem = if (selectedTransactionType == "EWallet") {
            EWalletSelectionBottomSheet.ALL_EWALLETS.firstOrNull { it.name.equals(selectedEWallet, ignoreCase = true) }
        } else null
        if (ewalletItem != null) {
            imageConfirmDestLogo?.setImageResource(ewalletItem.logoRes)
            imageConfirmDestLogo?.visibility = View.VISIBLE
        } else {
            imageConfirmDestLogo?.visibility = View.GONE
        }

        sheetView.findViewById<TextView>(R.id.textConfirmAccount).text = accountNumber
        sheetView.findViewById<TextView>(R.id.textConfirmSheetAmount).text = IdrFormatter.format(selectedAmount)

        val inputOtp = sheetView.findViewById<EditText>(R.id.inputSheetOtp)
        sheetView.findViewById<View>(R.id.btnGetOtp).bounceClick {
            inputOtp.setText("6789")
            Toast.makeText(this, "OTP sent: 6789", Toast.LENGTH_SHORT).show()
        }

        // Fingerprint biometric authentication
        sheetView.findViewById<View>(R.id.btnVerifyFingerprint).bounceClick {
            BiometricHelper.showBiometricPrompt(
                activity = this,
                title = "Authorize Transfer",
                subtitle = "Confirm transfer of ${IdrFormatter.format(selectedAmount)} to $recipientName",
                onSuccess = {
                    bottomSheet.dismiss()
                    executeTransfer(recipientName, accountNumber, destBankName)
                },
                onError = { err ->
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Final Confirm button
        sheetView.findViewById<View>(R.id.btnFinalConfirmTransfer).bounceClick {
            bottomSheet.dismiss()
            executeTransfer(recipientName, accountNumber, destBankName)
        }

        bottomSheet.show()
    }

    private fun updateEWalletSelection(chosen: String) {
        selectedEWallet = chosen
        if (chosen.isNotEmpty()) {
            textSelectedEWalletDestination.text = chosen
            textSelectedEWalletDestination.setTextColor(ContextCompat.getColor(this, R.color.figma_text_title))
            val ewalletItem = EWalletSelectionBottomSheet.ALL_EWALLETS.firstOrNull { it.name.equals(chosen, ignoreCase = true) }
            if (ewalletItem != null) {
                imageSelectedEWalletLogo.setImageResource(ewalletItem.logoRes)
                imageSelectedEWalletLogo.visibility = View.VISIBLE
            } else {
                imageSelectedEWalletLogo.visibility = View.GONE
            }
        } else {
            textSelectedEWalletDestination.text = getString(R.string.ewallet_select_provider)
            textSelectedEWalletDestination.setTextColor(ContextCompat.getColor(this, R.color.figma_text_muted))
            imageSelectedEWalletLogo.visibility = View.GONE
        }
    }

    private fun executeTransfer(recipientName: String, accountNumber: String, destBankName: String) {
        if (!com.example.bankapputomo.util.CardSecurityHelper.isPrimaryCardActive(this)) {
            com.example.bankapputomo.util.CardSecurityHelper.showCardLockedBottomSheet(this)
            return
        }

        val txTitle = if (selectedTransactionType == "EWallet") {
            "Top Up $destBankName - $recipientName"
        } else {
            "Transfer ke $recipientName ($destBankName)"
        }

        val success = AppStorage.get().addTransaction(
            title = txTitle,
            category = if (selectedTransactionType == "EWallet") "Top Up" else "Transfer",
            amount = selectedAmount,
            isIncome = false,
            counterparty = recipientName
        )

        if (success) {
            if (checkSaveBeneficiary.isChecked) {
                AppStorage.get().addBeneficiary(recipientName, accountNumber, destBankName)
            }
            ReceiptActivity.start(
                context = this,
                title = if (selectedTransactionType == "EWallet") "Transfer E-Wallet ke $recipientName" else "Transfer Dana ke $recipientName",
                category = if (selectedTransactionType == "EWallet") "Top Up" else "Transfer",
                amount = selectedAmount,
                fee = 0L,
                counterparty = recipientName,
                recipientAccount = accountNumber,
                bankName = destBankName,
                note = inputContent.text.toString().ifEmpty { "Transfer Dana" },
                origin = "TRANSFER"
            )
            finish()
        } else {
            com.example.bankapputomo.util.TransactionFeedbackDialog.show(
                context = this,
                isSuccess = false,
                title = "Transfer Gagal",
                message = "Saldo Anda tidak mencukupi untuk melakukan transfer sejumlah ${IdrFormatter.format(selectedAmount)}.",
                actionButtonText = "Coba Lagi"
            )
        }
    }

    private fun showSuccessScreen(recipientName: String) {
        viewForm.visibility = View.GONE
        viewSuccess.visibility = View.VISIBLE
        findViewById<TextView>(R.id.textSuccessRecipient).text = recipientName
        findViewById<TextView>(R.id.textSuccessTransferAmount).text = IdrFormatter.format(selectedAmount)

        val animSuccess = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieTransferSuccessAnim)
        if (animSuccess != null) {
            animSuccess.setAnimation("anim_success.json")
            com.example.bankapputomo.util.LottieMotionHelper.recolor(
                animSuccess,
                ContextCompat.getColor(this, R.color.figma_secondary)
            )
            com.example.bankapputomo.util.LottieMotionHelper.playOnce(animSuccess)
        }
    }
}
