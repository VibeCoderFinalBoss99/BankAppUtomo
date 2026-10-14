package com.example.bankapputomo.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.bankapputomo.R
import com.example.bankapputomo.util.ViewExtensions.bounceClick

object TransactionFeedbackDialog {

    fun show(
        context: Context,
        isSuccess: Boolean,
        title: String,
        message: String,
        referenceCode: String? = null,
        actionButtonText: String = "Done",
        onDone: (() -> Unit)? = null
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_transaction_feedback, null)
        dialog.setContentView(view)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val iconContainer = view.findViewById<FrameLayout>(R.id.feedbackIconContainer)
        val lottieAnim = view.findViewById<LottieAnimationView>(R.id.lottieFeedbackAnim)
        val textTitle = view.findViewById<TextView>(R.id.textFeedbackTitle)
        val textMessage = view.findViewById<TextView>(R.id.textFeedbackMessage)
        val textReference = view.findViewById<TextView>(R.id.textFeedbackReference)
        val btnAction = view.findViewById<TextView>(R.id.btnFeedbackAction)

        textTitle.text = title
        textMessage.text = message
        btnAction.text = actionButtonText

        if (!referenceCode.isNullOrEmpty()) {
            textReference.visibility = View.VISIBLE
            textReference.text = referenceCode
        } else {
            textReference.visibility = View.GONE
        }

        if (isSuccess) {
            iconContainer.setBackgroundResource(R.drawable.bg_tile_soft_green)
            lottieAnim.setAnimation("anim_success.json")
            val greenColor = ContextCompat.getColor(context, R.color.figma_secondary)
            LottieMotionHelper.recolor(lottieAnim, greenColor)
            LottieMotionHelper.playOnce(lottieAnim)
        } else {
            iconContainer.setBackgroundResource(R.drawable.bg_tile_soft_red)
            lottieAnim.setAnimation("anim_failed.json")
            val redColor = ContextCompat.getColor(context, R.color.bank_red)
            LottieMotionHelper.recolor(lottieAnim, redColor)
            LottieMotionHelper.playOnce(lottieAnim)
        }

        btnAction.bounceClick {
            dialog.dismiss()
            onDone?.invoke()
        }

        dialog.setCancelable(true)
        dialog.show()
        return dialog
    }
}
