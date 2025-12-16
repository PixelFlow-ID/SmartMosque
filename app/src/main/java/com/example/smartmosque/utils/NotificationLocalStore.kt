package com.example.smartmosque.utils
import android.content.Context

class NotificationLocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("smart_mosque_notif", Context.MODE_PRIVATE)

    // Simpan waktu sekarang (saat user buka notifikasi)
    fun markAsRead() {
        prefs.edit().putLong("last_seen_timestamp", System.currentTimeMillis()).apply()
    }

    // Ambil waktu terakhir dilihat
    fun getLastSeenTimestamp(): java.util.Date {
        val time = prefs.getLong("last_seen_timestamp", 0L)
        return java.util.Date(time)
    }
}