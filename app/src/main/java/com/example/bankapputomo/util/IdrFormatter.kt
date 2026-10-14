package com.example.bankapputomo.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Shared IDR (Indonesian Rupiah) Formatter
 * Complies with Rule 5:
 * - "Rp" prefix (e.g., Rp 1.250.000)
 * - Dot as thousands separator
 * - Auto-format in amount input fields with correct cursor position
 * - Extraction of raw numeric value
 */
object IdrFormatter {

    private val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val decimalFormat = DecimalFormat("#,###", symbols)

    /**
     * Formats numeric amount to IDR string.
     * Example: 1250000 -> "Rp 1.250.000"
     */
    fun format(amount: Long, withPrefix: Boolean = true): String {
        val formattedNumber = decimalFormat.format(amount)
        return if (withPrefix) "Rp $formattedNumber" else formattedNumber
    }

    /**
     * Formats double or string amounts.
     */
    fun format(amount: Double, withPrefix: Boolean = true): String {
        return format(amount.toLong(), withPrefix)
    }

    /**
     * Parses raw digits from formatted IDR string.
     * Example: "Rp 1.250.000" -> 1250000
     */
    fun parseRaw(text: String?): Long {
        if (text.isNullOrBlank()) return 0L
        val cleanDigits = text.replace("[^0-9]".toRegex(), "")
        return cleanDigits.toLongOrNull() ?: 0L
    }

    /**
     * Attaches a TextWatcher to an EditText that automatically adds dot thousands separators
     * as the user types and preserves cursor position correctly.
     */
    fun applyAmountInputWatcher(editText: EditText, onAmountChanged: ((Long) -> Unit)? = null): TextWatcher {
        val watcher = object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val original = s?.toString() ?: ""
                val rawDigits = original.replace("[^0-9]".toRegex(), "")

                if (rawDigits.isEmpty()) {
                    onAmountChanged?.invoke(0L)
                    return
                }

                val numericValue = rawDigits.toLongOrNull() ?: 0L
                val formatted = decimalFormat.format(numericValue)

                isUpdating = true
                val cursorPosition = editText.selectionStart
                val digitsBeforeCursor = original.substring(0, cursorPosition.coerceIn(0, original.length))
                    .replace("[^0-9]".toRegex(), "").length

                editText.setText(formatted)

                // Calculate new cursor position based on original digit count before cursor
                var newCursor = 0
                var countedDigits = 0
                for (i in formatted.indices) {
                    if (formatted[i].isDigit()) {
                        countedDigits++
                    }
                    if (countedDigits >= digitsBeforeCursor) {
                        newCursor = i + 1
                        break
                    }
                }
                newCursor = newCursor.coerceIn(0, formatted.length)
                editText.setSelection(newCursor)

                isUpdating = false
                onAmountChanged?.invoke(numericValue)
            }
        }

        editText.addTextChangedListener(watcher)
        return watcher
    }

    fun createWatcher(editText: EditText, onAmountChanged: ((Long) -> Unit)? = null): TextWatcher {
        return object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val original = s?.toString() ?: ""
                val rawDigits = original.replace("[^0-9]".toRegex(), "")
                if (rawDigits.isEmpty()) {
                    onAmountChanged?.invoke(0L)
                    return
                }
                val numericValue = rawDigits.toLongOrNull() ?: 0L
                val formatted = decimalFormat.format(numericValue)
                isUpdating = true
                val cursorPosition = editText.selectionStart
                val digitsBeforeCursor = original.substring(0, cursorPosition.coerceIn(0, original.length))
                    .replace("[^0-9]".toRegex(), "").length
                editText.setText(formatted)
                var newCursor = 0
                var countedDigits = 0
                for (i in formatted.indices) {
                    if (formatted[i].isDigit()) countedDigits++
                    if (countedDigits >= digitsBeforeCursor) {
                        newCursor = i + 1
                        break
                    }
                }
                newCursor = newCursor.coerceIn(0, formatted.length)
                editText.setSelection(newCursor)
                isUpdating = false
                onAmountChanged?.invoke(numericValue)
            }
        }
    }
}
