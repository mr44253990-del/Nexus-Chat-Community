package com.example.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit().putBoolean("is_first_launch", value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", true) // Default to dark as per user request
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()
}
