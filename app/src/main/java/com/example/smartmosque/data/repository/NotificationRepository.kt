package com.example.smartmosque.data.repository

import android.content.Context
import com.example.smartmosque.utils.NotificationPrefs
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk mengelola Notification Preferences dan FCM
 * Mengikuti Repository Pattern - Single Source of Truth untuk Notification operations
 */
class NotificationRepository(private val context: Context) {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val notificationPrefs = NotificationPrefs(context)

    /**
     * Load notification preferences dari local storage
     */
    fun loadNotificationPreferences(): Map<String, Boolean> {
        return notificationPrefs.getAllTopics()
    }

    /**
     * Check if a topic is enabled
     */
    fun isTopicEnabled(topic: String): Boolean {
        return notificationPrefs.isTopicEnabled(topic)
    }

    /**
     * Toggle notification for a specific topic
     */
    suspend fun toggleNotification(topic: String, isEnabled: Boolean): Result<Unit> {
        return try {
            if (isEnabled) {
                // Subscribe to topic
                FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            } else {
                // Unsubscribe from topic
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            }
            
            // Save to local preferences
            notificationPrefs.setTopicEnabled(topic, isEnabled)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get unread notification count
     */
    fun getUnreadNotificationCount(): Int {
        return notificationPrefs.getUnreadCount()
    }

    /**
     * Mark notifications as read
     */
    fun markNotificationsAsRead() {
        notificationPrefs.markAsRead()
    }

    /**
     * Increment unread notification count
     */
    fun incrementUnreadCount() {
        notificationPrefs.incrementUnreadCount()
    }

    /**
     * Save FCM token to Firestore
     */
    suspend fun saveFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
