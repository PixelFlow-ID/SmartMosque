package com.example.smartmosque.utils

import android.content.Context
import android.content.SharedPreferences

object NotificationPrefs {
    private const val PREF_NAME = "smart_mosque_notif_prefs"
    private const val KEY_LAST_CHECK = "last_notification_check_timestamp"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Ambil waktu terakhir user klik lonceng
    fun getLastCheckTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_CHECK, 0L)
    }

    // Simpan waktu SEKARANG (saat user klik lonceng)
    fun saveLastCheckTime(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }
}