package com.example.smartmosque.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.utils.NotificationPrefs
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

// Gunakan AndroidViewModel untuk akses Context (Preferences)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // --- STATE NOTIFIKASI ---
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications

    // --- STATE STATISTIK DASHBOARD ---
    // Jumlah Kegiatan Bulan Ini (Grafik Kiri)
    private val _eventsThisMonth = MutableStateFlow(0)
    val eventsThisMonth: StateFlow<Int> = _eventsThisMonth

    // Total Jamaah/Partisipan yang hadir di kegiatan bulan ini (Grafik Kanan)
    private val _totalParticipants = MutableStateFlow(0)
    val totalParticipants: StateFlow<Int> = _totalParticipants

    init {
        // 1. Cek Notifikasi Baru (Berdasarkan createdAt)
        checkNewNotifications()
        // 2. Hitung Statistik (Logic yang diperkuat)
        fetchScheduleStats()
    }

    // --- LOGIKA CEK NOTIFIKASI (TIDAK DIUBAH / TETAP) ---
    private fun checkNewNotifications() {
        viewModelScope.launch {
            try {
                // 1. Ambil waktu terakhir user klik lonceng dari HP
                val context = getApplication<Application>().applicationContext
                val lastCheckTime = NotificationPrefs.getLastCheckTime(context)

                var hasNewContent = false

                // 2. Query ke Firebase: Ambil 1 Jadwal yang paling baru diposting
                val latestSchedule = Firebase.firestore.collection("schedules")
                    .orderBy("createdAt", Query.Direction.DESCENDING) // Urutkan berdasarkan WAKTU POSTING
                    .limit(1)
                    .get()
                    .await()

                if (!latestSchedule.isEmpty) {
                    val postingTime = latestSchedule.documents[0].getTimestamp("createdAt")?.toDate()

                    // Jika waktu posting > waktu terakhir user cek, berarti ADA NOTIF BARU
                    if (postingTime != null && postingTime.time > lastCheckTime) {
                        hasNewContent = true
                    }
                }

                // 3. Cek Wakaf Baru (Jika jadwal belum ada yang baru)
                if (!hasNewContent) {
                    val latestWaqf = Firebase.firestore.collection("waqf_programs")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await()

                    if (!latestWaqf.isEmpty) {
                        val waqfTime = latestWaqf.documents[0].getTimestamp("createdAt")?.toDate()
                        if (waqfTime != null && waqfTime.time > lastCheckTime) {
                            hasNewContent = true
                        }
                    }
                }

                // 4. Update UI (Titik Merah)
                _hasUnreadNotifications.value = hasNewContent

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Dipanggil saat User Klik Lonceng di MainActivity
    fun markNotificationsAsRead() {
        _hasUnreadNotifications.value = false
        val context = getApplication<Application>().applicationContext
        NotificationPrefs.saveLastCheckTime(context)
    }

    // --- LOGIKA STATISTIK (DIPERKUAT & LEBIH AMAN) ---
    private fun fetchScheduleStats() {
        viewModelScope.launch {
            // Menggunakan listener agar data di Home berubah real-time saat ada yang absen
            Firebase.firestore.collection("schedules")
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener

                    val cal = Calendar.getInstance()
                    val currentMonth = cal.get(Calendar.MONTH)
                    val currentYear = cal.get(Calendar.YEAR)

                    var countThisMonth = 0
                    var totalAttendance = 0

                    for (doc in snapshot.documents) {
                        try {
                            val timestamp = doc.getTimestamp("date")
                            val eventDate = timestamp?.toDate()

                            if (eventDate != null) {
                                cal.time = eventDate
                                // Filter: Hanya ambil data BULAN INI & TAHUN INI
                                if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {

                                    // 1. Tambah Counter Kegiatan
                                    countThisMonth++

                                    // 2. Hitung Peserta (Dengan Pengaman Tipe Data)
                                    // Masalah: Kadang data berupa List, kadang String (error data lama)

                                    // Handle Online
                                    val rawOnline = doc.get("participantsOnline")
                                    val onlineCount = when (rawOnline) {
                                        is List<*> -> rawOnline.size
                                        is String -> if (rawOnline.isNotEmpty()) 1 else 0
                                        else -> 0
                                    }

                                    // Handle Offline
                                    val rawOffline = doc.get("participantsOffline")
                                    val offlineCount = when (rawOffline) {
                                        is List<*> -> rawOffline.size
                                        is String -> if (rawOffline.isNotEmpty()) 1 else 0
                                        else -> 0
                                    }

                                    // Jumlahkan Total
                                    totalAttendance += (onlineCount + offlineCount)
                                }
                            }
                        } catch (err: Exception) {
                            // Jika ada satu dokumen rusak, skip saja, jangan bikin crash aplikasi
                            err.printStackTrace()
                        }
                    }

                    // Update State ke UI
                    _eventsThisMonth.value = countThisMonth
                    _totalParticipants.value = totalAttendance
                }
        }
    }
}