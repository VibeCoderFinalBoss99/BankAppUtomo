package com.example.bankapputomo.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Confirm your fingerprint or face to proceed",
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If user cancelled, report it; otherwise offer fallback
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        onError?.invoke(errString.toString())
                    } else {
                        showFallbackDialog(activity, onSuccess, onError)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError?.invoke("Authentication failed. Please try again.")
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Cancel")
                .build()

            prompt.authenticate(promptInfo)
        } else {
            // Emulators or devices without enrolled biometric
            showFallbackDialog(activity, onSuccess, onError)
        }
    }

    private fun showFallbackDialog(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)?
    ) {
        CustomConfirmationDialog.show(
            context = activity,
            title = "Verifikasi Biometrik",
            message = "Sensor biometrik disimulasikan untuk verifikasi identitas Anda.",
            positiveText = "Otorisasi",
            negativeText = "Batal",
            iconRes = com.example.bankapputomo.R.drawable.ic_figma_fingerprint,
            isDestructive = false,
            onConfirm = { onSuccess() },
            onCancel = { onError?.invoke("Verifikasi dibatalkan") }
        )
    }
}
