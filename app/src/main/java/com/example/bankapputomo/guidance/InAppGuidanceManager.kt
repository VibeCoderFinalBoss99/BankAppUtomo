package com.example.bankapputomo.guidance

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.app.Dialog
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.bankapputomo.R
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView

object InAppGuidanceManager {

    data class StepInfo(
        val targetViewId: Int,
        val badgeText: String,
        val title: String,
        val description: String
    )

    private val tourSteps = listOf(
        StepInfo(
            targetViewId = R.id.homeDeckContainer,
            badgeText = "Langkah 1 dari 4 • Saldo & Kartu",
            title = "Pantau Saldo & Kartu Virtual",
            description = "Ketuk kartu digital ini untuk melihat detail nomor kartu, CVV, mutasi rekening, dan mengatur limit belanja bulanan Anda dengan aman."
        ),
        StepInfo(
            targetViewId = R.id.cardQuickMenuContainer,
            badgeText = "Langkah 2 dari 4 • Transaksi Cepat",
            title = "Menu Cepat 1-Ketukan",
            description = "Kirim uang kilat antar bank, terima transfer QRIS, bayar tagihan bulanan, dan top-up saldo dompet digital langsung dari halaman utama."
        ),
        StepInfo(
            targetViewId = R.id.cardNineMenuContainer,
            badgeText = "Langkah 3 dari 4 • Layanan Finansial",
            title = "9 Layanan Lengkap Perbankan",
            description = "Akses tarik tunai tanpa kartu di ATM, buka rekening tabungan berjangka online, manajemen kartu kredit, dan laporan mutasi lengkap."
        ),
        StepInfo(
            targetViewId = R.id.fabQr,
            badgeText = "Langkah 4 dari 4 • Pembayaran QRIS",
            title = "Scan & Bayar Praktis via QRIS",
            description = "Gunakan tombol tengah ini kapan saja untuk memindai kode QR standar nasional di kasir atau merchant favorit tanpa uang tunai."
        )
    )

    private var isGuidanceActive = false
    private var activeWelcomeDialog: Dialog? = null
    private var activeSpotlightDialog: Dialog? = null

    fun dismissAll() {
        try {
            activeWelcomeDialog?.dismiss()
        } catch (_: Exception) {}
        activeWelcomeDialog = null

        try {
            activeSpotlightDialog?.dismiss()
        } catch (_: Exception) {}
        activeSpotlightDialog = null

        isGuidanceActive = false
    }

    fun checkAndStartGuidance(
        activity: Activity,
        force: Boolean = false,
        onFinished: (() -> Unit)? = null
    ) {
        val storage = AppStorage.get()
        if (!force && storage.hasSeenInAppGuidance) {
            return
        }
        if (isGuidanceActive || activeWelcomeDialog?.isShowing == true || activeSpotlightDialog?.isShowing == true) {
            return
        }

        isGuidanceActive = true
        showWelcomePrompt(activity) {
            startSpotlightTour(activity) {
                isGuidanceActive = false
                onFinished?.invoke()
            }
        }
    }

    fun showWelcomePrompt(activity: Activity, onStartTour: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            isGuidanceActive = false
            return
        }

        try {
            activeWelcomeDialog?.dismiss()
        } catch (_: Exception) {}
        activeWelcomeDialog = null

        val dialog = Dialog(activity)
        activeWelcomeDialog = dialog
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val sheetView = LayoutInflater.from(activity).inflate(R.layout.dialog_guidance_welcome, null)
        dialog.setContentView(sheetView)
        dialog.setCancelable(false)

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.92).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val btnStart = sheetView.findViewById<TextView>(R.id.btnStartGuidance)
        val btnSkip = sheetView.findViewById<TextView>(R.id.btnSkipGuidance)

        btnStart.bounceClick {
            activeWelcomeDialog = null
            dialog.dismiss()
            onStartTour()
        }

        btnSkip.bounceClick {
            AppStorage.get().hasSeenInAppGuidance = true
            activeWelcomeDialog = null
            isGuidanceActive = false
            dialog.dismiss()
            Toast.makeText(activity, "Panduan dapat dibuka kembali di menu Pengaturan", Toast.LENGTH_SHORT).show()
        }

        dialog.setOnDismissListener {
            if (activeWelcomeDialog == dialog) {
                activeWelcomeDialog = null
            }
        }

        try {
            dialog.show()
        } catch (_: Exception) {
            isGuidanceActive = false
        }
    }

    fun startSpotlightTour(activity: Activity, onFinished: (() -> Unit)? = null) {
        if (activity.isFinishing || activity.isDestroyed) {
            isGuidanceActive = false
            return
        }

        try {
            activeWelcomeDialog?.dismiss()
        } catch (_: Exception) {}
        activeWelcomeDialog = null

        val dialog = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        activeSpotlightDialog = dialog
        val overlayView = LayoutInflater.from(activity).inflate(R.layout.dialog_guidance_spotlight, null)
        dialog.setContentView(overlayView)
        dialog.setCancelable(false)

        val viewSpotlight = overlayView.findViewById<View>(R.id.viewSpotlightTarget)
        val cardTooltip = overlayView.findViewById<MaterialCardView>(R.id.cardGuidanceTooltip)
        val textBadge = overlayView.findViewById<TextView>(R.id.textStepBadge)
        val textTitle = overlayView.findViewById<TextView>(R.id.textStepTitle)
        val textDesc = overlayView.findViewById<TextView>(R.id.textStepDesc)
        val btnPrev = overlayView.findViewById<TextView>(R.id.btnStepPrev)
        val btnNext = overlayView.findViewById<TextView>(R.id.btnStepNext)
        val btnSkip = overlayView.findViewById<TextView>(R.id.btnStepSkip)

        val tourDots = listOf(
            overlayView.findViewById<View>(R.id.tourDot0),
            overlayView.findViewById<View>(R.id.tourDot1),
            overlayView.findViewById<View>(R.id.tourDot2),
            overlayView.findViewById<View>(R.id.tourDot3)
        )

        var currentStepIndex = 0
        var pulseAnimator: ObjectAnimator? = null

        fun animatePulse(target: View) {
            pulseAnimator?.cancel()
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.04f, 1f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.04f, 1f)
            pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(target, scaleX, scaleY).apply {
                duration = 1200
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            pulseAnimator?.start()
        }

        fun updateStep(stepIndex: Int) {
            currentStepIndex = stepIndex
            val step = tourSteps[stepIndex]

            textBadge.text = step.badgeText
            textTitle.text = step.title
            textDesc.text = step.description

            btnPrev.visibility = if (stepIndex > 0) View.VISIBLE else View.GONE
            btnNext.text = if (stepIndex == tourSteps.size - 1) "Selesai" else "Lanjut"

            // Update dots
            tourDots.forEachIndexed { i, dot ->
                val params = dot.layoutParams
                if (i == stepIndex) {
                    params.width = (16 * activity.resources.displayMetrics.density).toInt()
                    dot.setBackgroundResource(R.drawable.bg_figma_button_primary)
                } else {
                    params.width = (6 * activity.resources.displayMetrics.density).toInt()
                    dot.setBackgroundResource(R.drawable.bg_onboarding_dot_inactive)
                }
                dot.layoutParams = params
            }

            // Scroll to view if inside NestedScrollView
            val targetView = activity.findViewById<View>(step.targetViewId)
            val nestedScrollView = activity.findViewById<androidx.core.widget.NestedScrollView>(R.id.homeScrollView)

            if (targetView != null && step.targetViewId != R.id.fabQr && nestedScrollView != null) {
                try {
                    val targetRect = Rect()
                    targetView.getDrawingRect(targetRect)
                    nestedScrollView.offsetDescendantRectToMyCoords(targetView, targetRect)
                    val scrollY = maxOf(0, targetRect.top - (60 * activity.resources.displayMetrics.density).toInt())
                    nestedScrollView.smoothScrollTo(0, scrollY)
                } catch (_: Exception) {
                    val scrollY = maxOf(0, targetView.top - (60 * activity.resources.displayMetrics.density).toInt())
                    nestedScrollView.smoothScrollTo(0, scrollY)
                }
            }

            overlayView.postDelayed({
                if (dialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                    val currentTarget = activity.findViewById<View>(step.targetViewId) ?: targetView
                    if (currentTarget != null) {
                        val rect = Rect()
                        currentTarget.getGlobalVisibleRect(rect)

                        if (rect.width() > 0 && rect.height() > 0) {
                            val padding = (8 * activity.resources.displayMetrics.density).toInt()
                            val spotlightParams = FrameLayout.LayoutParams(
                                rect.width() + (padding * 2),
                                rect.height() + (padding * 2)
                            ).apply {
                                leftMargin = rect.left - padding
                                topMargin = rect.top - padding
                            }
                            viewSpotlight.layoutParams = spotlightParams
                            viewSpotlight.visibility = View.VISIBLE
                            animatePulse(viewSpotlight)

                            // Adjust Tooltip Position (Top or Bottom based on target location)
                            val screenHeight = activity.resources.displayMetrics.heightPixels
                            val cardParams = cardTooltip.layoutParams as? RelativeLayout.LayoutParams
                            if (cardParams != null) {
                                cardParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
                                cardParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

                                if (rect.centerY() > screenHeight / 2) {
                                    // Target is in lower half -> place card at top
                                    cardParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                                    cardParams.topMargin = (70 * activity.resources.displayMetrics.density).toInt()
                                    cardParams.bottomMargin = 0
                                } else {
                                    // Target is in upper half -> place card at bottom
                                    cardParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                                    cardParams.bottomMargin = (60 * activity.resources.displayMetrics.density).toInt()
                                    cardParams.topMargin = 0
                                }
                                cardTooltip.layoutParams = cardParams
                            }
                        }
                    }
                }
            }, 300)
        }

        btnNext.bounceClick {
            if (currentStepIndex < tourSteps.size - 1) {
                updateStep(currentStepIndex + 1)
            } else {
                AppStorage.get().hasSeenInAppGuidance = true
                activeSpotlightDialog = null
                isGuidanceActive = false
                pulseAnimator?.cancel()
                try { dialog.dismiss() } catch (_: Exception) {}
                Toast.makeText(activity, "Selamat bertransaksi di UTOMO Bank!", Toast.LENGTH_SHORT).show()
                onFinished?.invoke()
            }
        }

        btnPrev.bounceClick {
            if (currentStepIndex > 0) {
                updateStep(currentStepIndex - 1)
            }
        }

        btnSkip.bounceClick {
            AppStorage.get().hasSeenInAppGuidance = true
            activeSpotlightDialog = null
            isGuidanceActive = false
            pulseAnimator?.cancel()
            try { dialog.dismiss() } catch (_: Exception) {}
            Toast.makeText(activity, "Panduan dilewati. Selamat bertransaksi!", Toast.LENGTH_SHORT).show()
            onFinished?.invoke()
        }

        dialog.setOnDismissListener {
            pulseAnimator?.cancel()
            if (activeSpotlightDialog == dialog) {
                activeSpotlightDialog = null
                isGuidanceActive = false
            }
        }

        try {
            dialog.show()
            updateStep(0)
        } catch (_: Exception) {
            isGuidanceActive = false
        }
    }
}
