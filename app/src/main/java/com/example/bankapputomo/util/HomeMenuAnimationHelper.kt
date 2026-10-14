package com.example.bankapputomo.util

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.bankapputomo.R

object HomeMenuAnimationHelper {

    /**
     * Attaches an animated Lottie micro-interaction from useAnimations to a home menu item,
     * and triggers the animation smoothly before navigating to the destination screen.
     */
    fun setupAnimatedMenu(
        buttonView: View?,
        iconContainer: ViewGroup?,
        staticIcon: ImageView?,
        assetName: String,
        iconTintRes: Int = R.color.figma_primary,
        onNavigate: () -> Unit
    ) {
        if (buttonView == null || iconContainer == null) return

        val context = buttonView.context
        val density = context.resources.displayMetrics.density
        val iconSize = (26 * density).toInt()

        // Reuse or create LottieAnimationView inside icon container
        val existing = iconContainer.findViewWithTag<LottieAnimationView>("lottie_menu_$assetName")
        val lottieView = existing ?: LottieAnimationView(context).apply {
            tag = "lottie_menu_$assetName"
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
            try {
                setAnimation(assetName)
                setFailureListener { _ -> }
            } catch (e: Throwable) {
                // Fall back gracefully to static icon
            }
            repeatCount = 0
            visibility = View.GONE
            iconContainer.addView(this)
        }

        val tintColor = ContextCompat.getColor(context, iconTintRes)
        LottieMotionHelper.recolor(lottieView, tintColor)

        buttonView.setOnClickListener {
            // 1. Tactile spring scale bounce
            buttonView.animate()
                .scaleX(0.90f)
                .scaleY(0.90f)
                .setDuration(90)
                .withEndAction {
                    buttonView.animate()
                        .scaleX(1.04f)
                        .scaleY(1.04f)
                        .setDuration(110)
                        .withEndAction {
                            buttonView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(80)
                                .start()
                        }
                        .start()
                }
                .start()

            // 2. Play useAnimations Lottie micro-interaction
            staticIcon?.animate()?.alpha(0f)?.setDuration(80)?.start()
            lottieView.visibility = View.VISIBLE
            lottieView.progress = 0f
            lottieView.playAnimation()

            // 3. Navigate after tactile animation plays (~280ms)
            buttonView.postDelayed({
                onNavigate()
                // Reset icon state smoothly for when user navigates back
                buttonView.postDelayed({
                    staticIcon?.alpha = 1f
                    lottieView.visibility = View.GONE
                    lottieView.progress = 0f
                }, 500)
            }, 280)
        }
    }
}
