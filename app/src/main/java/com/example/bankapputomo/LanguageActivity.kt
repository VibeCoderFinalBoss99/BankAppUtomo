package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.LocaleHelper
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class LanguageActivity : BaseActivity() {

    data class LanguageModel(val code: String, val name: String, val flagResId: Int)

    // Language list configured with round flag icons matching Figma reference
    private val languages = listOf(
        LanguageModel("en", "English", R.drawable.ic_flag_us_round),
        LanguageModel("au", "Australia", R.drawable.ic_flag_au_round),
        LanguageModel("fr", "Franch", R.drawable.ic_flag_fr_round),
        LanguageModel("es", "Spanish", R.drawable.ic_flag_es_round),
        LanguageModel("am", "America", R.drawable.ic_flag_am_round),
        LanguageModel("vi", "Vietnam", R.drawable.ic_flag_vi_round),
        LanguageModel("id", "Indonesia", R.drawable.ic_flag_id_round),
        LanguageModel("ja", "Japanese", R.drawable.ic_flag_jp_round),
        LanguageModel("uk", "United Kingdom", R.drawable.ic_flag_gb_round)
    )

    private lateinit var containerLanguages: LinearLayout
    private lateinit var inputSearchLanguage: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        findViewById<View>(R.id.btnBack).bounceClick { finish() }

        containerLanguages = findViewById(R.id.containerLanguages)
        inputSearchLanguage = findViewById(R.id.inputSearchLanguage)

        inputSearchLanguage.addTextChangedListener { s ->
            val query = s?.toString()?.trim() ?: ""
            renderLanguages(query)
        }

        renderLanguages()
    }

    private fun renderLanguages(query: String = "") {
        containerLanguages.removeAllViews()
        val currentLang = AppStorage.get(this).language

        val filtered = if (query.isEmpty()) languages else languages.filter {
            it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true)
        }

        filtered.forEach { lang ->
            val itemView = layoutInflater.inflate(R.layout.item_language_row, containerLanguages, false)
            val flagView = itemView.findViewById<ImageView>(R.id.imgLanguageFlag)
            val nameView = itemView.findViewById<TextView>(R.id.textLanguageName)
            val checkView = itemView.findViewById<ImageView>(R.id.iconLanguageCheck)
            val clickRow = itemView.findViewById<View>(R.id.layoutLanguageRowClick) ?: itemView

            flagView.setImageResource(lang.flagResId)
            nameView.text = lang.name

            val isSelected = when (lang.code) {
                "en" -> currentLang.equals("en", ignoreCase = true) || currentLang.isEmpty()
                "id" -> currentLang.equals("id", ignoreCase = true) || currentLang.equals("in", ignoreCase = true)
                else -> lang.code.equals(currentLang, ignoreCase = true)
            }
            checkView.visibility = if (isSelected) View.VISIBLE else View.GONE

            clickRow.bounceClick {
                LocaleHelper.setLocale(this, lang.code)
                AppStorage.get(this).language = lang.code
                Toast.makeText(this, "${lang.name} applied!", Toast.LENGTH_SHORT).show()

                // Return to dashboard and reload full activity stack with new language
                val intent = Intent(this, DashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }

            containerLanguages.addView(itemView)
        }
    }
}
