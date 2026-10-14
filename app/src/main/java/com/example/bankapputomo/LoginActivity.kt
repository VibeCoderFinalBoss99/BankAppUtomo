package com.example.bankapputomo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.airbnb.lottie.LottieAnimationView
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.BiometricHelper
import com.example.bankapputomo.util.CardModel
import com.example.bankapputomo.util.FirebaseSync
import com.example.bankapputomo.util.RegisteredUserRecord
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.example.bankapputomo.util.ViewExtensions.setValidationState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot

class LoginActivity : BaseActivity() {

    private lateinit var containerUsername: View
    private lateinit var containerPassword: View
    private lateinit var inputUsername: EditText
    private lateinit var inputPassword: EditText
    private lateinit var iconUsernameValid: ImageView
    private lateinit var iconPasswordValid: ImageView
    private lateinit var btnTogglePassword: ImageView
    private var isPasswordVisible: Boolean = false

    private lateinit var layoutAlternativeAuth: View
    private lateinit var btnPin: View
    private lateinit var btnBiometric: View
    private lateinit var btnLogin: TextView
    private lateinit var btnGoogle: View

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            if (email.isNotEmpty()) {
                val idToken = account.idToken
                if (!idToken.isNullOrBlank()) {
                    FirebaseAuth.getInstance()
                        .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                        .addOnCompleteListener {
                            checkAndLoginGoogleAccount(email)
                        }
                } else {
                    checkAndLoginGoogleAccount(email)
                }
                return@registerForActivityResult
            }
        } catch (e: ApiException) {
            if (e.statusCode == 12501) {
                Toast.makeText(this, "Pemilihan akun Google dibatalkan.", Toast.LENGTH_SHORT).show()
            } else {
                rejectGoogleLogin(googleSignInErrorMessage(e.statusCode))
            }
            return@registerForActivityResult
        } catch (e: Exception) {
            rejectGoogleLogin("Google Sign-In gagal diproses. Periksa Google Play Services dan konfigurasi Firebase.")
            return@registerForActivityResult
        }

        val storage = AppStorage.get(this)
        val linkedUsers = storage.getRegisteredUsers().filter { it.googleLinked && it.googleEmail.isNotEmpty() }
        if (linkedUsers.isNotEmpty()) {
            showLinkedGoogleLoginBottomSheet(linkedUsers)
        } else if (storage.googleLinked && storage.googleEmail.isNotEmpty()) {
            checkAndLoginGoogleAccount(storage.googleEmail)
        } else {
            rejectGoogleLogin("Akun Google ini belum ditautkan ke akun Bank UTOMO Anda. Silakan masuk menggunakan email & kata sandi, lalu tautkan akun Google di menu Profil > Keamanan Akun.")
        }
    }

    override fun onResume() {
        super.onResume()
        setupConditionalAuthOptions()
        checkMaintenanceAndBlock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AppStorage.get().hasSeenOnboarding) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)
        setupLoginAnimation()

        containerUsername = findViewById(R.id.containerUsername)
        containerPassword = findViewById(R.id.containerPassword)
        inputUsername = findViewById(R.id.username)
        inputPassword = findViewById(R.id.password)
        iconUsernameValid = findViewById(R.id.iconUsernameValid)
        iconPasswordValid = findViewById(R.id.iconPasswordValid)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)

        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            val currentTypeface = inputPassword.typeface
            if (isPasswordVisible) {
                inputPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye)
                btnTogglePassword.setColorFilter(ContextCompat.getColor(this, R.color.figma_primary))
            } else {
                inputPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
                btnTogglePassword.setColorFilter(ContextCompat.getColor(this, R.color.figma_text_muted))
            }
            inputPassword.typeface = currentTypeface
            inputPassword.text?.let { inputPassword.setSelection(it.length) }
        }

        layoutAlternativeAuth = findViewById(R.id.layoutAlternativeAuth)
        btnLogin = findViewById(R.id.buttonLogin)
        btnGoogle = findViewById(R.id.btnGoogleSignIn)
        btnPin = findViewById(R.id.btnQuickPin)
        btnBiometric = findViewById(R.id.btnQuickFingerprint)
        val textRegister = findViewById<TextView>(R.id.textGoToRegister)
        val btnForgot = findViewById<View>(R.id.btnForgotPassword)

        setupConditionalAuthOptions()

        // Real-time animated validation
        inputUsername.addTextChangedListener { text ->
            val str = text?.toString()?.trim() ?: ""
            if (str.isEmpty()) {
                containerUsername.setValidationState(null, iconUsernameValid)
            } else if (str.any { it.isDigit() }) {
                containerUsername.setValidationState(false, iconUsernameValid, shakeOnError = false)
            } else {
                containerUsername.setValidationState(true, iconUsernameValid)
            }
        }

        inputPassword.addTextChangedListener { text ->
            val str = text?.toString() ?: ""
            if (str.isEmpty()) {
                containerPassword.setValidationState(null, iconPasswordValid)
            } else if (str.length >= 8) {
                containerPassword.setValidationState(true, iconPasswordValid)
            } else {
                containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = false)
            }
        }

        btnLogin.bounceClick {
            val username = inputUsername.text.toString().trim()
            val password = inputPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                if (username.isEmpty()) containerUsername.setValidationState(false, iconUsernameValid, shakeOnError = true)
                if (password.isEmpty()) containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_empty), Toast.LENGTH_SHORT).show()
            } else if (username.any { it.isDigit() }) {
                containerUsername.setValidationState(false, iconUsernameValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_name_digits), Toast.LENGTH_SHORT).show()
            } else if (password.length < 8) {
                containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_password_length), Toast.LENGTH_SHORT).show()
            } else {
                containerUsername.setValidationState(true, iconUsernameValid)
                containerPassword.setValidationState(true, iconPasswordValid)

                checkMaintenanceAndBlock {
                    performFirebaseCredentialLogin(username, password)
                }
            }
        }

        btnGoogle.bounceClick {
            val gso = buildGoogleSignInOptions()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            try {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            } catch (e: Exception) {
                rejectGoogleLogin("Google Sign-In tidak tersedia di perangkat ini.")
            }
        }

        btnBiometric.bounceClick {
            val storage = AppStorage.get(this)
            if (!storage.biometricEnabled) {
                Toast.makeText(this, "Login biometrik belum diaktifkan.", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            BiometricHelper.showBiometricPrompt(
                activity = this,
                title = getString(R.string.auth_biometric_title),
                subtitle = getString(R.string.auth_biometric_subtitle),
                onSuccess = {
                    val uid = storage.currentUserId
                    if (uid.isNotEmpty()) {
                        verifyFirebaseAccountAndProceed(uid, storage.userName, storage.userEmail)
                    } else {
                        Toast.makeText(this, "Silakan login dengan email & password terlebih dahulu.", Toast.LENGTH_LONG).show()
                    }
                },
                onError = { err ->
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                }
            )
        }

        btnPin.bounceClick {
            val storage = AppStorage.get(this)
            if (!storage.isPinConfigured || storage.userPin.isEmpty()) {
                Toast.makeText(this, "PIN belum disetel pada akun ini.", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            showQuickPinBottomSheet()
        }

        textRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnForgot.setOnClickListener {
            showForgotPasswordBottomSheet()
        }
    }

    private fun setupConditionalAuthOptions() {
        val storage = AppStorage.get(this)
        val canShowPin = storage.isPinConfigured && storage.userPin.isNotEmpty()
        val canShowBio = storage.biometricEnabled

        btnPin.visibility = if (canShowPin) View.VISIBLE else View.GONE
        btnBiometric.visibility = if (canShowBio) View.VISIBLE else View.GONE
        layoutAlternativeAuth.visibility = if (canShowPin || canShowBio) View.VISIBLE else View.GONE
    }

    private fun completeSuccessfulLocalLogin(user: RegisteredUserRecord) {
        val storage = AppStorage.get(this)
        storage.switchUser(
            uid = user.uid,
            name = user.name,
            email = user.email,
            phone = user.phone,
            isPinSet = user.isPinSet,
            bioEnabled = user.biometricEnabled,
            isGoogleLinked = user.googleLinked,
            linkedGoogleEmail = user.googleEmail,
            linkedGoogleName = user.googleName,
            initialDebit = user.debitBalance,
            initialSavings = user.savingsBalance,
            pinValue = user.pin
        )
        // Keep the account's existing card list; initialize only when no cards exist yet.
        storage.initUserCards(user.name, user.debitBalance)
        saveLoginAndProceed(user.name, user.email)
    }

    private fun validateLocalUser(user: RegisteredUserRecord, passwordEntered: String) {
        btnLogin.isEnabled = true
        btnLogin.alpha = 1.0f

        if (user.status.equals("suspended", ignoreCase = true)) {
            val reason = if (user.statusReason.isNotEmpty()) user.statusReason else "Akun dinonaktifkan oleh administrator."
            MaterialAlertDialogBuilder(this)
                .setTitle("Akun Ditangguhkan")
                .setMessage("Akun Anda ditangguhkan: $reason")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        if (user.password.isNotEmpty() && user.password != passwordEntered) {
            containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = true)
            Toast.makeText(this, "Kata sandi salah. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
            return
        }

        completeSuccessfulLocalLogin(user)
    }

    private fun processLocalGoogleUser(user: RegisteredUserRecord) {
        if (!user.googleLinked) {
            rejectGoogleLogin("Akun Google ini belum ditautkan ke akun Bank UTOMO Anda. Silakan masuk menggunakan email & kata sandi, lalu tautkan akun Google di menu Profil > Keamanan Akun.")
            return
        }
        if (user.status.equals("suspended", ignoreCase = true)) {
            val reason = if (user.statusReason.isNotEmpty()) user.statusReason else "Akun dinonaktifkan."
            rejectGoogleLogin("Akun Anda sedang ditangguhkan: $reason")
            return
        }
        Toast.makeText(this, "Login Google Berhasil!", Toast.LENGTH_SHORT).show()
        completeSuccessfulLocalLogin(user)
    }

    private fun performFirebaseCredentialLogin(identifier: String, passwordEntered: String) {
        btnLogin.isEnabled = false
        btnLogin.alpha = 0.6f

        val targetEmail = if (identifier.contains("@")) identifier else "$identifier@bankutomo.co.id"
        val docId = "user_" + targetEmail.replace(Regex("[^a-zA-Z0-9]"), "_")

        val storage = AppStorage.get(this)
        var loginHandled = false
        val loginHandler = Handler(Looper.getMainLooper())
        val loginTimeout = Runnable {
            if (!loginHandled) {
                loginHandled = true
                btnLogin.isEnabled = true
                btnLogin.alpha = 1.0f
                val localUser = storage.findRegisteredUserByEmail(targetEmail)
                if (localUser != null) {
                    validateLocalUser(localUser, passwordEntered)
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Akun Tidak Ditemukan")
                        .setMessage("Akun Anda belum terdaftar di Firebase. Silakan lakukan registrasi terlebih dahulu.")
                        .setPositiveButton("Daftar Sekarang") { _, _ ->
                            startActivity(Intent(this, RegisterActivity::class.java))
                        }
                        .setNegativeButton("Tutup", null)
                        .show()
                }
            }
        }
        loginHandler.postDelayed(loginTimeout, 2000L)

        // First attempt to query by email in users collection
        BackOfficeRepository.firestore.collection("users")
            .whereEqualTo("email", targetEmail)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!loginHandled) {
                    if (!snapshot.isEmpty) {
                        loginHandled = true
                        loginHandler.removeCallbacks(loginTimeout)
                        validateUserDocument(snapshot.documents[0], passwordEntered)
                    } else {
                        // Try doc ID directly
                        BackOfficeRepository.firestore.collection("users").document(docId).get()
                            .addOnSuccessListener { doc ->
                                if (!loginHandled) {
                                    loginHandled = true
                                    loginHandler.removeCallbacks(loginTimeout)
                                    if (doc.exists()) {
                                        validateUserDocument(doc, passwordEntered)
                                    } else {
                                        val localUser = storage.findRegisteredUserByEmail(targetEmail)
                                        if (localUser != null) {
                                            validateLocalUser(localUser, passwordEntered)
                                        } else {
                                            btnLogin.isEnabled = true
                                            btnLogin.alpha = 1.0f
                                            MaterialAlertDialogBuilder(this)
                                                .setTitle("Akun Tidak Ditemukan")
                                                .setMessage("Akun Anda belum terdaftar di Firebase. Silakan lakukan registrasi terlebih dahulu.")
                                                .setPositiveButton("Daftar Sekarang") { _, _ ->
                                                    startActivity(Intent(this, RegisterActivity::class.java))
                                                }
                                                .setNegativeButton("Tutup", null)
                                                .show()
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                if (!loginHandled) {
                                    loginHandled = true
                                    loginHandler.removeCallbacks(loginTimeout)
                                    val localUser = storage.findRegisteredUserByEmail(targetEmail)
                                    if (localUser != null) {
                                        validateLocalUser(localUser, passwordEntered)
                                    } else {
                                        btnLogin.isEnabled = true
                                        btnLogin.alpha = 1.0f
                                        Toast.makeText(this, "Gagal menghubungi Firebase. Periksa koneksi internet.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                    }
                }
            }
            .addOnFailureListener {
                if (!loginHandled) {
                    loginHandled = true
                    loginHandler.removeCallbacks(loginTimeout)
                    val localUser = storage.findRegisteredUserByEmail(targetEmail)
                    if (localUser != null) {
                        validateLocalUser(localUser, passwordEntered)
                    } else {
                        btnLogin.isEnabled = true
                        btnLogin.alpha = 1.0f
                        Toast.makeText(this, "Gagal menghubungi Firebase. Periksa koneksi internet.", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun validateUserDocument(doc: DocumentSnapshot, passwordEntered: String) {
        btnLogin.isEnabled = true
        btnLogin.alpha = 1.0f

        val status = doc.getString("status") ?: "active"
        if (status.equals("suspended", ignoreCase = true)) {
            val reason = doc.getString("statusReason") ?: "Akun dinonaktifkan oleh administrator."
            MaterialAlertDialogBuilder(this)
                .setTitle("Akun Ditangguhkan")
                .setMessage("Akun Anda ditangguhkan: $reason")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val storedPassword = doc.getString("password")
        if (storedPassword != null && storedPassword != passwordEntered) {
            containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = true)
            Toast.makeText(this, "Kata sandi salah. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
            return
        }

        establishFirebaseSession(doc, passwordEntered)
    }

    private fun establishFirebaseSession(doc: DocumentSnapshot, passwordEntered: String) {
        val email = doc.getString("email")?.trim().orEmpty()
        if (email.isEmpty()) {
            completeSuccessfulLogin(doc)
            return
        }

        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        if (currentUser?.email.equals(email, ignoreCase = true)) {
            completeSuccessfulLogin(doc)
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, passwordEntered)
            .addOnSuccessListener {
                completeSuccessfulLogin(doc)
            }
            .addOnFailureListener {
                // Older accounts may exist in Firestore but not in Firebase Auth yet.
                // Create the Auth identity so Firestore security rules can identify it.
                firebaseAuth.createUserWithEmailAndPassword(email, passwordEntered)
                    .addOnSuccessListener {
                        completeSuccessfulLogin(doc)
                    }
                    .addOnFailureListener {
                        // Keep the existing prototype login flow even if Auth provisioning
                        // is unavailable; Firestore writes will be skipped until Auth exists.
                        completeSuccessfulLogin(doc)
                    }
            }
    }

    private fun buildGoogleSignInOptions(): GoogleSignInOptions {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        val clientIdRes = resources.getIdentifier("default_web_client_id", "string", packageName)
        if (clientIdRes != 0) {
            val clientId = getString(clientIdRes)
            if (clientId.isNotBlank()) {
                builder.requestIdToken(clientId)
            }
        }
        return builder.build()
    }

    private fun googleSignInErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            10 -> "Konfigurasi Google Sign-In Firebase belum cocok. Tambahkan SHA-1 dan SHA-256 aplikasi ini di Firebase, aktifkan provider Google, lalu unduh ulang google-services.json."
            7 -> "Google Sign-In tidak dapat terhubung. Periksa koneksi internet."
            12501 -> "Pemilihan akun Google dibatalkan."
            else -> "Google Sign-In gagal (kode $statusCode). Periksa Google Play Services dan konfigurasi Firebase."
        }
    }

    private fun checkAndLoginGoogleAccount(googleEmail: String) {
        val storage = AppStorage.get(this)
        var googleHandled = false
        val gHandler = Handler(Looper.getMainLooper())
        val gTimeout = Runnable {
            if (!googleHandled) {
                googleHandled = true
                val localUser = storage.findRegisteredUserByGoogleEmail(googleEmail)
                    ?: storage.findRegisteredUserByEmail(googleEmail)
                if (localUser != null) {
                    processLocalGoogleUser(localUser)
                } else {
                    rejectGoogleLogin("Akun Google ($googleEmail) belum terdaftar di Firebase. Silakan registrasi terlebih dahulu.")
                }
            }
        }
        gHandler.postDelayed(gTimeout, 2000L)

        // Query users collection for this googleEmail
        BackOfficeRepository.firestore.collection("users")
            .whereEqualTo("googleEmail", googleEmail)
            .get()
            .addOnSuccessListener { snap ->
                if (!googleHandled) {
                    if (!snap.isEmpty) {
                        googleHandled = true
                        gHandler.removeCallbacks(gTimeout)
                        processGoogleDoc(snap.documents[0], googleEmail)
                    } else {
                        // Check by email field
                        BackOfficeRepository.firestore.collection("users")
                            .whereEqualTo("email", googleEmail)
                            .get()
                            .addOnSuccessListener { snap2 ->
                                if (!googleHandled) {
                                    googleHandled = true
                                    gHandler.removeCallbacks(gTimeout)
                                    if (!snap2.isEmpty) {
                                        processGoogleDoc(snap2.documents[0], googleEmail)
                                    } else {
                                        val localUser = storage.findRegisteredUserByGoogleEmail(googleEmail)
                                            ?: storage.findRegisteredUserByEmail(googleEmail)
                                        if (localUser != null) {
                                            processLocalGoogleUser(localUser)
                                        } else {
                                            rejectGoogleLogin("Akun Google ($googleEmail) belum terdaftar di Firebase. Silakan registrasi terlebih dahulu.")
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                if (!googleHandled) {
                                    googleHandled = true
                                    gHandler.removeCallbacks(gTimeout)
                                    val localUser = storage.findRegisteredUserByGoogleEmail(googleEmail)
                                        ?: storage.findRegisteredUserByEmail(googleEmail)
                                    if (localUser != null) {
                                        processLocalGoogleUser(localUser)
                                    } else {
                                        rejectGoogleLogin("Gagal memverifikasi akun Google di Firebase.")
                                    }
                                }
                            }
                    }
                }
            }
            .addOnFailureListener {
                if (!googleHandled) {
                    googleHandled = true
                    gHandler.removeCallbacks(gTimeout)
                    val localUser = storage.findRegisteredUserByGoogleEmail(googleEmail)
                        ?: storage.findRegisteredUserByEmail(googleEmail)
                    if (localUser != null) {
                        processLocalGoogleUser(localUser)
                    } else {
                        rejectGoogleLogin("Gagal memverifikasi akun Google di Firebase.")
                    }
                }
            }
    }

    private fun processGoogleDoc(doc: DocumentSnapshot, googleEmail: String) {
        val googleLinked = doc.getBoolean("googleLinked") ?: false
        if (!googleLinked) {
            val registeredEmail = doc.getString("email").orEmpty()
            if (!registeredEmail.equals(googleEmail, ignoreCase = true)) {
                rejectGoogleLogin("Akun Google ini belum ditautkan ke akun Bank UTOMO Anda. Silakan masuk menggunakan email & kata sandi, lalu tautkan akun Google di menu Profil > Keamanan Akun.")
                return
            }
            BackOfficeRepository.firestore.collection("users").document(doc.id).set(
                mapOf(
                    "googleLinked" to true,
                    "googleEmail" to googleEmail,
                    "googleName" to ""
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }

        val status = doc.getString("status") ?: "active"
        if (status.equals("suspended", ignoreCase = true)) {
            val reason = doc.getString("statusReason") ?: "Akun dinonaktifkan."
            rejectGoogleLogin("Akun Anda sedang ditangguhkan: $reason")
            return
        }

        Toast.makeText(this, "Login Google Berhasil!", Toast.LENGTH_SHORT).show()
        completeSuccessfulLogin(doc)
        if (!googleLinked) {
            val storage = AppStorage.get(this)
            storage.googleLinked = true
            storage.googleEmail = googleEmail
        }
    }

    private fun rejectGoogleLogin(message: String) {
        try {
            val gso = buildGoogleSignInOptions()
            GoogleSignIn.getClient(this, gso).signOut()
        } catch (e: Exception) {
            // Ignore sign-out exceptions
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Autentikasi Google Ditolak")
            .setMessage(message)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun showLinkedGoogleLoginBottomSheet(linkedUsers: List<RegisteredUserRecord>) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_linked_google_login, null)
        dialog.setContentView(sheetView)

        val container = sheetView.findViewById<LinearLayout>(R.id.containerLinkedGoogleAccounts)
        val btnCancel = sheetView.findViewById<View>(R.id.btnCancelGoogleLogin)

        btnCancel.bounceClick {
            dialog.dismiss()
        }

        for (user in linkedUsers) {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 24)
                }
                setBackgroundResource(R.drawable.bg_figma_input)
                setPadding(40, 36, 40, 36)
                gravity = android.view.Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
            }

            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(70, 70).apply {
                    marginEnd = 32
                }
                setImageResource(R.drawable.ic_google)
            }

            val textContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
                )
            }

            val titleView = TextView(this).apply {
                text = user.googleName.ifEmpty { user.name }
                setTextColor(ContextCompat.getColor(this@LoginActivity, R.color.figma_text_title))
                textSize = 14f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@LoginActivity, R.font.poppins_semibold)
            }

            val emailView = TextView(this).apply {
                text = user.googleEmail
                setTextColor(ContextCompat.getColor(this@LoginActivity, R.color.figma_text_muted))
                textSize = 12f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@LoginActivity, R.font.poppins_regular)
            }

            val badge = TextView(this).apply {
                text = "Masuk"
                setBackgroundResource(R.drawable.bg_figma_button_primary)
                setTextColor(android.graphics.Color.WHITE)
                textSize = 11.5f
                setPadding(32, 14, 32, 14)
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@LoginActivity, R.font.poppins_semibold)
            }

            textContainer.addView(titleView)
            textContainer.addView(emailView)
            itemLayout.addView(icon)
            itemLayout.addView(textContainer)
            itemLayout.addView(badge)

            itemLayout.bounceClick {
                dialog.dismiss()
                processLocalGoogleUser(user)
            }

            container.addView(itemLayout)
        }

        dialog.show()
    }

    private fun verifyFirebaseAccountAndProceed(uid: String, fallbackName: String, fallbackEmail: String) {
        val storage = AppStorage.get(this)
        var handled = false
        val handler = Handler(Looper.getMainLooper())
        val timeout = Runnable {
            if (!handled) {
                handled = true
                val localUser = storage.findRegisteredUserByEmail(fallbackEmail)
                if (localUser != null) {
                    completeSuccessfulLocalLogin(localUser)
                } else {
                    saveLoginAndProceed(fallbackName, fallbackEmail)
                }
            }
        }
        handler.postDelayed(timeout, 2000L)

        BackOfficeRepository.firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!handled) {
                    handled = true
                    handler.removeCallbacks(timeout)
                    if (doc.exists()) {
                        val status = doc.getString("status") ?: "active"
                        if (status.equals("suspended", ignoreCase = true)) {
                            Toast.makeText(this, "Akun Anda sedang ditangguhkan.", Toast.LENGTH_LONG).show()
                        } else {
                            completeSuccessfulLogin(doc)
                        }
                    } else {
                        Toast.makeText(this, "Akun tidak ditemukan di Firebase. Silakan login dengan email & sandi.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                if (!handled) {
                    handled = true
                    handler.removeCallbacks(timeout)
                    val localUser = storage.findRegisteredUserByEmail(fallbackEmail)
                    if (localUser != null) {
                        completeSuccessfulLocalLogin(localUser)
                    } else {
                        saveLoginAndProceed(fallbackName, fallbackEmail)
                    }
                }
            }
    }

    private fun completeSuccessfulLogin(doc: DocumentSnapshot) {
        val uid = doc.id
        val realName = doc.getString("name") ?: "Nasabah"
        val email = doc.getString("email") ?: ""
        val phone = doc.getString("phone") ?: ""
        val pinEnabled = doc.getBoolean("pinEnabled") ?: false
        val pin = doc.getString("pin") ?: ""
        val bioEnabled = doc.getBoolean("biometricEnabled") ?: false
        val googleLinked = doc.getBoolean("googleLinked") ?: false
        val googleEmail = doc.getString("googleEmail") ?: ""
        val googleName = doc.getString("googleName") ?: ""
        // A missing cloud balance must not silently recreate the prototype's
        // 25M default. Registration writes the initial value explicitly.
        val debitBal = doc.getLong("debitBalance") ?: 0L
        val savingsBal = doc.getLong("savingsBalance") ?: 0L

        val storage = AppStorage.get(this)
        storage.switchUser(
            uid = uid,
            name = realName,
            email = email,
            phone = phone,
            isPinSet = pinEnabled,
            bioEnabled = bioEnabled,
            isGoogleLinked = googleLinked,
            linkedGoogleEmail = googleEmail,
            linkedGoogleName = googleName,
            initialDebit = debitBal,
            initialSavings = savingsBal,
            pinValue = pin
        )

        // Do not open the dashboard until Firestore has hydrated all business
        // data. SharedPreferences is intentionally limited to session state,
        // theme and language, so reopening the app cannot restore stale defaults.
        BackOfficeRepository.hydrateUserData(this) {
            saveLoginAndProceed(realName, email)
        }
    }

    private fun restoreCardsFromDocuments(
        documents: List<DocumentSnapshot>,
        realName: String,
        fallbackDebitBalance: Long,
        storage: AppStorage,
        hadLocalDebitBalance: Boolean
    ) {
        val cardsList = documents.map { cDoc ->
            CardModel(
                id = cDoc.getString("id") ?: cDoc.id,
                cardNumber = cDoc.getString("cardNumber") ?: "4756 8821 3490 8018",
                cardHolder = cDoc.getString("cardHolder") ?: realName,
                expiry = cDoc.getString("expiry") ?: "12/29",
                balance = cDoc.getLong("balance") ?: fallbackDebitBalance,
                cardType = cDoc.getString("cardType") ?: "Debit",
                gradientType = (cDoc.getLong("gradientType") ?: 1L).toInt(),
                isBlocked = cDoc.getBoolean("isBlocked") ?: false,
                status = cDoc.getString("status") ?: "active"
            )
        }

        // On a fresh install the card record is the most reliable balance
        // snapshot. Keep an existing local user balance intact.
        if (!hadLocalDebitBalance) {
            cardsList.firstOrNull { it.cardType.equals("Debit", ignoreCase = true) }
                ?.balance
                ?.let { storage.debitBalance = it }
        }
        storage.saveCards(cardsList)
    }

    private fun showQuickPinBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_quick_pin_login, null)
        dialog.setContentView(sheetView)

        val inputLoginPin = sheetView.findViewById<EditText>(R.id.inputLoginPin)
        val btnLoginWithPin = sheetView.findViewById<View>(R.id.btnLoginWithPin)
        val btnCancelLoginPin = sheetView.findViewById<View>(R.id.btnCancelLoginPin)

        val storage = AppStorage.get(this)

        btnLoginWithPin.bounceClick {
            val pin = inputLoginPin.text.toString().trim()
            if (pin.length != 6) {
                Toast.makeText(this, "PIN harus 6 angka", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            if (pin == storage.userPin) {
                dialog.dismiss()
                val uid = storage.currentUserId
                if (uid.isNotEmpty()) {
                    verifyFirebaseAccountAndProceed(uid, storage.userName, storage.userEmail)
                } else {
                    saveLoginAndProceed(storage.userName, storage.userEmail)
                }
            } else {
                Toast.makeText(this, "PIN salah. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelLoginPin.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showForgotPasswordBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_forgot_password, null)
        dialog.setContentView(sheetView)

        val inputForgotIdentifier = sheetView.findViewById<EditText>(R.id.inputForgotIdentifier)
        val btnSendForgotOtp = sheetView.findViewById<View>(R.id.btnSendForgotOtp)
        val btnCancelForgot = sheetView.findViewById<View>(R.id.btnCancelForgot)

        btnSendForgotOtp.bounceClick {
            val identifier = inputForgotIdentifier.text.toString().trim()
            if (identifier.isEmpty()) {
                Toast.makeText(this, "Masukkan email atau nomor ponsel Anda", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            dialog.dismiss()
            Toast.makeText(this, "Kode OTP verifikasi telah dikirim ke $identifier", Toast.LENGTH_LONG).show()
        }

        btnCancelForgot.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveLoginAndProceed(username: String, email: String) {
        val storage = AppStorage.get(this)
        storage.isLoggedIn = true
        storage.loggedInUser = username
        storage.userEmail = email

        val currentTime = com.google.firebase.firestore.FieldValue.serverTimestamp()
        val deviceName = android.os.Build.MODEL
        BackOfficeRepository.updateUserFields(this, mapOf(
            "name" to username,
            "email" to email,
            "lastLogin" to currentTime,
            "device" to deviceName
        ))

        Toast.makeText(this, "Selamat datang, $username!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, DashboardActivity::class.java).apply {
            putExtra("nama", username)
        }
        startActivity(intent)
        finish()
    }

    private fun setupLoginAnimation() {
        val container = findViewById<FrameLayout>(R.id.loginAnimationContainer) ?: return
        val preview = findViewById<ImageView>(R.id.loginAnimationPreview)
        val animationView = LottieAnimationView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setAnimation(R.raw.login_animation)
            repeatCount = com.airbnb.lottie.LottieDrawable.INFINITE
        }

        container.addView(animationView)
        animationView.addAnimatorListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {
                preview?.visibility = View.GONE
            }
            override fun onAnimationEnd(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
        animationView.playAnimation()
    }

    private fun checkMaintenanceAndBlock(onProceed: (() -> Unit)? = null) {
        try {
            com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseSync.FIREBASE_DATABASE_URL)
                .getReference("config/maintenance")
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        if (snapshot.exists()) {
                            val enabled = snapshot.child("enabled").getValue(Boolean::class.java) ?: false
                            if (enabled) {
                                val lang = AppStorage.get(this@LoginActivity).language
                                val title = snapshot.child("title").child(lang).getValue(String::class.java)
                                    ?: snapshot.child("title").child("id").getValue(String::class.java)
                                    ?: "Pemeliharaan Sistem"
                                val msg = snapshot.child("message").child(lang).getValue(String::class.java)
                                    ?: snapshot.child("message").child("id").getValue(String::class.java)
                                    ?: "Sistem perbankan sedang dalam peningkatan layanan berkala demi kenyamanan transaksi Anda."
                                val intent = Intent(this@LoginActivity, MaintenanceActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    putExtra("title", title)
                                    putExtra("message", msg)
                                }
                                startActivity(intent)
                                finish()
                                return
                            }
                        }
                        onProceed?.invoke()
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        onProceed?.invoke()
                    }
                })
        } catch (e: Exception) {
            onProceed?.invoke()
        }
    }
}
