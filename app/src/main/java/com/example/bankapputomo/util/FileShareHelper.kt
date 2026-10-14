package com.example.bankapputomo.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileShareHelper {

    /**
     * Renders any Android View hierarchy to a Bitmap with its current measurement.
     */
    fun captureViewToBitmap(view: View): Bitmap {
        val width = if (view.width > 0) view.width else view.measuredWidth
        val height = if (view.height > 0) view.height else view.measuredHeight

        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        view.draw(canvas)
        return bitmap
    }

    /**
     * Shares a rendered bitmap as a PNG through Android's system share sheet using FileProvider.
     */
    fun shareBitmap(activity: Activity, bitmap: Bitmap, chooserTitle: String = "Bagikan Bukti Transaksi") {
        try {
            val imagesFolder = File(activity.cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "BankUtomo_TRX_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Bukti Transaksi UTOMO Bank")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Gagal membagikan gambar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves a rendered bitmap to the user's gallery in Pictures/BankUtomo.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filenamePrefix: String = "BankUtomo_Receipt"): Boolean {
        return try {
            val filename = "${filenamePrefix}_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BankUtomo")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            Toast.makeText(context, "Berhasil disimpan ke Galeri (Pictures/BankUtomo)", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal menyimpan ke galeri: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Generates a PDF of the receipt view using Android's native PdfDocument and shares it.
     */
    fun shareViewAsPdf(activity: Activity, view: View, chooserTitle: String = "Bagikan Bukti PDF") {
        try {
            val width = if (view.width > 0) view.width else view.measuredWidth
            val height = if (view.height > 0) view.height else view.measuredHeight

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(width.coerceAtLeast(1), height.coerceAtLeast(1), 1).create()
            val page = pdfDocument.startPage(pageInfo)

            view.draw(page.canvas)
            pdfDocument.finishPage(page)

            val docsFolder = File(activity.cacheDir, "docs")
            docsFolder.mkdirs()
            val file = File(docsFolder, "BankUtomo_Receipt_${System.currentTimeMillis()}.pdf")

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Bukti Transaksi Resmi UTOMO Bank (PDF)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Gagal membuat PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
