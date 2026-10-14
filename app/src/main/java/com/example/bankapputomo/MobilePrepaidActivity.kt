package com.example.bankapputomo

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BiometricHelper
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class MobilePrepaidActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var rechargeHeaderTitle: TextView

    private lateinit var viewInputStep: View
    private lateinit var viewConfirmStep: View
    private lateinit var viewSuccessStep: View

    private lateinit var inputPhoneNumber: EditText
    private lateinit var btnPickContact: ImageView
    private lateinit var recentContact1: TextView
    private lateinit var recentContact2: TextView

    private lateinit var tabAirtime: TextView
    private lateinit var tabData: TextView

    private lateinit var denom25: LinearLayout
    private lateinit var denom50: LinearLayout
    private lateinit var denom100: LinearLayout
    private lateinit var denom200: LinearLayout
    private lateinit var denom300: LinearLayout
    private lateinit var denom500: LinearLayout

    private lateinit var btnProceedRecharge: TextView

    private lateinit var textConfirmPhone: TextView
    private lateinit var textConfirmDenom: TextView
    private lateinit var textConfirmTotal: TextView
    private lateinit var inputRechargePin: EditText
    private lateinit var btnConfirmRecharge: TextView
    private lateinit var btnUseFingerprint: View

    private lateinit var textReceiptTotal: TextView
    private lateinit var textReceiptPhone: TextView
    private lateinit var btnDoneRecharge: TextView

    private var selectedDenom: Long = 100000
    private var adminFee: Long = 1500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mobile_prepaid)
        registerBackHandler(::handleBack)

        btnBack = findViewById(R.id.btnBack)
        rechargeHeaderTitle = findViewById(R.id.rechargeHeaderTitle)

        viewInputStep = findViewById(R.id.viewInputStep)
        viewConfirmStep = findViewById(R.id.viewConfirmStep)
        viewSuccessStep = findViewById(R.id.viewSuccessStep)

        inputPhoneNumber = findViewById(R.id.inputPhoneNumber)
        btnPickContact = findViewById(R.id.btnPickContact)
        recentContact1 = findViewById(R.id.recentContact1)
        recentContact2 = findViewById(R.id.recentContact2)

        tabAirtime = findViewById(R.id.tabAirtime)
        tabData = findViewById(R.id.tabData)

        denom25 = findViewById(R.id.denom25)
        denom50 = findViewById(R.id.denom50)
        denom100 = findViewById(R.id.denom100)
        denom200 = findViewById(R.id.denom200)
        denom300 = findViewById(R.id.denom300)
        denom500 = findViewById(R.id.denom500)

        btnProceedRecharge = findViewById(R.id.btnProceedRecharge)

        textConfirmPhone = findViewById(R.id.textConfirmPhone)
        textConfirmDenom = findViewById(R.id.textConfirmDenom)
        textConfirmTotal = findViewById(R.id.textConfirmTotal)
        inputRechargePin = findViewById(R.id.inputRechargePin)
        btnConfirmRecharge = findViewById(R.id.btnConfirmRecharge)
        btnUseFingerprint = findViewById(R.id.btnUseFingerprint)

        textReceiptTotal = findViewById(R.id.textReceiptTotal)
        textReceiptPhone = findViewById(R.id.textReceiptPhone)
        btnDoneRecharge = findViewById(R.id.btnDoneRecharge)

        btnBack.bounceClick { handleBack() }

        recentContact1.bounceClick { inputPhoneNumber.setText("08123456789") }
        recentContact2.bounceClick { inputPhoneNumber.setText("08567891234") }

        btnPickContact.bounceClick {
            inputPhoneNumber.setText("081298765432")
            Toast.makeText(this, "Selected contact: 081298765432", Toast.LENGTH_SHORT).show()
        }

        tabAirtime.bounceClick {
            tabAirtime.setBackgroundResource(R.drawable.bg_figma_tab_active)
            tabAirtime.setTextColor(getColor(android.R.color.white))
            tabData.setBackgroundResource(R.drawable.bg_figma_tab_inactive)
            tabData.setTextColor(getColor(R.color.figma_text_muted))
        }

        tabData.bounceClick {
            tabData.setBackgroundResource(R.drawable.bg_figma_tab_active)
            tabData.setTextColor(getColor(android.R.color.white))
            tabAirtime.setBackgroundResource(R.drawable.bg_figma_tab_inactive)
            tabAirtime.setTextColor(getColor(R.color.figma_text_muted))
        }

        val allDenoms = listOf(
            denom25 to 25000L,
            denom50 to 50000L,
            denom100 to 100000L,
            denom200 to 200000L,
            denom300 to 300000L,
            denom500 to 500000L
        )

        allDenoms.forEach { (view, amount) ->
            view.bounceClick {
                selectDenom(view, amount, allDenoms)
            }
        }

        selectDenom(denom100, 100000L, allDenoms)

        btnProceedRecharge.bounceClick {
            val phone = inputPhoneNumber.text.toString().trim()
            if (phone.length < 10) {
                Toast.makeText(this, getString(R.string.mobile_error_phone), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val total = selectedDenom + adminFee
            if (total > AppStorage.get().debitBalance) {
                Toast.makeText(this, getString(R.string.mobile_error_insufficient_balance, IdrFormatter.format(AppStorage.get().debitBalance)), Toast.LENGTH_LONG).show()
                return@bounceClick
            }

            textConfirmPhone.text = phone
            textConfirmDenom.text = IdrFormatter.format(selectedDenom)
            textConfirmTotal.text = IdrFormatter.format(total)

            viewInputStep.visibility = View.GONE
            viewConfirmStep.visibility = View.VISIBLE
            viewSuccessStep.visibility = View.GONE
            rechargeHeaderTitle.text = getString(R.string.mobile_payment_confirmation)
        }

        btnConfirmRecharge.bounceClick {
            val pin = inputRechargePin.text.toString().trim()
            if (pin.length < 6) {
                Toast.makeText(this, getString(R.string.mobile_error_pin), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            executeRecharge()
        }

        btnUseFingerprint.bounceClick {
            val total = selectedDenom + adminFee
            val phone = inputPhoneNumber.text.toString().trim()
            BiometricHelper.showBiometricPrompt(
                activity = this,
                title = getString(R.string.mobile_authorize_recharge),
                subtitle = getString(R.string.mobile_authorize_recharge_subtitle, IdrFormatter.format(total), phone),
                onSuccess = {
                    executeRecharge()
                },
                onError = { err ->
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                }
            )
        }

        btnDoneRecharge.bounceClick {
            finish()
        }
    }

    private fun selectDenom(selected: LinearLayout, amount: Long, all: List<Pair<LinearLayout, Long>>) {
        selectedDenom = amount
        all.forEach { (view, _) ->
            if (view == selected) {
                view.setBackgroundResource(R.drawable.bg_figma_button_primary)
                (view.getChildAt(0) as? TextView)?.setTextColor(resources.getColor(android.R.color.white, theme))
                (view.getChildAt(1) as? TextView)?.setTextColor(resources.getColor(android.R.color.white, theme))
            } else {
                view.setBackgroundResource(R.drawable.bg_figma_card)
                (view.getChildAt(0) as? TextView)?.setTextColor(resources.getColor(R.color.figma_text_title, theme))
                (view.getChildAt(1) as? TextView)?.setTextColor(resources.getColor(R.color.figma_text_muted, theme))
            }
        }
    }

    private fun executeRecharge() {
        if (!com.example.bankapputomo.util.CardSecurityHelper.isPrimaryCardActive(this)) {
            com.example.bankapputomo.util.CardSecurityHelper.showCardLockedBottomSheet(this)
            return
        }

        val total = selectedDenom + adminFee
        val phone = inputPhoneNumber.text.toString().trim()

        val success = AppStorage.get().addTransaction(
            title = getString(R.string.mobile_recharge_title, phone),
            category = "Mobile Prepaid",
            amount = total,
            isIncome = false,
            counterparty = phone
        )

        if (success) {
            ReceiptActivity.start(
                context = this,
                title = getString(R.string.mobile_topup),
                category = "Pulsa & Data",
                amount = selectedDenom,
                fee = adminFee,
                counterparty = "Telkomsel • $phone",
                recipientAccount = phone,
                bankName = "UTOMO Bank Telco",
                note = "Pulsa Regular $phone",
                origin = "PULSA"
            )
            finish()
        } else {
            Toast.makeText(this, getString(R.string.mobile_error_transaction), Toast.LENGTH_LONG).show()
        }
    }

    private fun showSuccess() {
        viewInputStep.visibility = View.GONE
        viewConfirmStep.visibility = View.GONE
        viewSuccessStep.visibility = View.VISIBLE
        rechargeHeaderTitle.text = getString(R.string.mobile_topup_success)

        textReceiptPhone.text = inputPhoneNumber.text.toString().trim()
        textReceiptTotal.text = IdrFormatter.format(selectedDenom + adminFee)
    }

    private fun handleBack() {
        if (viewSuccessStep.visibility == View.VISIBLE) {
            finish()
        } else if (viewConfirmStep.visibility == View.VISIBLE) {
            viewConfirmStep.visibility = View.GONE
            viewInputStep.visibility = View.VISIBLE
        rechargeHeaderTitle.text = getString(R.string.mobile_topup)
        } else {
            finish()
        }
    }

}
