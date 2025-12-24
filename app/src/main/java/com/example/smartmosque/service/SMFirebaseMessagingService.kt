package com.example.smartmosque.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.smartmosque.MainActivity
import com.example.smartmosque.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SMFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM", "Token Baru: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Prioritaskan notifikasi dari console atau payload data
        val title = remoteMessage.notification?.title ?: "Smart Mosque"
        val body = remoteMessage.notification?.body ?: "Ada informasi baru untuk Anda"

        sendNotification(title, body)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // Flag Immutable wajib untuk Android 12+
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "smart_mosque_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Warna Emerald Green (Sesuai tema aplikasi Anda)
        val emeraldColor = ContextCompat.getColor(this, R.color.teal_700) // Atau masukkan kode warna manual 0xFF047857

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // PENTING: Icon notifikasi sebaiknya putih transparan (siluet)
            // Jika pakai ic_launcher, seringkali jadi kotak putih di Android baru
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(0xFF047857.toInt()) // <--- TAMBAHAN: Warna Emerald pada teks judul/icon kecil

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Setup Channel untuk Android Oreo ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifikasi Smart Mosque",
                NotificationManager.IMPORTANCE_HIGH // Ubah ke HIGH agar muncul popup (Heads-up)
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
