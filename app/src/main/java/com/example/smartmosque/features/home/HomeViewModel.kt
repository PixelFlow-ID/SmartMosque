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

    // --- LOGIKA NOTIFIKASI REAL-TIME ---
    private fun listenForNewNotifications() {
        // A. LISTENER JADWAL
        Firebase.firestore.collection("schedules")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                val latestDoc = snapshot.documents[0]
                val createdDate = latestDoc.getTimestamp("createdAt")?.toDate()
                // Cek apakah jadwal ini dipublish? (Opsional: agar notif tidak muncul kalau masih draft)
                val isPublished = latestDoc.getBoolean("isPublished") ?: true

                if (createdDate != null && isPublished) {
                    if (createdDate.time > lastCheckTime) {
                        _hasUnreadNotifications.value = true
                    }
                }
            }

        // B. LISTENER WAKAF
        Firebase.firestore.collection("waqf_programs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                val latestDoc = snapshot.documents[0]
                val createdDate = latestDoc.getTimestamp("createdAt")?.toDate()

                if (createdDate != null) {
                    if (createdDate.time > lastCheckTime) {
                        _hasUnreadNotifications.value = true
                    }
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

    // --- LOGIKA STATISTIK (DIPERBAIKI UNTUK FILTER DRAFT) ---
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
                            // 1. FILTER: CEK APAKAH SUDAH TAYANG (PUBLISHED)
                            // Default true agar data lama (sebelum fitur draft ada) tetap muncul
                            val isPublished = doc.getBoolean("isPublished") ?: true

                            // Jika False (Draft), skip/lompati item ini, jangan dihitung
                            if (!isPublished) continue

                            // 2. CEK TANGGAL
                            val timestamp = doc.getTimestamp("date")
                            val eventDate = timestamp?.toDate()

                            if (eventDate != null) {
                                cal.time = eventDate
                                // Cek apakah bulan dan tahun sama dengan saat ini
                                if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {

                                    // Hitung Jumlah Kegiatan
                                    countThisMonth++

                                    // Hitung Jumlah Peserta (Online + Offline)
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
                    // Update State UI
                    _eventsThisMonth.value = countThisMonth
                    _totalParticipants.value = totalAttendance
                }
        }
    }
}