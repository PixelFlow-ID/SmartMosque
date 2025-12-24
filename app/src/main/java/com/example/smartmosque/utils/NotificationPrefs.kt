package com.example.smartmosque.utils

import android.content.Context
import android.content.SharedPreferences

class NotificationPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_mosque_notif_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_CHECK = "last_notification_check_timestamp"
        private const val KEY_UNREAD_COUNT = "unread_notification_count"
        private const val PREFIX_TOPIC = "topic_"
    }

    // --- TOPICS ---
    fun getAllTopics(): Map<String, Boolean> {
        val all = prefs.all
        val topics = mutableMapOf<String, Boolean>()
        all.forEach { (key, value) ->
            if (key.startsWith(PREFIX_TOPIC) && value is Boolean) {
                topics[key.removePrefix(PREFIX_TOPIC)] = value
            }
        }
        // Jika kosong (pertama kali install), set default
        if (topics.isEmpty()) {
            val defaults = mapOf("general" to true, "waqf" to true, "events" to true)
            defaults.forEach { (k, v) -> setTopicEnabled(k, v) }
            return defaults
        }
        return topics
    }

    fun isTopicEnabled(topic: String): Boolean {
        return prefs.getBoolean("$PREFIX_TOPIC$topic", true) // Default true
    }

    fun setTopicEnabled(topic: String, isEnabled: Boolean) {
        prefs.edit().putBoolean("$PREFIX_TOPIC$topic", isEnabled).apply()
    }

    // --- UNREAD COUNT ---
    fun getUnreadCount(): Int {
        return prefs.getInt(KEY_UNREAD_COUNT, 0)
    }

    fun incrementUnreadCount() {
        val current = getUnreadCount()
        prefs.edit().putInt(KEY_UNREAD_COUNT, current + 1).apply()
    }

    fun markAsRead() {
        prefs.edit().putInt(KEY_UNREAD_COUNT, 0).apply()
    }

     // --- CHECK TIME ---
    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK, 0L)
    }

    fun saveLastCheckTime() {
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }
}