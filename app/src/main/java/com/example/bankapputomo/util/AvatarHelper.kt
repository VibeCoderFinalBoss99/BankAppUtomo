package com.example.bankapputomo.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.example.bankapputomo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File

object AvatarHelper {

    val PRESET_AVATARS = listOf(
        "source_b_avatar" to R.drawable.source_b_avatar,
        "avatar_push" to R.drawable.avatar_push,
        "avatar_olivia" to R.drawable.avatar_olivia,
        "avatar_emma" to R.drawable.avatar_emma,
        "avatar_justin" to R.drawable.avatar_justin,
        "avatar_alexander" to R.drawable.avatar_alexander
    )

    fun getPresetDrawableRes(resName: String): Int {
        return PRESET_AVATARS.firstOrNull { it.first == resName }?.second ?: R.drawable.source_b_avatar
    }

    fun loadAvatar(context: Context, imageView: ImageView) {
        val storage = AppStorage.get(context)
        val customPath = storage.userCustomAvatarUri
        val resName = storage.userAvatarResName
        imageView.setImageResource(getPresetDrawableRes(resName))
        if (customPath.isNotEmpty()) {
            if (customPath.startsWith("https://") || customPath.startsWith("gs://")) {
                try {
                    FirebaseStorage.getInstance().getReferenceFromUrl(customPath)
                        .getBytes(4L * 1024L * 1024L)
                        .addOnSuccessListener { bytes ->
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let(imageView::setImageBitmap)
                        }
                } catch (_: Exception) {
                    // Fall back to the preset if the cloud avatar cannot be loaded.
                }
                return
            }
            val file = File(customPath)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) {
                    imageView.setImageBitmap(bmp)
                    return
                }
            }
        }
    }

    fun saveCustomAvatar(context: Context, uri: Uri): String {
        return try {
            val file = File(context.filesDir, "custom_user_avatar.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val path = file.absolutePath
            path
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun setPresetAvatar(context: Context, resName: String) {
        val storage = AppStorage.get(context)
        storage.userAvatarResName = resName
        storage.userCustomAvatarUri = ""
    }

    fun uploadCustomAvatar(
        context: Context,
        sourceUri: Uri,
        onComplete: (success: Boolean, downloadUrl: String?) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            onComplete(false, null)
            return
        }

        val reference = FirebaseStorage.getInstance()
            .reference
            .child("users/$uid/avatar.jpg")

        reference.putFile(sourceUri)
            .continueWithTask { reference.downloadUrl }
            .addOnSuccessListener { downloadUri ->
                val url = downloadUri.toString()
                val storage = AppStorage.get(context)
                storage.userCustomAvatarUri = url
                storage.userAvatarResName = ""
                BackOfficeRepository.updateUserFields(
                    context,
                    mapOf(
                        "avatar" to "custom",
                        "customAvatarUrl" to url,
                        "customAvatarUri" to url
                    )
                )
                onComplete(true, url)
            }
            .addOnFailureListener {
                onComplete(false, null)
            }
    }
}
