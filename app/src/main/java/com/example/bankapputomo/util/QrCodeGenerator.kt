package com.example.bankapputomo.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object QrCodeGenerator {

    /**
     * Generates a realistic, professional QRIS-style QR Code Bitmap purely in Kotlin
     * without any third-party dependencies.
     */
    fun generateQrBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512,
        darkColor: Int = Color.parseColor("#12233D"),
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val size = 33 // 33x33 matrix (Version 4 QR size)
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // Helper to mark finder pattern (7x7)
        fun setFinder(r: Int, c: Int) {
            for (i in 0 until 7) {
                for (j in 0 until 7) {
                    val row = r + i
                    val col = c + j
                    reserved[row][col] = true
                    if (i == 0 || i == 6 || j == 0 || j == 6 || (i in 2..4 && j in 2..4)) {
                        matrix[row][col] = true
                    }
                }
            }
            // Add separator (1 module border around finder)
            for (i in -1..7) {
                for (j in -1..7) {
                    val row = r + i
                    val col = c + j
                    if (row in 0 until size && col in 0 until size && !reserved[row][col]) {
                        reserved[row][col] = true
                        matrix[row][col] = false
                    }
                }
            }
        }

        // 1. Finder patterns at Top-Left, Top-Right, Bottom-Left
        setFinder(0, 0)
        setFinder(0, size - 7)
        setFinder(size - 7, 0)

        // 2. Alignment pattern at (size-9, size-9) -> (24, 24)
        val alignR = 24
        val alignC = 24
        for (i in -2..2) {
            for (j in -2..2) {
                val row = alignR + i
                val col = alignC + j
                reserved[row][col] = true
                if (kotlin.math.abs(i) == 2 || kotlin.math.abs(j) == 2 || (i == 0 && j == 0)) {
                    matrix[row][col] = true
                }
            }
        }

        // 3. Timing patterns on row 6 and col 6
        for (i in 8 until size - 8) {
            reserved[6][i] = true
            reserved[i][6] = true
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // 4. Dark module
        reserved[size - 8][8] = true
        matrix[size - 8][8] = true

        // 5. Fill remaining data modules deterministically using SHA-256 of payload
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray(StandardCharsets.UTF_8))
        var byteIndex = 0
        var bitIndex = 0

        for (c in size - 1 downTo 0 step 2) {
            val col1 = c
            val col2 = c - 1
            for (r in 0 until size) {
                val row = if ((c / 2) % 2 == 0) r else size - 1 - r
                if (col1 >= 0 && !reserved[row][col1]) {
                    val bit = ((hash[byteIndex % hash.size].toInt() ushr (bitIndex % 8)) and 1) == 1
                    matrix[row][col1] = bit xor ((row + col1) % 2 == 0) // Standard checkerboard mask
                    bitIndex++
                    if (bitIndex % 8 == 0) byteIndex++
                }
                if (col2 >= 0 && !reserved[row][col2]) {
                    val bit = ((hash[byteIndex % hash.size].toInt() ushr (bitIndex % 8)) and 1) == 1
                    matrix[row][col2] = bit xor ((row + col2) % 3 == 0)
                    bitIndex++
                    if (bitIndex % 8 == 0) byteIndex++
                }
            }
        }

        // 6. Draw modules onto Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(lightColor)

        val padding = 24f
        val renderW = width - (padding * 2)
        val moduleSize = renderW / size
        val paintDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkColor
            style = Paint.Style.FILL
        }

        for (r in 0 until size) {
            for (c in 0 until size) {
                if (matrix[r][c]) {
                    val left = padding + (c * moduleSize)
                    val top = padding + (r * moduleSize)
                    val rect = RectF(left, top, left + moduleSize, top + moduleSize)
                    // Draw clean modules with subtle roundedness for a modern look
                    canvas.drawRoundRect(rect, moduleSize * 0.25f, moduleSize * 0.25f, paintDark)
                }
            }
        }

        return bitmap
    }
}
