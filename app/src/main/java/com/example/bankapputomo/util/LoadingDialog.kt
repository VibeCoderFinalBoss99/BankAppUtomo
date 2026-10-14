package com.example.bankapputomo.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.bankapputomo.R

object LoadingDialog {

    fun create(context: Context, message: String = "Processing..."): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        dialog.setContentView(view)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val lottie = view.findViewById<LottieAnimationView>(R.id.lottieLoadingIndicator)
        val textMessage = view.findViewById<TextView>(R.id.textLoadingMessage)

        textMessage.text = message

        lottie.setAnimation("anim_loading.json")
        LottieMotionHelper.recolor(
            lottie,
            ContextCompat.getColor(context, R.color.figma_primary)
        )
        LottieMotionHelper.playLoop(lottie)

        dialog.setCancelable(false)
        return dialog
    }
}
