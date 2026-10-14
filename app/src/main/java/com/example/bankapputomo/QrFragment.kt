package com.example.bankapputomo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class QrFragment : Fragment() {

    private var isFlashOn = false
    private var camera: Camera? = null
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var imgQrGraphic: ImageView
    private lateinit var qrScanLine: View
    private lateinit var textQrAvailableBalance: TextView

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            imgQrGraphic.visibility = View.VISIBLE
            cameraPreviewView.visibility = View.GONE
            Toast.makeText(requireContext(), "Camera permission denied. Tap frame to scan sample QR.", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(requireContext(), "QR Code detected from image!", Toast.LENGTH_SHORT).show()
            simulateQrPayment("STARBUCKS SENAYAN CITY", 65000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_qr, container, false)

        val btnFlashlight = view.findViewById<ImageView>(R.id.btnFlashlight)
        val qrViewfinder = view.findViewById<View>(R.id.qrViewfinder)
        val btnUploadGallery = view.findViewById<View>(R.id.btnUploadGallery)
        val btnManualInput = view.findViewById<View>(R.id.btnManualInput)
        cameraPreviewView = view.findViewById(R.id.cameraPreviewView)
        imgQrGraphic = view.findViewById(R.id.imgQrGraphic)
        qrScanLine = view.findViewById(R.id.qrScanLine)
        textQrAvailableBalance = view.findViewById(R.id.textQrAvailableBalance)

        refreshBalance()

        // Lottie QRIS Reticle Animation
        val lottieReticle = view.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieQrScanReticle)
        if (lottieReticle != null) {
            lottieReticle.setAnimation("anim_qris_scan.json")
            com.example.bankapputomo.util.LottieMotionHelper.recolor(
                lottieReticle,
                ContextCompat.getColor(requireContext(), R.color.figma_primary)
            )
            com.example.bankapputomo.util.LottieMotionHelper.playLoop(lottieReticle)
        }

        // Start scanning line animation
        startScanLineAnimation()

        // Camera permission and startup
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Flashlight toggle
        btnFlashlight.bounceClick {
            isFlashOn = !isFlashOn
            if (camera != null) {
                camera?.cameraControl?.enableTorch(isFlashOn)
            }
            btnFlashlight.setColorFilter(
                ContextCompat.getColor(requireContext(), if (isFlashOn) R.color.figma_warning else R.color.figma_primary)
            )
            Toast.makeText(requireContext(), if (isFlashOn) "Lampu nyala" else "Lampu mati", Toast.LENGTH_SHORT).show()
        }

        // Tap viewfinder to simulate QR scan
        qrViewfinder.bounceClick {
            simulateQrPayment("KOPI KENANGAN GRAND INDONESIA", 38000)
        }

        // Pick from gallery
        btnUploadGallery.bounceClick {
            galleryPickerLauncher.launch("image/*")
        }

        // Manual code input
        btnManualInput.bounceClick {
            val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
            val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_manual_qris, null)
            dialog.setContentView(sheetView)

            val inputCode = sheetView.findViewById<EditText>(R.id.inputManualQrisCode)
            val btnProceed = sheetView.findViewById<View>(R.id.btnProceedManualQris)
            val btnCancel = sheetView.findViewById<View>(R.id.btnCancelManualQris)

            btnProceed.bounceClick {
                val code = inputCode.text.toString().trim()
                if (code.isNotEmpty()) {
                    dialog.dismiss()
                    simulateQrPayment("INDOMARET SUDIRMAN", 42500)
                } else {
                    android.widget.Toast.makeText(requireContext(), "Masukkan kode merchant QRIS", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            btnCancel.bounceClick {
                dialog.dismiss()
            }

            dialog.show()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshBalance()
    }

    private fun refreshBalance() {
        if (::textQrAvailableBalance.isInitialized) {
            textQrAvailableBalance.text = "${IdrFormatter.format(AppStorage.get().debitBalance)} available"
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview)
                imgQrGraphic.visibility = View.GONE
                cameraPreviewView.visibility = View.VISIBLE
            } catch (e: Exception) {
                imgQrGraphic.visibility = View.VISIBLE
                cameraPreviewView.visibility = View.GONE
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startScanLineAnimation() {
        val animation = TranslateAnimation(0f, 0f, 0f, 600f).apply {
            duration = 1600
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        qrScanLine.startAnimation(animation)
    }

    private fun simulateQrPayment(merchantName: String, amount: Long) {
        if (!com.example.bankapputomo.util.CardSecurityHelper.isPrimaryCardActive(requireContext())) {
            com.example.bankapputomo.util.CardSecurityHelper.showCardLockedBottomSheet(requireContext())
            return
        }

        val formatted = IdrFormatter.format(amount)
        com.example.bankapputomo.util.CustomConfirmationDialog.show(
            context = requireContext(),
            title = "Konfirmasi Bayar QRIS",
            message = "Merchant: $merchantName\nTotal: $formatted\nBiaya Layanan: Gratis (Rp 0)\nSumber Dana: Utomo Debit (**** 7890)",
            positiveText = "Bayar $formatted",
            negativeText = "Batal",
            iconRes = R.drawable.ic_qr,
            isDestructive = false
        ) {
            val success = AppStorage.get().addTransaction(
                title = "QRIS: $merchantName",
                category = "QRIS",
                amount = amount,
                isIncome = false,
                counterparty = merchantName
            )
            refreshBalance()

            if (success) {
                ReceiptActivity.start(
                    context = requireContext(),
                    title = "Pembayaran QRIS Merchant",
                    category = "QRIS",
                    amount = amount,
                    fee = 0L,
                    counterparty = merchantName,
                    recipientAccount = "NMID: ID1020021982901",
                    bankName = "QRIS Standar Indonesia",
                    note = "Scan QRIS Merchant",
                    merchantDetails = "ID1020021982901 • Terminal A-01 (ASPI)",
                    origin = "QRIS",
                    customRefNo = "QRIS-${(10000000..99999999).random()}"
                )
            } else {
                Toast.makeText(requireContext(), "Saldo tidak mencukupi untuk pembayaran QRIS", Toast.LENGTH_LONG).show()
            }
        }
    }
}
