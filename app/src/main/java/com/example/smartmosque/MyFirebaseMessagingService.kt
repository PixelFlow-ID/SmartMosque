package com.example.smartmosque

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Smart Mosque"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: "Ada info baru!"

        // Panggil fungsi untuk memunculkan notifikasi (yang otomatis memicu Badge)
        showNotification(title, message)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "SMART_MOSQUE_CHANNEL"

        // Gunakan ID unik agar notifikasi tidak saling menimpa (bisa menumpuk)
        // Jika ingin menumpuk angka badge (misal "2", "3"), notifikasi harus tetap ada di status bar
        val notificationId = Random.nextInt()

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // --- PENTING: PENGATURAN BADGE ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Smart Mosque Updates",
                NotificationManager.IMPORTANCE_HIGH // Wajib HIGH agar muncul pop-up & badge
            ).apply {
                description = "Notifikasi update aplikasi dan wakaf"
                enableLights(true)
                enableVibration(true)

                // INI KUNCINYA: Mengizinkan Badge (Titik/Angka) pada icon
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Ganti dengan R.drawable.logo_masjid jika ada
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        // .setNumber(1) // Opsional: Beberapa HP (seperti Samsung/Xiaomi) membaca ini untuk angka badge

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
