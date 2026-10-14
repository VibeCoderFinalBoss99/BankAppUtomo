package com.example.bankapputomo

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class CreditCardActivity : BaseActivity() {

    private lateinit var stepStatement: View
    private lateinit var stepPayForm: View
    private lateinit var stepConfirm: View
    private lateinit var stepPin: View
    private lateinit var stepSuccess: View
    private lateinit var inputAmount: EditText
    private lateinit var textConfirmAmount: TextView
    private lateinit var textSuccessAmount: TextView

    private var currentStep = 1
    private var paymentAmount = 4500000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credit_card)

        findViewById<View>(R.id.btnBack).bounceClick {
            if (currentStep > 1 && currentStep < 5) {
                goToStep(currentStep - 1)
            } else {
                finish()
            }
        }

        stepStatement = findViewById(R.id.stepCreditCardStatement)
        stepPayForm = findViewById(R.id.stepCreditCardPayForm)
        stepConfirm = findViewById(R.id.stepCreditCardConfirm)
        stepPin = findViewById(R.id.stepCreditCardPin)
        stepSuccess = findViewById(R.id.stepCreditCardSuccess)

        inputAmount = findViewById(R.id.inputCreditCardAmount)
        textConfirmAmount = findViewById(R.id.textConfirmAmount)
        textSuccessAmount = findViewById(R.id.textSuccessAmount)

        IdrFormatter.applyAmountInputWatcher(inputAmount) { rawVal ->
            paymentAmount = rawVal
        }
        inputAmount.setText(IdrFormatter.format(4500000L, withPrefix = false))

        findViewById<View>(R.id.btnGoToPayCreditCard).bounceClick {
            goToStep(2)
        }

        findViewById<View>(R.id.btnSubmitCreditCardPay).bounceClick {
            if (paymentAmount <= 0L) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            if (paymentAmount > AppStorage.get().debitBalance) {
                Toast.makeText(this, "Insufficient balance (${IdrFormatter.format(AppStorage.get().debitBalance)})", Toast.LENGTH_LONG).show()
                return@bounceClick
            }
            textConfirmAmount.text = IdrFormatter.format(paymentAmount)
            goToStep(3)
        }

        findViewById<View>(R.id.btnPromptPin).bounceClick {
            goToStep(4)
        }

        val inputPin = findViewById<EditText>(R.id.inputPin)
        findViewById<View>(R.id.btnVerifyPin).bounceClick {
            val pin = inputPin.text.toString()
            if (pin.length < 6) {
                Toast.makeText(this, "Please enter 6-digit PIN", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val success = AppStorage.get().addTransaction(
                title = "Credit Card Bill Payment",
                category = "Credit Card",
                amount = paymentAmount,
                isIncome = false,
                counterparty = "VISA Card 9018"
            )

            if (success) {
                textSuccessAmount.text = IdrFormatter.format(paymentAmount)
                goToStep(5)
            } else {
                Toast.makeText(this, "Insufficient balance to pay credit card bill", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<View>(R.id.btnDoneSuccess).bounceClick {
            ReceiptActivity.start(
                context = this,
                title = "Pembayaran Tagihan Kartu Kredit",
                category = "Kartu Kredit",
                amount = paymentAmount,
                fee = 0L,
                counterparty = "Kartu Kredit VISA Utomo",
                recipientAccount = "4242 **** 9018",
                bankName = "UTOMO Bank Card Center",
                note = "Pembayaran Tagihan Kartu Kredit",
                origin = "CARD"
            )
            finish()
        }
    }

    private fun goToStep(step: Int) {
        currentStep = step
        stepStatement.visibility = if (step == 1) View.VISIBLE else View.GONE
        stepPayForm.visibility = if (step == 2) View.VISIBLE else View.GONE
        stepConfirm.visibility = if (step == 3) View.VISIBLE else View.GONE
        stepPin.visibility = if (step == 4) View.VISIBLE else View.GONE
        stepSuccess.visibility = if (step == 5) View.VISIBLE else View.GONE
    }
}
