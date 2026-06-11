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
        // 1. Ambil data teks judul dan isi dari Firebase
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Smart Mosque"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Ada informasi baru untuk Anda"

        // 2. Ambil tipe payload data untuk menentukan jenis notifikasinya (schedule, wakaf, atau system)
        val type = remoteMessage.data["type"] ?: "system"

        // 3. Tampilkan Notifikasi Push di Layar
        sendNotification(title, body)

        // 4. Simpan status ke SharedPreferences agar Ikon Lonceng di HomeScreen tahu ada info unread beserta tipenya
        val sharedPreferences = getSharedPreferences("smart_mosque_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean("has_new_notification", true)
            putString("last_notification_type", type)
        }.apply()
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "smart_mosque_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(0xFF047857.toInt())

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifikasi Smart Mosque",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}