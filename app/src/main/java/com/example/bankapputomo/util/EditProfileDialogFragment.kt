package com.example.bankapputomo.util

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.bankapputomo.R
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class EditProfileDialogFragment : BottomSheetDialogFragment() {

    var onProfileUpdated: (() -> Unit)? = null

    private var selectedPresetName: String = ""
    private var selectedCustomPath: String = ""
    private var selectedCustomUri: android.net.Uri? = null

    private var previewImage: ImageView? = null
    private val presetWrappers = mutableListOf<FrameLayout>()

    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = AvatarHelper.saveCustomAvatar(requireContext(), uri)
            if (savedPath.isNotEmpty()) {
                selectedCustomPath = savedPath
                selectedCustomUri = uri
                selectedPresetName = ""
                val bmp = BitmapFactory.decodeFile(savedPath)
                if (bmp != null) {
                    previewImage?.setImageBitmap(bmp)
                }
                clearPresetSelectionHighlight()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val storage = AppStorage.get(requireContext())
        previewImage = view.findViewById(R.id.imgEditAvatarPreview)

        // Load current avatar
        AvatarHelper.loadAvatar(requireContext(), previewImage!!)
        selectedCustomPath = storage.userCustomAvatarUri
        if (selectedCustomPath.startsWith("https://") || selectedCustomPath.startsWith("gs://")) {
            selectedCustomPath = ""
        }
        selectedPresetName = storage.userAvatarResName

        val inputName = view.findViewById<EditText>(R.id.inputEditName)
        val inputEmail = view.findViewById<EditText>(R.id.inputEditEmail)
        val inputPhone = view.findViewById<EditText>(R.id.inputEditPhone)

        inputName.setText(storage.userName.ifEmpty { "Nasabah" })
        inputEmail.setText(storage.userEmail.ifEmpty { "" })
        inputPhone.setText(storage.userPhone.ifEmpty { "" })

        // Camera badge & Gallery button
        view.findViewById<View>(R.id.btnBadgePickPhoto).bounceClick {
            galleryPickerLauncher.launch("image/*")
        }
        view.findViewById<View>(R.id.btnPickFromGallery).bounceClick {
            galleryPickerLauncher.launch("image/*")
        }

        // Preset wrappers
        val presets = AvatarHelper.PRESET_AVATARS
        val wrapIds = listOf(
            R.id.presetWrap1,
            R.id.presetWrap2,
            R.id.presetWrap3,
            R.id.presetWrap4,
            R.id.presetWrap5,
            R.id.presetWrap6
        )

        presetWrappers.clear()
        wrapIds.forEachIndexed { index, wrapId ->
            val wrap = view.findViewById<FrameLayout>(wrapId)
            presetWrappers.add(wrap)
            if (index < presets.size) {
                val (name, resId) = presets[index]
                wrap.bounceClick {
                    selectedPresetName = name
                    selectedCustomPath = ""
                    selectedCustomUri = null
                    previewImage?.setImageResource(resId)
                    updatePresetHighlight(index)
                }
            }
        }

        // Highlight preset if currently active
        if (selectedCustomPath.isEmpty() && selectedPresetName.isNotEmpty()) {
            val currentIndex = presets.indexOfFirst { it.first == selectedPresetName }
            if (currentIndex >= 0) {
                updatePresetHighlight(currentIndex)
            } else {
                clearPresetSelectionHighlight()
            }
        } else {
            clearPresetSelectionHighlight()
        }

        // Save Button
        view.findViewById<View>(R.id.btnSaveProfile).bounceClick {
            val newName = inputName.text.toString().trim()
            val newEmail = inputEmail.text.toString().trim()
            val newPhone = inputPhone.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_name_empty), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                Toast.makeText(requireContext(), getString(R.string.error_email_invalid), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val cleanPhone = newPhone.replace(Regex("[^0-9+]"), "")
            if (cleanPhone.length < 9) {
                Toast.makeText(requireContext(), getString(R.string.error_phone_invalid), Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            // Commit the profile fields. Business data remains cloud-backed.
            storage.userName = newName
            storage.userEmail = newEmail
            storage.userPhone = newPhone

            val cards = storage.getCards()
            cards.forEach { it.cardHolder = newName }
            storage.saveCards(cards)

            if (selectedPresetName.isNotEmpty() && selectedCustomUri == null) {
                storage.userAvatarResName = selectedPresetName
                storage.userCustomAvatarUri = ""
            }

            val saveProfile = {
                BackOfficeRepository.updateUserFields(
                    requireContext(),
                    mapOf(
                        "name" to newName,
                        "email" to newEmail,
                        "phone" to newPhone,
                        "avatar" to if (selectedCustomUri != null || storage.userCustomAvatarUri.isNotBlank()) {
                            "custom"
                        } else {
                            storage.userAvatarResName
                        },
                        "customAvatarUrl" to storage.userCustomAvatarUri
                    )
                )
                Toast.makeText(requireContext(), getString(R.string.profile_updated_toast), Toast.LENGTH_SHORT).show()
                onProfileUpdated?.invoke()
                dismiss()
            }

            val customUri = selectedCustomUri
            if (customUri != null) {
                Toast.makeText(requireContext(), "Menyimpan foto profil...", Toast.LENGTH_SHORT).show()
                AvatarHelper.uploadCustomAvatar(requireContext(), customUri) { success, _ ->
                    if (success) {
                        saveProfile()
                    } else {
                        Toast.makeText(requireContext(), "Foto profil gagal disimpan ke Firebase.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                saveProfile()
            }
        }

        // Cancel Button
        view.findViewById<View>(R.id.btnCancelProfile).bounceClick {
            dismiss()
        }
    }

    private fun updatePresetHighlight(selectedIndex: Int) {
        presetWrappers.forEachIndexed { i, wrap ->
            if (i == selectedIndex) {
                wrap.setBackgroundResource(R.drawable.bg_avatar_ring_selected)
            } else {
                wrap.background = null
            }
        }
    }

    private fun clearPresetSelectionHighlight() {
        presetWrappers.forEach { it.background = null }
    }
}
