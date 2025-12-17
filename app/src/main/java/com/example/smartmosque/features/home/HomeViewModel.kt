package com.example.smartmosque.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.utils.NotificationPrefs
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import java.util.Date

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // --- STATE STATISTIK & NOTIFIKASI ---
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications

    private val _eventsThisMonth = MutableStateFlow(0)
    val eventsThisMonth: StateFlow<Int> = _eventsThisMonth

    private val _totalParticipants = MutableStateFlow(0)
    val totalParticipants: StateFlow<Int> = _totalParticipants

    private var lastCheckTime: Long = 0L

    // --- STATE UNTUK ACARA SEDANG BERLANGSUNG (LIVE) ---
    private val _ongoingEvent = MutableStateFlow<Schedule?>(null)
    val ongoingEvent: StateFlow<Schedule?> = _ongoingEvent

    init {
        val context = getApplication<Application>().applicationContext
        lastCheckTime = NotificationPrefs.getLastCheckTime(context)

        // Memanggil semua fungsi listener
        listenForNewNotifications()
        fetchScheduleStats()
        listenForOngoingEvent()
    }

    // ==========================================================
    // 1. LOGIKA NOTIFIKASI (Mengecek notif baru)
    // ==========================================================
    private fun listenForNewNotifications() {
        // Cek notifikasi yang waktunya lebih baru dari 'lastCheckTime'
        Firebase.firestore.collection("notifications")
            .whereGreaterThan("timestamp", Date(lastCheckTime))
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && !snapshot.isEmpty) {
                    _hasUnreadNotifications.value = true
                }
            }
    }

    fun markNotificationsAsRead() {
        _hasUnreadNotifications.value = false
        val currentTime = System.currentTimeMillis()
        lastCheckTime = currentTime
        val context = getApplication<Application>().applicationContext
        NotificationPrefs.saveLastCheckTime(context)
    }

    // ==========================================================
    // 2. LOGIKA STATISTIK (Menghitung Event & Jamaah Bulan Ini)
    // ==========================================================
    private fun fetchScheduleStats() {
        val calendar = Calendar.getInstance()

        // Tentukan Awal Bulan Ini (Tanggal 1, jam 00:00:00)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time

        // Tentukan Awal Bulan Depan (sebagai batas atas)
        calendar.add(Calendar.MONTH, 1)
        val startOfNextMonth = calendar.time

        // Query ke Firestore: Ambil jadwal yang ada di bulan ini
        Firebase.firestore.collection("schedules")
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThan("date", startOfNextMonth)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null) {
                    var publishedEventCount = 0
                    var totalParticipantCount = 0

                    // Loop semua dokumen untuk memfilter DRAFT
                    for (doc in snapshot.documents) {
                        // --- FILTERING DITAMBAHKAN DI SINI ---
                        // Ambil status published, default false jika null
                        val isPublished = doc.getBoolean("isPublished") ?: false

                        // Cek juga status DRAFT dari string (jaga-jaga legacy data)
                        val status = doc.getString("status") ?: ""
                        val isDraftStatus = status.equals("DRAFT", ignoreCase = true)

                        // Hanya hitung jika Published DAN bukan Draft
                        if (isPublished && !isDraftStatus) {

                            // 1. Tambah Jumlah Event
                            publishedEventCount++

                            // 2. Tambah Jumlah Peserta (Online + Offline)
                            val onlineList = doc.get("participantsOnline") as? List<*> ?: emptyList<Any>()
                            val offlineList = doc.get("participantsOffline") as? List<*> ?: emptyList<Any>()

                            totalParticipantCount += (onlineList.size + offlineList.size)
                        }
                    }

                    // Update State UI
                    _eventsThisMonth.value = publishedEventCount
                    _totalParticipants.value = totalParticipantCount
                }
            }
    }

    // ==========================================================
    // 3. LOGIKA LIVE EVENT (Mendeteksi Kajian Sedang Berlangsung)
    // ==========================================================
    private fun listenForOngoingEvent() {
        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        // Kita mundur 4 jam ke belakang untuk mencari event yang mungkin masih berjalan
        calendar.add(Calendar.HOUR_OF_DAY, -4)
        val searchStart = calendar.time

        Firebase.firestore.collection("schedules")
            .whereGreaterThan("date", searchStart) // Ambil jadwal yang mulai baru-baru ini
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val currentTime = System.currentTimeMillis()
                val threeHoursInMillis = 3 * 60 * 60 * 1000 // Asumsi durasi kajian max 3 jam

                // Cari jadwal pertama yang memenuhi syarat LIVE
                val activeEventDoc = snapshot.documents.firstOrNull { doc ->
                    val isPublished = doc.getBoolean("isPublished") ?: true
                    // UPDATE: Cek apakah event sudah ditandai selesai oleh admin
                    val isCompleted = doc.getBoolean("isCompleted") ?: false
                    val date = doc.getTimestamp("date")?.toDate()

                    // Syarat Tampil:
                    // 1. Sudah dipublish
                    // 2. BELUM SELESAI (!isCompleted)
                    // 3. Waktunya Valid (Sekarang >= Mulai & Sekarang < Selesai)
                    if (isPublished && !isCompleted && date != null) {
                        val startTime = date.time
                        val endTime = startTime + threeHoursInMillis

                        // LOGIKA WAKTU: Waktu sekarang ada di antara Mulai & Selesai
                        startTime <= currentTime && currentTime < endTime
                    } else {
                        false
                    }
                }

                if (activeEventDoc != null) {
                    try {
                        // Mapping Data ke Model Schedule
                        val schedule = Schedule(
                            id = activeEventDoc.id,
                            title = activeEventDoc.getString("title") ?: "",
                            speaker = activeEventDoc.getString("speaker") ?: "",
                            time = activeEventDoc.getString("time") ?: "",
                            location = activeEventDoc.getString("location") ?: "",
                            category = activeEventDoc.getString("category") ?: "Pengajian",
                            date = activeEventDoc.getTimestamp("date"),
                            participantsOnline = (activeEventDoc.get("participantsOnline") as? List<String>) ?: emptyList(),
                            participantsOffline = (activeEventDoc.get("participantsOffline") as? List<String>) ?: emptyList(),
                            streamingUrl = activeEventDoc.getString("streamingUrl") ?: "",
                            isPublished = activeEventDoc.getBoolean("isPublished") ?: true,
                            isFinished = activeEventDoc.getBoolean("isCompleted") ?: false
                        )
                        _ongoingEvent.value = schedule
                    } catch (e: Exception) {
                        _ongoingEvent.value = null
                    }
                } else {
                    _ongoingEvent.value = null
                }
            }
    }
}