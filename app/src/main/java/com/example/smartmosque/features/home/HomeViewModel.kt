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
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // --- STATE NOTIFIKASI ---
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications

    // --- STATE STATISTIK DASHBOARD ---
    private val _eventsThisMonth = MutableStateFlow(0)
    val eventsThisMonth: StateFlow<Int> = _eventsThisMonth

    private val _totalParticipants = MutableStateFlow(0)
    val totalParticipants: StateFlow<Int> = _totalParticipants

    // Variabel lokal untuk menyimpan waktu terakhir cek di memori
    // Agar listener bisa langsung membandingkan tanpa baca Prefs berulang kali
    private var lastCheckTime: Long = 0L

    init {
        // 1. Load waktu terakhir cek dari HP saat inisialisasi
        val context = getApplication<Application>().applicationContext
        lastCheckTime = NotificationPrefs.getLastCheckTime(context)

        // 2. Mulai mendengarkan data secara REAL-TIME
        listenForNewNotifications()

        // 3. Hitung Statistik
        fetchScheduleStats()
    }

    // --- LOGIKA NOTIFIKASI REAL-TIME (DIPERBAIKI) ---
    private fun listenForNewNotifications() {
        // Kita pasang 2 Listener: Satu untuk Jadwal, Satu untuk Wakaf

        // A. LISTENER JADWAL
        Firebase.firestore.collection("schedules")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                val latestDoc = snapshot.documents[0]
                val createdDate = latestDoc.getTimestamp("createdAt")?.toDate()

                if (createdDate != null) {
                    // Cek: Apakah waktu buat data > waktu terakhir user buka notif?
                    if (createdDate.time > lastCheckTime) {
                        _hasUnreadNotifications.value = true
                    }
                }
            }

        // B. LISTENER WAKAF
        Firebase.firestore.collection("waqf_programs") // Pastikan nama koleksi sesuai di Firebase
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                val latestDoc = snapshot.documents[0]
                val createdDate = latestDoc.getTimestamp("createdAt")?.toDate()

                if (createdDate != null) {
                    // Cek: Apakah waktu buat data > waktu terakhir user buka notif?
                    if (createdDate.time > lastCheckTime) {
                        _hasUnreadNotifications.value = true
                    }
                }
            }
    }

    // Dipanggil saat User Klik Lonceng
    fun markNotificationsAsRead() {
        // 1. Hilangkan Badge di UI seketika
        _hasUnreadNotifications.value = false

        // 2. Simpan waktu SEKARANG sebagai "Titik Referensi Baru"
        val currentTime = System.currentTimeMillis()
        lastCheckTime = currentTime // Update variabel memori agar listener tahu

        // 3. Simpan ke Memory HP (SharedPrefs) agar tersimpan walau aplikasi ditutup
        val context = getApplication<Application>().applicationContext
        NotificationPrefs.saveLastCheckTime(context) // Pastikan fungsi ini menyimpan System.currentTimeMillis()
    }

    // --- LOGIKA STATISTIK (SUDAH BENAR, DIPERTAHANKAN) ---
    private fun fetchScheduleStats() {
        viewModelScope.launch {
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
                                if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                                    countThisMonth++

                                    val rawOnline = doc.get("participantsOnline")
                                    val onlineCount = when (rawOnline) {
                                        is List<*> -> rawOnline.size
                                        is String -> if (rawOnline.isNotEmpty()) 1 else 0
                                        else -> 0
                                    }

                                    val rawOffline = doc.get("participantsOffline")
                                    val offlineCount = when (rawOffline) {
                                        is List<*> -> rawOffline.size
                                        is String -> if (rawOffline.isNotEmpty()) 1 else 0
                                        else -> 0
                                    }
                                    totalAttendance += (onlineCount + offlineCount)
                                }
                            }
                        } catch (err: Exception) {
                            err.printStackTrace()
                        }
                    }
                    _eventsThisMonth.value = countThisMonth
                    _totalParticipants.value = totalAttendance
                }
        }
    }
}