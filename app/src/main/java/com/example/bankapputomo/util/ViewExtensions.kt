package com.example.bankapputomo.util

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import com.example.bankapputomo.R

object ViewExtensions {

    @SuppressLint("ClickableViewAccessibility")
    fun View.bounceClick(onClick: () -> Unit) {
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).withEndAction {
                        onClick()
                    }.start()
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }
            }
            true
        }
    }

    fun View.shake() {
        val animation = AnimationUtils.loadAnimation(context, R.anim.shake)
        startAnimation(animation)
    }

    fun View.setValidationState(isValid: Boolean?, checkIcon: ImageView? = null, shakeOnError: Boolean = false) {
        when (isValid) {
            true -> {
                setBackgroundResource(R.drawable.bg_figma_input_valid)
                if (checkIcon != null && checkIcon.visibility != View.VISIBLE) {
                    checkIcon.visibility = View.VISIBLE
                    val popAnim = AnimationUtils.loadAnimation(context, R.anim.pop_in)
                    checkIcon.startAnimation(popAnim)
                }
            }
            false -> {
                setBackgroundResource(R.drawable.bg_figma_input_error)
                checkIcon?.visibility = View.GONE
                if (shakeOnError) {
                    shake()
                }
            }
            null -> {
                setBackgroundResource(R.drawable.bg_figma_input)
                checkIcon?.visibility = View.GONE
            }
        }
    }
}
