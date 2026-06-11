package com.example.smartmosque.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.smartmosque.MainActivity
import com.example.smartmosque.R

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // 1. Ambil data teks dari payload yang dikirim oleh ViewModel
        val title = inputData.getString("title") ?: "Kajian Akan Dimulai!"
        val body = inputData.getString("body") ?: "Jangan lupa hadiri kajian beberapa saat lagi."

        // 2. Picu notifikasi lokal di HP jemaah
        sendLocalNotification(title, body)

        return Result.success()
    }

    private fun sendLocalNotification(title: String, messageBody: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "smart_mosque_reminder_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Gunakan icon siluet putih Anda
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(0xFF047857.toInt()) // Warna Emerald Green khas aplikasi Anda
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pengingat Kajian Smart Mosque",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel khusus untuk pengingat alarm kajian jemaah"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Jalankan notifikasi dengan ID unik berdasarkan waktu saat ini
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}