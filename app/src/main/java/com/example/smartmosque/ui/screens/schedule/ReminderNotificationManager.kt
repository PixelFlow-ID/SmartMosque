package com.example.smartmosque.ui.screens.schedule

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smartmosque.MainActivity
import com.example.smartmosque.R
import java.util.Calendar

/**
 * Helper class untuk manage notifikasi lokal
 * Menggunakan AlarmManager untuk schedule reminder tanpa biaya Firebase
 */
class ReminderNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        const val CHANNEL_ID = "event_reminder_channel"
        const val CHANNEL_NAME = "Pengingat Jadwal"
        const val NOTIFICATION_ID_BASE = 1000
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Membuat notification channel untuk Android O ke atas
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat untuk jadwal pengajian dan kegiatan masjid"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Schedule reminder untuk event tertentu
     * @param eventId ID unik event
     * @param eventTitle Judul event
     * @param eventTime Waktu event dalam format string (e.g., "18:30")
     * @param eventDate Tanggal event dalam format Calendar
     * @param reminderMinutesBefore Berapa menit sebelum event untuk reminder (default: 30 menit)
     */
    fun scheduleReminder(
        eventId: String,
        eventTitle: String,
        eventTime: String,
        eventDate: Calendar,
        reminderMinutesBefore: Int = 30
    ) {
        // Parse waktu event
        val timeParts = eventTime.split("-")[0].trim().split(":")
        if (timeParts.size != 2) return
        
        val hour = timeParts[0].toIntOrNull() ?: return
        val minute = timeParts[1].toIntOrNull() ?: return
        
        // Set waktu event
        val eventCalendar = eventDate.clone() as Calendar
        eventCalendar.set(Calendar.HOUR_OF_DAY, hour)
        eventCalendar.set(Calendar.MINUTE, minute)
        eventCalendar.set(Calendar.SECOND, 0)
        
        // Kurangi dengan waktu reminder
        eventCalendar.add(Calendar.MINUTE, -reminderMinutesBefore)
        
        // Jangan schedule jika waktu sudah lewat
        if (eventCalendar.timeInMillis < System.currentTimeMillis()) {
            return
        }
        
        // Buat intent untuk broadcast receiver
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("event_title", eventTitle)
            putExtra("event_time", eventTime)
        }
        
        val requestCode = eventId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Schedule alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                eventCalendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                eventCalendar.timeInMillis,
                pendingIntent
            )
        }
        
        // Simpan info reminder ke SharedPreferences
        saveReminderInfo(eventId, eventCalendar.timeInMillis)
    }
    
    /**
     * Cancel reminder yang sudah di-schedule
     */
    fun cancelReminder(eventId: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val requestCode = eventId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        removeReminderInfo(eventId)
    }
    
    /**
     * Cek apakah reminder sudah diaktifkan untuk event tertentu
     */
    fun isReminderSet(eventId: String): Boolean {
        val prefs = context.getSharedPreferences("event_reminders", Context.MODE_PRIVATE)
        return prefs.contains(eventId)
    }
    
    /**
     * Simpan info reminder ke SharedPreferences
     */
    private fun saveReminderInfo(eventId: String, reminderTime: Long) {
        val prefs = context.getSharedPreferences("event_reminders", Context.MODE_PRIVATE)
        prefs.edit().putLong(eventId, reminderTime).apply()
    }
    
    /**
     * Hapus info reminder dari SharedPreferences
     */
    private fun removeReminderInfo(eventId: String) {
        val prefs = context.getSharedPreferences("event_reminders", Context.MODE_PRIVATE)
        prefs.edit().remove(eventId).apply()
    }
}

/**
 * BroadcastReceiver untuk menerima alarm dan menampilkan notifikasi
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra("event_id") ?: return
        val eventTitle = intent.getStringExtra("event_title") ?: "Jadwal Pengajian"
        val eventTime = intent.getStringExtra("event_time") ?: ""
        
        showNotification(context, eventId, eventTitle, eventTime)
    }
    
    private fun showNotification(context: Context, eventId: String, title: String, time: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Intent untuk membuka app ketika notifikasi diklik
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_schedule", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, ReminderNotificationManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pengingat: $title")
            .setContentText("Acara akan dimulai pukul $time. Jangan sampai terlewat!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .build()
        
        notificationManager.notify(eventId.hashCode(), notification)
    }
}
