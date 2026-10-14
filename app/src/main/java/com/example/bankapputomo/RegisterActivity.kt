package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.example.bankapputomo.util.ViewExtensions.setValidationState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : BaseActivity() {

    private lateinit var containerNama: View
    private lateinit var containerKtp: View
    private lateinit var containerEmail: View
    private lateinit var containerPhone: View
    private lateinit var containerPassword: View

    private lateinit var inputNama: EditText
    private lateinit var inputKtp: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPhone: EditText
    private lateinit var inputPassword: EditText

    private lateinit var iconNamaValid: ImageView
    private lateinit var iconKtpValid: ImageView
    private lateinit var iconEmailValid: ImageView
    private lateinit var iconPhoneValid: ImageView
    private lateinit var iconPasswordValid: ImageView

    private lateinit var textPasswordRequirements: TextView
    private lateinit var registerScrollView: ScrollView

    private var isPasswordVisible: Boolean = false

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    }

    private fun scrollToView(targetView: View) {
        registerScrollView.postDelayed({
            val rect = android.graphics.Rect()
            targetView.getDrawingRect(rect)
            registerScrollView.offsetDescendantRectToMyCoords(targetView, rect)
            val offset = (24 * resources.displayMetrics.density).toInt()
            val targetY = maxOf(0, rect.top - offset)
            registerScrollView.smoothScrollTo(0, targetY)
        }, 150)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        registerScrollView = findViewById<ScrollView>(R.id.registerScrollView)
        containerNama = findViewById(R.id.containerNama)
        containerKtp = findViewById(R.id.containerKtp)
        containerEmail = findViewById(R.id.containerEmail)
        containerPhone = findViewById(R.id.containerPhone)
        containerPassword = findViewById(R.id.containerPassword)

        inputNama = findViewById(R.id.editTextNamaLengkap)
        inputKtp = findViewById(R.id.editTextKtp)
        inputEmail = findViewById(R.id.editTextEmail)
        inputPhone = findViewById(R.id.editTextPhone)
        inputPassword = findViewById(R.id.editTextPassword)

        iconNamaValid = findViewById(R.id.iconNamaValid)
        iconKtpValid = findViewById(R.id.iconKtpValid)
        iconEmailValid = findViewById(R.id.iconEmailValid)
        iconPhoneValid = findViewById(R.id.iconPhoneValid)
        iconPasswordValid = findViewById(R.id.iconPasswordValid)

        textPasswordRequirements = findViewById(R.id.textPasswordRequirements)

        // Smooth scroll focused input fields into view above the keyboard
        inputNama.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) scrollToView(containerNama) }
        inputKtp.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) scrollToView(containerKtp) }
        inputEmail.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) scrollToView(containerEmail) }
        inputPhone.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) scrollToView(containerPhone) }
        inputPassword.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) scrollToView(containerPassword) }

        inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(inputPassword.windowToken, 0)
                inputPassword.clearFocus()
                true
            } else {
                false
            }
        }

        val btnTogglePassword = findViewById<ImageView>(R.id.btnTogglePassword)
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

        val btnRegister = findViewById<TextView>(R.id.buttonRegister)
        val textGoToLogin = findViewById<TextView>(R.id.textGoToLogin)

        // Real-time animated validation
        inputNama.addTextChangedListener { text ->
            val str = text?.toString()?.trim() ?: ""
            if (str.isEmpty()) {
                containerNama.setValidationState(null, iconNamaValid)
            } else if (str.any { it.isDigit() }) {
                containerNama.setValidationState(false, iconNamaValid)
            } else {
                containerNama.setValidationState(true, iconNamaValid)
            }
        }

        inputKtp.addTextChangedListener { text ->
            val str = text?.toString()?.trim() ?: ""
            if (str.isEmpty()) {
                containerKtp.setValidationState(null, iconKtpValid)
            } else if (str.length == 16 && str.all { it.isDigit() }) {
                containerKtp.setValidationState(true, iconKtpValid)
            } else {
                containerKtp.setValidationState(false, iconKtpValid)
            }
        }

        inputEmail.addTextChangedListener { text ->
            val str = text?.toString()?.trim() ?: ""
            if (str.isEmpty()) {
                containerEmail.setValidationState(null, iconEmailValid)
            } else if (str.contains('@') && str.contains('.')) {
                containerEmail.setValidationState(true, iconEmailValid)
            } else {
                containerEmail.setValidationState(false, iconEmailValid)
            }
        }

        inputPhone.addTextChangedListener { text ->
            val str = text?.toString()?.trim() ?: ""
            if (str.isEmpty()) {
                containerPhone.setValidationState(null, iconPhoneValid)
            } else if (str.length >= 10 && str.all { it.isDigit() }) {
                containerPhone.setValidationState(true, iconPhoneValid)
            } else {
                containerPhone.setValidationState(false, iconPhoneValid)
            }
        }

        inputPassword.addTextChangedListener { text ->
            val str = text?.toString() ?: ""
            if (str.isEmpty()) {
                containerPassword.setValidationState(null, iconPasswordValid)
                textPasswordRequirements.setTextColor(ContextCompat.getColor(this, R.color.figma_text_muted))
            } else if (isPasswordValid(str)) {
                containerPassword.setValidationState(true, iconPasswordValid)
                textPasswordRequirements.setTextColor(ContextCompat.getColor(this, R.color.figma_primary))
            } else {
                containerPassword.setValidationState(false, iconPasswordValid)
                textPasswordRequirements.setTextColor(ContextCompat.getColor(this, R.color.bank_red))
            }
        }

        btnRegister.bounceClick {
            val nama = inputNama.text.toString().trim()
            val ktp = inputKtp.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val password = inputPassword.text.toString()

            if (nama.isEmpty() || ktp.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                if (nama.isEmpty()) containerNama.setValidationState(false, iconNamaValid, shakeOnError = true)
                if (ktp.isEmpty()) containerKtp.setValidationState(false, iconKtpValid, shakeOnError = true)
                if (email.isEmpty()) containerEmail.setValidationState(false, iconEmailValid, shakeOnError = true)
                if (phone.isEmpty()) containerPhone.setValidationState(false, iconPhoneValid, shakeOnError = true)
                if (password.isEmpty()) containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_empty), Toast.LENGTH_SHORT).show()
                if (password.isEmpty()) scrollToView(containerPassword)
            } else if (nama.any { it.isDigit() }) {
                containerNama.setValidationState(false, iconNamaValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_name_digits), Toast.LENGTH_SHORT).show()
                scrollToView(containerNama)
            } else if (ktp.length != 16) {
                containerKtp.setValidationState(false, iconKtpValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_ktp_digits), Toast.LENGTH_SHORT).show()
                scrollToView(containerKtp)
            } else if (password.length < 8) {
                containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_password_length), Toast.LENGTH_SHORT).show()
                scrollToView(containerPassword)
            } else if (!password.any { it.isUpperCase() }) {
                containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_password_uppercase), Toast.LENGTH_SHORT).show()
                scrollToView(containerPassword)
            } else if (!password.any { it.isDigit() }) {
                containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_password_digit), Toast.LENGTH_SHORT).show()
                scrollToView(containerPassword)
            } else if (!password.any { !it.isLetterOrDigit() && !it.isWhitespace() }) {
                containerPassword.setValidationState(false, iconPasswordValid, shakeOnError = true)
                Toast.makeText(this, getString(R.string.auth_error_password_symbol), Toast.LENGTH_SHORT).show()
                scrollToView(containerPassword)
            } else {
                btnRegister.isEnabled = false
                btnRegister.alpha = 0.6f

                val storage = AppStorage.get(this)
                if (storage.findRegisteredUserByEmail(email) != null) {
                    btnRegister.isEnabled = true
                    btnRegister.alpha = 1.0f
                    containerEmail.setValidationState(false, iconEmailValid, shakeOnError = true)
                    Toast.makeText(this, "Email ini sudah terdaftar. Silakan login.", Toast.LENGTH_LONG).show()
                    return@bounceClick
                }

                var isDone = false
                val handler = Handler(Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    if (!isDone) {
                        isDone = true
                        performRegistration(nama, ktp, email, phone, password)
                    }
                }
                handler.postDelayed(timeoutRunnable, 2000L)

                // Verify with Firestore if account already exists
                BackOfficeRepository.firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!isDone) {
                            isDone = true
                            handler.removeCallbacks(timeoutRunnable)
                            if (!querySnapshot.isEmpty) {
                                btnRegister.isEnabled = true
                                btnRegister.alpha = 1.0f
                                containerEmail.setValidationState(false, iconEmailValid, shakeOnError = true)
                                Toast.makeText(this, "Email ini sudah terdaftar. Silakan login.", Toast.LENGTH_LONG).show()
                            } else {
                                performRegistration(nama, ktp, email, phone, password)
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (!isDone) {
                            isDone = true
                            handler.removeCallbacks(timeoutRunnable)
                            performRegistration(nama, ktp, email, phone, password)
                        }
                    }
            }
        }

        textGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun performRegistration(nama: String, ktp: String, email: String, phone: String, password: String) {
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val now = Timestamp.now()
                val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                val accountNumber = "8801" + (100000..999999).random().toString()
                // Passwords remain inside Firebase Authentication; they are
                // never copied to Firestore or local preferences.
                val userData = hashMapOf<String, Any>(
                    "id" to uid,
                    "uid" to uid,
                    "name" to nama,
                    "email" to email,
                    "phone" to phone,
                    "ktp" to ktp,
                    "accountNumber" to accountNumber,
                    "status" to "active",
                    "role" to "customer",
                    "googleLinked" to false,
                    "googleEmail" to "",
                    "pinEnabled" to false,
                    "pin" to "",
                    "biometricEnabled" to false,
                    "debitBalance" to 25000000L,
                    "savingsBalance" to 0L,
                    "currencyBalances" to mapOf<String, Double>(),
                    "createdAt" to now,
                    "registeredAt" to nowStr
                )

                BackOfficeRepository.firestore.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        val random15 = (1..15).map { (0..9).random() }.joinToString("")
                        val cardData = hashMapOf<String, Any>(
                            "id" to "card-1",
                            "userId" to uid,
                            "cardNumber" to ("4" + random15).chunked(4).joinToString(" "),
                            "cardHolder" to nama,
                            "expiry" to "12/29",
                            "balance" to 25000000L,
                            "cardType" to "Debit",
                            "gradientType" to 1,
                            "isBlocked" to false,
                            "status" to "active"
                        )
                        BackOfficeRepository.firestore.collection("users").document(uid)
                            .collection("cards").document("card-1").set(cardData)
                            .addOnSuccessListener {
                                BackOfficeRepository.firestore.collection("cards")
                                    .document("${uid}_card-1").set(cardData)
                                completeRegistration(uid, nama, email, phone, accountNumber)
                            }
                            .addOnFailureListener { showRegistrationFailure(it) }
                    }
                    .addOnFailureListener { showRegistrationFailure(it) }
            }
            .addOnFailureListener { showRegistrationFailure(it) }
    }

    private fun showRegistrationFailure(error: Exception) {
        findViewById<View>(R.id.btnRegister)?.let {
            it.isEnabled = true
            it.alpha = 1.0f
        }
        Toast.makeText(this, "Registrasi gagal: ${error.message ?: "periksa koneksi"}", Toast.LENGTH_LONG).show()
    }

    private fun completeRegistration(uid: String, nama: String, email: String, phone: String, accountNumber: String = "8801829301") {
        val storage = AppStorage.get(this)
        storage.switchUser(
            uid = uid,
            name = nama,
            email = email,
            phone = phone,
            isPinSet = false,
            bioEnabled = false,
            isGoogleLinked = false,
            linkedGoogleEmail = "",
            initialDebit = 25000000L,
            initialSavings = 0L,
            pinValue = ""
        )
        // The card document was created in Firestore before entering the app.
        // Keep the in-memory cache empty until the normal hydration path reads it.

        // AppStorage writes only the approved session keys locally. The
        // profile, balances, card, and transaction data remain in Firestore.
        storage.isLoggedIn = true
        storage.loggedInUser = nama
        storage.userEmail = email

        BackOfficeRepository.hydrateUserData(this) {
            Toast.makeText(this, getString(R.string.auth_account_created), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, DashboardActivity::class.java).apply {
                putExtra("nama", nama)
            }
            startActivity(intent)
            finish()
        }
    }
}
