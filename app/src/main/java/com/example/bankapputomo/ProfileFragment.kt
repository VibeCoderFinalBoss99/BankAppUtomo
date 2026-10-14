package com.example.bankapputomo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bankapputomo.guidance.InAppGuidanceManager
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.AvatarHelper
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.CustomConfirmationDialog
import com.example.bankapputomo.util.EditProfileDialogFragment
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog

class ProfileFragment : Fragment() {

    private var activeLinkGoogleDialog: BottomSheetDialog? = null

    private fun buildGoogleSignInOptions(): GoogleSignInOptions {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        val clientIdRes = resources.getIdentifier("default_web_client_id", "string", requireContext().packageName)
        if (clientIdRes != 0) {
            val clientId = getString(clientIdRes)
            if (clientId.isNotBlank()) {
                builder.requestIdToken(clientId)
            }
        }
        return builder.build()
    }

    private val googleLinkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            val name = (account?.displayName ?: account?.givenName ?: "").ifEmpty {
                AppStorage.get(requireContext()).userName
            }
            if (email.isNotEmpty()) {
                performGoogleLinking(name, email)
                activeLinkGoogleDialog?.dismiss()
            } else {
                Toast.makeText(requireContext(), "Gagal membaca email akun Google.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            if (e.statusCode == 12501) {
                Toast.makeText(requireContext(), "Pemilihan akun Google dibatalkan.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Google Sign-In tidak dapat menghubungkan secara otomatis (${e.statusCode}). Anda dapat memasukkan akun Google di bawah ini.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal menautkan Google: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performGoogleLinking(name: String, email: String) {
        val cleanName = name.trim().ifEmpty { AppStorage.get(requireContext()).userName.ifEmpty { "Nasabah Bank UTOMO" } }
        val cleanEmail = email.trim()

        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            Toast.makeText(requireContext(), "Format email Google tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val storage = AppStorage.get(requireContext())
        storage.googleLinked = true
        storage.googleEmail = cleanEmail
        storage.googleName = cleanName

        val uid = BackOfficeRepository.getEffectiveUserId(requireContext())
        storage.updateRegisteredUserSecurity(uid, gLinked = true, gEmail = cleanEmail, gName = cleanName)

        BackOfficeRepository.firestore.collection("users").document(uid).set(
            mapOf(
                "googleLinked" to true,
                "googleEmail" to cleanEmail,
                "googleName" to cleanName
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )

        view?.let { updateGoogleLinkStatusUI(it) }
        Toast.makeText(
            requireContext(),
            "Akun Google ($cleanEmail) atas nama $cleanName berhasil ditautkan!",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    private fun refreshProfileHeader(rootView: View) {
        val storage = AppStorage.get(requireContext())
        val textProfileName = rootView.findViewById<TextView>(R.id.textProfileName)
        val textProfilePhone = rootView.findViewById<TextView>(R.id.textProfilePhone)
        val textProfileEmail = rootView.findViewById<TextView>(R.id.textProfileEmail)
        val profileAvatar = rootView.findViewById<ImageView>(R.id.profileAvatar)

        textProfileName?.text = storage.userName.ifEmpty { "Nasabah" }
        textProfileEmail?.text = storage.userEmail.ifEmpty { "-" }
        textProfilePhone?.text = storage.userPhone.ifEmpty { "-" }

        if (profileAvatar != null) {
            AvatarHelper.loadAvatar(requireContext(), profileAvatar)
        }
    }

    private fun updateGoogleLinkStatusUI(rootView: View?) {
        val root = rootView ?: return
        val storage = AppStorage.get(requireContext())
        val textTitle = root.findViewById<TextView>(R.id.textGoogleLinkTitle)
        val textStatus = root.findViewById<TextView>(R.id.textGoogleLinkStatus) ?: return
        val badgeConnected = root.findViewById<TextView>(R.id.badgeGoogleConnected)
        val iconGoogle = root.findViewById<ImageView>(R.id.iconGoogleStatus)

        if (storage.googleLinked && storage.googleEmail.isNotEmpty()) {
            val displayName = storage.googleName.ifEmpty { storage.userName.ifEmpty { "Nasabah" } }
            textTitle?.text = "Akun Google Terhubung"
            textStatus.text = "Atas nama: $displayName • ${storage.googleEmail}"
            textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.figma_primary))
            badgeConnected?.visibility = View.VISIBLE
            iconGoogle?.setImageResource(R.drawable.ic_google)
        } else {
            textTitle?.text = "Tautkan Akun Google"
            textStatus.text = "Belum terhubung - Ketuk untuk tautkan"
            textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.figma_text_muted))
            badgeConnected?.visibility = View.GONE
            iconGoogle?.setImageResource(R.drawable.ic_google)
        }
    }

    private fun openEditProfile() {
        val dialog = EditProfileDialogFragment()
        dialog.onProfileUpdated = {
            view?.let { refreshProfileHeader(it) }
        }
        dialog.show(parentFragmentManager, "EditProfileDialog")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val storage = AppStorage.get()

        // Bind user header
        refreshProfileHeader(view)
        updateGoogleLinkStatusUI(view)

        // Edit Profile click triggers
        view.findViewById<View>(R.id.cardUserProfile).bounceClick {
            openEditProfile()
        }
        view.findViewById<View>(R.id.btnEditProfileHeader)?.bounceClick {
            openEditProfile()
        }
        view.findViewById<View>(R.id.btnEditProfileData)?.bounceClick {
            openEditProfile()
        }

        // Logout Confirmation via Custom Branded Dialog
        view.findViewById<View>(R.id.btnLogout).bounceClick {
            CustomConfirmationDialog.show(
                context = requireContext(),
                title = getString(R.string.item_logout),
                message = "Apakah Anda yakin ingin keluar dari akun UTOMO Bank Anda?",
                positiveText = "Keluar",
                negativeText = "Batal",
                iconRes = R.drawable.ic_user,
                isDestructive = true,
                onConfirm = {
                    storage.isLoggedIn = false
                    val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
            )
        }

        // Dark Mode
        val switchDarkMode = view.findViewById<SwitchCompat>(R.id.switchDarkMode)
        switchDarkMode.isChecked = storage.isDarkMode
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            storage.isDarkMode = isChecked
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Biometrics
        val switchBiometrics = view.findViewById<SwitchCompat>(R.id.switchBiometrics)
        switchBiometrics.isChecked = storage.biometricEnabled
        switchBiometrics.setOnCheckedChangeListener { _, isChecked ->
            storage.biometricEnabled = isChecked
            val uid = BackOfficeRepository.getEffectiveUserId(requireContext())
            storage.updateRegisteredUserSecurity(uid, bio = isChecked)
            BackOfficeRepository.firestore.collection("users").document(uid).update(
                mapOf("biometricEnabled" to isChecked)
            )
            Toast.makeText(
                requireContext(),
                if (isChecked) "Login biometrik diaktifkan! Tombol biometrik kini aktif di halaman login." else "Login biometrik dinonaktifkan.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Change PIN via Custom Bottom Sheet
        view.findViewById<View>(R.id.btnChangePin).bounceClick {
            showChangePinBottomSheet()
        }

        // Link Google Account
        view.findViewById<View>(R.id.btnLinkGoogle)?.bounceClick {
            if (storage.googleLinked && storage.googleEmail.isNotEmpty()) {
                showLinkedGoogleAccountDetailBottomSheet()
            } else {
                showLinkGoogleBottomSheet()
            }
        }

        // Change Password via Custom Bottom Sheet
        view.findViewById<View>(R.id.btnChangePassword).bounceClick {
            showChangePasswordBottomSheet()
        }

        // Language selector
        val textCurrentLanguage = view.findViewById<TextView>(R.id.textCurrentLanguage)
        textCurrentLanguage.text = getLanguageDisplayName(storage.language)

        view.findViewById<View>(R.id.layoutLanguage).bounceClick {
            startActivity(Intent(requireContext(), LanguageActivity::class.java))
        }

        // App Information via Custom Bottom Sheet
        view.findViewById<View>(R.id.btnAppInfo).bounceClick {
            showAppInfoBottomSheet()
        }

        // Customer Care -> CustomerSupportActivity
        view.findViewById<View>(R.id.btnCustomerCare).bounceClick {
            startActivity(Intent(requireContext(), CustomerSupportActivity::class.java))
        }

        // Guidance & Onboarding via Custom Bottom Sheet
        view.findViewById<View>(R.id.btnWalkthrough)?.bounceClick {
            showGuidanceBottomSheet()
        }
    }

    private fun showLinkGoogleBottomSheet() {
        val storage = AppStorage.get(requireContext())
        val dialog = BottomSheetDialog(requireContext())
        activeLinkGoogleDialog = dialog
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_link_google, null)
        dialog.setContentView(sheetView)

        val btnOptionGooglePlay = sheetView.findViewById<View>(R.id.btnOptionGooglePlay)
        val inputGoogleName = sheetView.findViewById<EditText>(R.id.inputGoogleName)
        val inputGoogleEmail = sheetView.findViewById<EditText>(R.id.inputGoogleEmail)
        val btnConfirmLinkGoogle = sheetView.findViewById<View>(R.id.btnConfirmLinkGoogle)
        val btnCancelLinkGoogle = sheetView.findViewById<View>(R.id.btnCancelLinkGoogle)

        val defaultName = storage.googleName.ifEmpty { storage.userName.ifEmpty { "Budi Santoso" } }
        val defaultEmail = if (storage.googleEmail.isNotEmpty()) {
            storage.googleEmail
        } else if (storage.userEmail.isNotEmpty() && storage.userEmail.contains("@gmail.com")) {
            storage.userEmail
        } else {
            val cleanUser = storage.userName.lowercase().replace(Regex("[^a-z0-9]"), ".")
            if (cleanUser.isNotEmpty()) "$cleanUser@gmail.com" else "budi.santoso@gmail.com"
        }

        inputGoogleName.setText(defaultName)
        inputGoogleEmail.setText(defaultEmail)

        btnOptionGooglePlay.bounceClick {
            val gso = buildGoogleSignInOptions()
            val client = GoogleSignIn.getClient(requireContext(), gso)
            try {
                googleLinkLauncher.launch(client.signInIntent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Google Sign-In tidak dapat dibuka: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnConfirmLinkGoogle.bounceClick {
            val name = inputGoogleName.text.toString().trim()
            val email = inputGoogleEmail.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Masukkan nama pemilik akun Google", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Masukkan format email Google yang valid (@gmail.com atau domain Google)", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            performGoogleLinking(name, email)
            dialog.dismiss()
        }

        btnCancelLinkGoogle.bounceClick {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            if (activeLinkGoogleDialog == dialog) {
                activeLinkGoogleDialog = null
            }
        }

        dialog.show()
    }

    private fun showLinkedGoogleAccountDetailBottomSheet() {
        val storage = AppStorage.get(requireContext())
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_google_account_detail, null)
        dialog.setContentView(sheetView)

        val textDetailGoogleName = sheetView.findViewById<TextView>(R.id.textDetailGoogleName)
        val textDetailGoogleEmail = sheetView.findViewById<TextView>(R.id.textDetailGoogleEmail)
        val btnChangeGoogleAccount = sheetView.findViewById<View>(R.id.btnChangeGoogleAccount)
        val btnDisconnectGoogle = sheetView.findViewById<View>(R.id.btnDisconnectGoogle)
        val btnCloseDetailGoogle = sheetView.findViewById<View>(R.id.btnCloseDetailGoogle)

        val currentName = storage.googleName.ifEmpty { storage.userName.ifEmpty { "Nasabah" } }
        val currentEmail = storage.googleEmail

        textDetailGoogleName.text = currentName
        textDetailGoogleEmail.text = currentEmail

        btnChangeGoogleAccount.bounceClick {
            dialog.dismiss()
            showLinkGoogleBottomSheet()
        }

        btnDisconnectGoogle.bounceClick {
            CustomConfirmationDialog.show(
                context = requireContext(),
                title = "Putuskan Akun Google",
                message = "Apakah Anda yakin ingin memutuskan tautan akun Google ($currentEmail) atas nama $currentName?",
                positiveText = "Putuskan",
                negativeText = "Batal",
                iconRes = R.drawable.ic_google,
                isDestructive = true,
                onConfirm = {
                    storage.googleLinked = false
                    storage.googleEmail = ""
                    storage.googleName = ""
                    val uid = BackOfficeRepository.getEffectiveUserId(requireContext())
                    storage.updateRegisteredUserSecurity(uid, gLinked = false, gEmail = "", gName = "")
                    BackOfficeRepository.firestore.collection("users").document(uid).set(
                        mapOf("googleLinked" to false, "googleEmail" to "", "googleName" to ""),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    try {
                        val gso = buildGoogleSignInOptions()
                        GoogleSignIn.getClient(requireContext(), gso).signOut()
                    } catch (e: Exception) {}
                    view?.let { updateGoogleLinkStatusUI(it) }
                    Toast.makeText(requireContext(), "Tautan Google berhasil dilepas.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            )
        }

        btnCloseDetailGoogle.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showChangePinBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_change_pin, null)
        dialog.setContentView(sheetView)

        val inputNewPin = sheetView.findViewById<EditText>(R.id.inputNewPin)
        val btnSaveNewPin = sheetView.findViewById<View>(R.id.btnSaveNewPin)
        val btnCancelPin = sheetView.findViewById<View>(R.id.btnCancelPin)

        btnSaveNewPin.bounceClick {
            val pin = inputNewPin.text.toString().trim()
            if (pin.length == 6) {
                val storage = AppStorage.get(requireContext())
                storage.userPin = pin
                storage.isPinConfigured = true
                val uid = BackOfficeRepository.getEffectiveUserId(requireContext())
                storage.updateRegisteredUserSecurity(uid, pinSet = true, pinVal = pin)
                BackOfficeRepository.firestore.collection("users").document(uid).update(
                    mapOf("pin" to pin, "pinEnabled" to true)
                )
                Toast.makeText(requireContext(), "PIN berhasil disimpan! Tombol login PIN kini aktif di halaman login.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "PIN harus tepat 6 angka", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelPin.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showChangePasswordBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_change_password, null)
        dialog.setContentView(sheetView)

        val inputNewPassword = sheetView.findViewById<EditText>(R.id.inputNewPassword)
        val inputConfirmPassword = sheetView.findViewById<EditText>(R.id.inputConfirmPassword)
        val btnToggleNewPassword = sheetView.findViewById<ImageView>(R.id.btnToggleNewPassword)
        val btnToggleConfirmPassword = sheetView.findViewById<ImageView>(R.id.btnToggleConfirmPassword)
        val btnSaveNewPassword = sheetView.findViewById<View>(R.id.btnSaveNewPassword)
        val btnCancelPassword = sheetView.findViewById<View>(R.id.btnCancelPassword)

        var isNewPassVisible = false
        btnToggleNewPassword.setOnClickListener {
            isNewPassVisible = !isNewPassVisible
            val currentTypeface = inputNewPassword.typeface
            if (isNewPassVisible) {
                inputNewPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                btnToggleNewPassword.setImageResource(R.drawable.ic_eye)
                btnToggleNewPassword.setColorFilter(ContextCompat.getColor(requireContext(), R.color.figma_primary))
            } else {
                inputNewPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnToggleNewPassword.setImageResource(R.drawable.ic_eye_off)
                btnToggleNewPassword.setColorFilter(ContextCompat.getColor(requireContext(), R.color.figma_text_muted))
            }
            inputNewPassword.typeface = currentTypeface
            inputNewPassword.text?.let { inputNewPassword.setSelection(it.length) }
        }

        var isConfirmPassVisible = false
        btnToggleConfirmPassword.setOnClickListener {
            isConfirmPassVisible = !isConfirmPassVisible
            val currentTypeface = inputConfirmPassword.typeface
            if (isConfirmPassVisible) {
                inputConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye)
                btnToggleConfirmPassword.setColorFilter(ContextCompat.getColor(requireContext(), R.color.figma_primary))
            } else {
                inputConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off)
                btnToggleConfirmPassword.setColorFilter(ContextCompat.getColor(requireContext(), R.color.figma_text_muted))
            }
            inputConfirmPassword.typeface = currentTypeface
            inputConfirmPassword.text?.let { inputConfirmPassword.setSelection(it.length) }
        }

        btnSaveNewPassword.bounceClick {
            val newPass = inputNewPassword.text.toString().trim()
            val confirmPass = inputConfirmPassword.text.toString().trim()

            if (newPass.length < 8) {
                Toast.makeText(requireContext(), "Kata sandi minimal 8 karakter", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            if (newPass != confirmPass) {
                Toast.makeText(requireContext(), "Konfirmasi kata sandi tidak cocok", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val uid = BackOfficeRepository.getEffectiveUserId(requireContext())
            BackOfficeRepository.firestore.collection("users").document(uid).update(
                mapOf("password" to newPass)
            )

            Toast.makeText(requireContext(), "Kata sandi berhasil diperbarui di Firebase", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnCancelPassword.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAppInfoBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_app_info, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<View>(R.id.btnCloseAppInfo).bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showGuidanceBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_guidance_menu, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<View>(R.id.btnOptionOnboarding).bounceClick {
            dialog.dismiss()
            startActivity(Intent(requireContext(), OnboardingActivity::class.java))
        }

        sheetView.findViewById<View>(R.id.btnOptionSpotlightTour).bounceClick {
            dialog.dismiss()
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation)
            bottomNav?.selectedItemId = R.id.bottom_home
            view?.postDelayed({
                InAppGuidanceManager.startSpotlightTour(requireActivity())
            }, 350)
        }

        sheetView.findViewById<View>(R.id.btnOptionResetFreshStart).bounceClick {
            dialog.dismiss()
            CustomConfirmationDialog.show(
                context = requireContext(),
                title = "Reset Fresh Start",
                message = "Seluruh data preferensi dan status onboarding akan direset untuk pengujian baru.",
                positiveText = "Reset Sekarang",
                negativeText = "Batal",
                iconRes = R.drawable.ic_swap_arrows,
                isDestructive = true,
                onConfirm = {
                    AppStorage.get().resetSessionAndOnboarding()
                    val intent = Intent(requireActivity(), OnboardingActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
            )
        }

        sheetView.findViewById<View>(R.id.btnCloseGuidanceMenu).bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            refreshProfileHeader(it)
            updateGoogleLinkStatusUI(it)
        }
        val storage = AppStorage.get()
        view?.findViewById<TextView>(R.id.textCurrentLanguage)?.text = getLanguageDisplayName(storage.language)
        view?.findViewById<SwitchCompat>(R.id.switchDarkMode)?.isChecked = storage.isDarkMode
        view?.findViewById<SwitchCompat>(R.id.switchBiometrics)?.isChecked = storage.biometricEnabled
    }

    private fun getLanguageDisplayName(langCode: String): String {
        return when (langCode) {
            "en" -> "English (EN)"
            "id" -> "Bahasa Indonesia (ID)"
            else -> "Bahasa Indonesia (ID)"
        }
    }
}
