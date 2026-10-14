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

class BillPaymentActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var billHeaderTitle: TextView

    private lateinit var viewCategoryHub: View
    private lateinit var viewBillForm: View
    private lateinit var viewBillConfirm: View
    private lateinit var viewBillReceipt: View

    private lateinit var cardCategoryElectricity: View
    private lateinit var cardCategoryInternet: View
    private lateinit var cardCategoryWater: View
    private lateinit var cardCategoryBpjs: View
    private lateinit var recentBill1: View
    private lateinit var recentBill2: View

    private lateinit var textFormProviderLabel: TextView
    private lateinit var btnSelectProvider: View
    private lateinit var textSelectedProvider: TextView
    private lateinit var textCustomerIdLabel: TextView
    private lateinit var inputCustomerId: EditText
    private lateinit var btnInquireBill: TextView

    private lateinit var textSummaryCustomerName: TextView
    private lateinit var textSummaryCustomerId: TextView
    private lateinit var textSummaryBillAmount: TextView
    private lateinit var textSummaryTotalPayment: TextView
    private lateinit var inputBillPin: EditText
    private lateinit var btnPayBillConfirm: TextView

    private lateinit var textReceiptBillTotal: TextView
    private lateinit var textReceiptService: TextView
    private lateinit var textReceiptCustomer: TextView
    private lateinit var btnDoneBill: TextView

    private var currentCategory = "Electricity"
    private var currentProvider = "PLN Postpaid (Tagihan Listrik)"
    private var billAmount: Long = 285000
    private var adminFee: Long = 2500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill_payment)
        registerBackHandler(::handleBack)

        btnBack = findViewById(R.id.btnBack)
        billHeaderTitle = findViewById(R.id.billHeaderTitle)

        viewCategoryHub = findViewById(R.id.viewCategoryHub)
        viewBillForm = findViewById(R.id.viewBillForm)
        viewBillConfirm = findViewById(R.id.viewBillConfirm)
        viewBillReceipt = findViewById(R.id.viewBillReceipt)

        cardCategoryElectricity = findViewById(R.id.cardCategoryElectricity)
        cardCategoryInternet = findViewById(R.id.cardCategoryInternet)
        cardCategoryWater = findViewById(R.id.cardCategoryWater)
        cardCategoryBpjs = findViewById(R.id.cardCategoryBpjs)
        recentBill1 = findViewById(R.id.recentBill1)
        recentBill2 = findViewById(R.id.recentBill2)

        textFormProviderLabel = findViewById(R.id.textFormProviderLabel)
        btnSelectProvider = findViewById(R.id.btnSelectProvider)
        textSelectedProvider = findViewById(R.id.textSelectedProvider)
        textCustomerIdLabel = findViewById(R.id.textCustomerIdLabel)
        inputCustomerId = findViewById(R.id.inputCustomerId)
        btnInquireBill = findViewById(R.id.btnInquireBill)

        textSummaryCustomerName = findViewById(R.id.textSummaryCustomerName)
        textSummaryCustomerId = findViewById(R.id.textSummaryCustomerId)
        textSummaryBillAmount = findViewById(R.id.textSummaryBillAmount)
        textSummaryTotalPayment = findViewById(R.id.textSummaryTotalPayment)
        inputBillPin = findViewById(R.id.inputBillPin)
        btnPayBillConfirm = findViewById(R.id.btnPayBillConfirm)

        textReceiptBillTotal = findViewById(R.id.textReceiptBillTotal)
        textReceiptService = findViewById(R.id.textReceiptService)
        textReceiptCustomer = findViewById(R.id.textReceiptCustomer)
        btnDoneBill = findViewById(R.id.btnDoneBill)

        btnBack.bounceClick {
            handleBack()
        }

        cardCategoryElectricity.bounceClick {
            setupForm("Electricity", "PLN Postpaid (Tagihan Listrik)", "Customer ID / Meter No", "5123 4567 8901", 285000)
        }

        cardCategoryInternet.bounceClick {
            setupForm("Internet & TV", "IndiHome Fiber", "Subscriber Number", "1229 4810 9923", 380000)
        }

        cardCategoryWater.bounceClick {
            setupForm("Water (PDAM)", "PDAM Jaya Jakarta", "Customer No", "0091 8823 4410", 145000)
        }

        cardCategoryBpjs.bounceClick {
            setupForm("BPJS Kesehatan", "BPJS Kesehatan", "VA / Virtual Account", "8888 8012 3456 7890", 150000)
        }

        recentBill1.bounceClick {
            setupForm("Electricity", "PLN Postpaid (Tagihan Listrik)", "Customer ID / Meter No", "5123 4567 8901", 285000)
        }

        recentBill2.bounceClick {
            setupForm("Internet & TV", "IndiHome Fiber", "Subscriber Number", "1229 4810 9923", 380000)
        }

        btnInquireBill.bounceClick {
            val customerId = inputCustomerId.text.toString().trim()
            if (customerId.isEmpty()) {
                Toast.makeText(this, getString(R.string.bill_error_customer), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            showSummary(customerId)
        }

        btnPayBillConfirm.bounceClick {
            if (!com.example.bankapputomo.util.CardSecurityHelper.isPrimaryCardActive(this)) {
                com.example.bankapputomo.util.CardSecurityHelper.showCardLockedBottomSheet(this)
                return@bounceClick
            }

            val pin = inputBillPin.text.toString().trim()
            if (pin.length < 6) {
                Toast.makeText(this, getString(R.string.bill_error_pin), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val total = billAmount + adminFee
            val success = AppStorage.get().addTransaction(
                title = getString(R.string.bill_transaction_title, currentCategory, currentProvider),
                category = "Bill Payment",
                amount = total,
                isIncome = false,
                counterparty = currentProvider
            )

            if (success) {
                ReceiptActivity.start(
                    context = this,
                    title = getString(R.string.bill_transaction_title, currentCategory, currentProvider),
                    category = "Tagihan $currentCategory",
                    amount = billAmount,
                    fee = adminFee,
                    counterparty = currentProvider,
                    recipientAccount = "ID: 5123 **** 8901",
                    bankName = "UTOMO Bank Biller",
                    note = "Pembayaran $currentCategory",
                    origin = "BILL"
                )
                finish()
            } else {
                com.example.bankapputomo.util.TransactionFeedbackDialog.show(
                    context = this,
                    isSuccess = false,
                    title = getString(R.string.bill_payment_failed),
                    message = "Insufficient balance in your account to pay ${com.example.bankapputomo.util.IdrFormatter.format(total)} for $currentProvider.",
                    actionButtonText = "Close"
                )
            }
        }

        btnDoneBill.bounceClick {
            finish()
        }
    }

    private fun setupForm(category: String, provider: String, idLabel: String, defaultId: String, amount: Long) {
        currentCategory = category
        currentProvider = provider
        billAmount = amount
        textSelectedProvider.text = provider
        textCustomerIdLabel.text = idLabel
        inputCustomerId.setText(defaultId)

        viewCategoryHub.visibility = View.GONE
        viewBillForm.visibility = View.VISIBLE
        viewBillConfirm.visibility = View.GONE
        viewBillReceipt.visibility = View.GONE
        billHeaderTitle.text = category
    }

    private fun showSummary(customerId: String) {
        textSummaryCustomerName.text = AppStorage.get().userName.ifEmpty { "NASABAH" }
        textSummaryCustomerId.text = customerId
        textSummaryBillAmount.text = IdrFormatter.format(billAmount)
        textSummaryTotalPayment.text = IdrFormatter.format(billAmount + adminFee)

        viewCategoryHub.visibility = View.GONE
        viewBillForm.visibility = View.GONE
        viewBillConfirm.visibility = View.VISIBLE
        viewBillReceipt.visibility = View.GONE
        billHeaderTitle.text = getString(R.string.bill_payment_confirmation)
    }

    private fun showReceipt() {
        viewCategoryHub.visibility = View.GONE
        viewBillForm.visibility = View.GONE
        viewBillConfirm.visibility = View.GONE
        viewBillReceipt.visibility = View.VISIBLE
        billHeaderTitle.text = getString(R.string.bill_receipt)

        val anim = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieBillSuccessAnim)
        if (anim != null) {
            anim.setAnimation("anim_success.json")
            com.example.bankapputomo.util.LottieMotionHelper.recolor(
                anim,
                androidx.core.content.ContextCompat.getColor(this, R.color.figma_secondary)
            )
            com.example.bankapputomo.util.LottieMotionHelper.playOnce(anim)
        }

        textReceiptBillTotal.text = IdrFormatter.format(billAmount + adminFee)
        textReceiptService.text = currentProvider
        textReceiptCustomer.text = AppStorage.get().userName.ifEmpty { "NASABAH" }
    }

    private fun handleBack() {
        if (viewBillReceipt.visibility == View.VISIBLE) {
            finish()
        } else if (viewBillConfirm.visibility == View.VISIBLE) {
            viewBillConfirm.visibility = View.GONE
            viewBillForm.visibility = View.VISIBLE
            billHeaderTitle.text = currentCategory
        } else if (viewBillForm.visibility == View.VISIBLE) {
            viewBillForm.visibility = View.GONE
            viewCategoryHub.visibility = View.VISIBLE
            billHeaderTitle.text = getString(R.string.bill_pay_the_bill)
        } else {
            finish()
        }
    }

}
