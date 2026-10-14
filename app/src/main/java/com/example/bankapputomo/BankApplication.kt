package com.example.bankapputomo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.bankapputomo.util.AppStorage

class BankApplication : Application() {

    companion object {
        lateinit var instance: BankApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val storage = AppStorage.init(this)
        com.example.bankapputomo.util.LocaleHelper.applyAppLocale(this)
        if (storage.isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
