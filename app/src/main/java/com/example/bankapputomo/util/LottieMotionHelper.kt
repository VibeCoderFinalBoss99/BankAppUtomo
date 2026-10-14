package com.example.bankapputomo.util

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.provider.Settings
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.example.bankapputomo.R

object LottieMotionHelper {

    /**
     * Checks if user has enabled "Reduce Motion" in system settings.
     */
    fun isReduceMotionEnabled(context: Context): Boolean {
        return try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Dynamically recolors all stroke/fill vector layers in a LottieAnimationView.
     */
    fun recolor(view: LottieAnimationView, @ColorInt color: Int) {
        val filter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER
        ) { filter }
        view.colorFilter = filter
    }

    /**
     * Plays a single-shot micro-interaction animation respecting Reduce Motion.
     */
    fun playOnce(
        view: LottieAnimationView,
        onComplete: (() -> Unit)? = null
    ) {
        if (isReduceMotionEnabled(view.context)) {
            view.progress = 1.0f
            onComplete?.invoke()
            return
        }
        view.repeatCount = 0
        view.progress = 0f
        view.playAnimation()
    }

    /**
     * Plays an infinite loop animation (e.g. loading spinner, scanner pulse)
     */
    fun playLoop(view: LottieAnimationView) {
        if (isReduceMotionEnabled(view.context)) {
            view.progress = 0.5f
            return
        }
        view.repeatCount = LottieDrawable.INFINITE
        view.playAnimation()
    }

    /**
     * Sets up and recolors a Bottom Navigation animated icon.
     */
    fun bindBottomNavIcon(
        view: LottieAnimationView,
        assetName: String,
        isSelected: Boolean
    ) {
        val context = view.context
        val activeColor = ContextCompat.getColor(context, R.color.figma_primary)
        val inactiveColor = ContextCompat.getColor(context, R.color.figma_text_muted)
        val color = if (isSelected) activeColor else inactiveColor

        view.setAnimation(assetName)
        recolor(view, color)

        if (isSelected) {
            playOnce(view)
        } else {
            view.progress = 0f
            view.cancelAnimation()
        }
    }
}
