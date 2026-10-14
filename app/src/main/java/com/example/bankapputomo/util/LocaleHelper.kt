package com.example.bankapputomo.util

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "BankAppUtomoPrefs"
    private const val LEGACY_PREFS_NAME = "MyPrefs"
    private const val KEY_LANGUAGE = "language"

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var lang = prefs.getString(KEY_LANGUAGE, null)
        if (lang.isNullOrEmpty()) {
            val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            lang = legacy.getString(KEY_LANGUAGE, "id") ?: "id"
        }
        return lang
    }

    fun setLocale(context: Context, languageCode: String): Context {
        persistLanguage(context, languageCode)

        // 1. AndroidX AppCompat Per-App Language
        val localeTag = when (languageCode.lowercase()) {
            "id", "in" -> "in-ID,id"
            "uk" -> "en-GB,en"
            "en", "us", "am" -> "en-US,en"
            "au" -> "en-AU,en"
            "fr" -> "fr-FR,fr"
            "es" -> "es-ES,es"
            "vi" -> "vi-VN,vi"
            "ja" -> "ja-JP,ja"
            "ko" -> "ko-KR,ko"
            "zh" -> "zh-CN,zh"
            else -> languageCode
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))

        // 2. Direct Locale & Resources Configuration Update
        val locale = getLocaleForCode(languageCode)
        Locale.setDefault(locale)

        return updateResources(context, locale)
    }

    fun applyAppLocale(context: Context) {
        val lang = getLanguage(context)
        val locale = getLocaleForCode(lang)
        Locale.setDefault(locale)
        updateResources(context, locale)
        updateResources(context.applicationContext, locale)
    }

    fun wrapContext(context: Context): ContextWrapper {
        val lang = getLanguage(context)
        val locale = getLocaleForCode(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        val updatedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            context.createConfigurationContext(config)
        }

        return ContextWrapper(updatedContext)
    }

    private fun persistLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()

        context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    private fun getLocaleForCode(code: String): Locale {
        return when (code.lowercase()) {
            "id", "in" -> Locale("id", "ID")
            "en", "us", "am" -> Locale.US
            "uk" -> Locale.UK
            "au" -> Locale("en", "AU")
            "fr" -> Locale.FRANCE
            "es" -> Locale("es", "ES")
            "vi" -> Locale("vi", "VN")
            "ja" -> Locale.JAPAN
            "ko" -> Locale.KOREA
            "zh" -> Locale.SIMPLIFIED_CHINESE
            else -> Locale(code)
        }
    }

    @Suppress("DEPRECATION")
    private fun updateResources(context: Context, locale: Locale): Context {
        val res: Resources = context.resources
        val config = Configuration(res.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            config.locale = locale
        }
        config.setLayoutDirection(locale)

        res.updateConfiguration(config, res.displayMetrics)
        return context
    }
}
