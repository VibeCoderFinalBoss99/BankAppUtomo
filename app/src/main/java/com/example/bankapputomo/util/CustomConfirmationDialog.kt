package com.example.bankapputomo.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.bankapputomo.R
import com.example.bankapputomo.util.ViewExtensions.bounceClick

object CustomConfirmationDialog {

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun show(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "Lanjutkan",
        negativeText: String = "Batal",
        iconRes: Int = R.drawable.ic_bell,
        isDestructive: Boolean = false,
        neutralText: String? = null,
        onNeutral: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        onConfirm: () -> Unit
    ) {
        val activity = findActivity(context) ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_custom_confirmation, null)
        dialog.setContentView(view)

        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.90).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val textTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val textMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val iconView = view.findViewById<ImageView>(R.id.dialogIcon)
        val iconContainer = view.findViewById<FrameLayout>(R.id.dialogIconContainer)
        val btnCancel = view.findViewById<TextView>(R.id.btnDialogCancel)
        val btnConfirm = view.findViewById<TextView>(R.id.btnDialogConfirm)
        val btnNeutral = view.findViewById<TextView>(R.id.btnDialogNeutral)

        textTitle.text = title
        textMessage.text = message
        iconView.setImageResource(iconRes)
        btnCancel.text = negativeText
        btnConfirm.text = positiveText

        if (isDestructive) {
            iconContainer.setBackgroundResource(R.drawable.bg_tile_soft_red)
            iconView.setColorFilter(ContextCompat.getColor(activity, R.color.bank_red))
            btnConfirm.setBackgroundResource(R.drawable.bg_button_danger)
            btnConfirm.setTextColor(Color.WHITE)
        } else {
            iconContainer.setBackgroundResource(R.drawable.bg_tile_soft_blue)
            iconView.setColorFilter(ContextCompat.getColor(activity, R.color.figma_primary))
            btnConfirm.setBackgroundResource(R.drawable.bg_figma_button_primary)
            btnConfirm.setTextColor(Color.WHITE)
        }

        if (neutralText != null && onNeutral != null) {
            btnNeutral.visibility = View.VISIBLE
            btnNeutral.text = neutralText
            btnNeutral.bounceClick {
                dialog.dismiss()
                onNeutral()
            }
        } else {
            btnNeutral.visibility = View.GONE
        }

        btnCancel.bounceClick {
            dialog.dismiss()
            onCancel?.invoke()
        }

        btnConfirm.bounceClick {
            dialog.dismiss()
            onConfirm()
        }

        dialog.setOnCancelListener {
            onCancel?.invoke()
        }

        dialog.show()
    }
}
