package com.example.smartmosque.features.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.model.Donation
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.model.WaqfProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import com.example.smartmosque.features.notification.data.NotificationRepository
import com.google.firebase.firestore.ListenerRegistration


sealed class JamaahNotificationItem {
    abstract val timestamp: Date
    data class DonationStatus(val donation: Donation) : JamaahNotificationItem() {
        override val timestamp: Date = donation.date?.toDate() ?: Date()
    }
    data class NewSchedule(val schedule: Schedule) : JamaahNotificationItem() {
        override val timestamp: Date = schedule.createdAt?.toDate() ?: Date()
    }
    data class NewWaqf(val waqf: WaqfProject) : JamaahNotificationItem() {
        override val timestamp: Date = waqf.createdAt?.toDate() ?: Date()
    }
    data class GeneralNotification(
        val id: String,
        val title: String,
        val body: String,
        val type: String,
        override val timestamp: Date
    ) : JamaahNotificationItem()
}

class NotificationViewModel : ViewModel() {

    // Inisialisasi Repository
    private val repository = NotificationRepository()
    private var pendingDonationsListener: ListenerRegistration? = null

    private val _pendingDonations = MutableStateFlow<List<Donation>>(emptyList())
    val pendingDonations: StateFlow<List<Donation>> = _pendingDonations

    private val _jamaahNotifications = MutableStateFlow<List<JamaahNotificationItem>>(emptyList())
    val jamaahNotifications: StateFlow<List<JamaahNotificationItem>> = _jamaahNotifications

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchPendingDonationsForAdmin()
    }

    // --- ADMIN: FETCH PENDING DONATIONS ---
    private fun fetchPendingDonationsForAdmin() {
        pendingDonationsListener = repository.listenPendingDonations(
            onSuccess = { list ->
                _pendingDonations.value = list.sortedByDescending { it.date }
            },
            onError = { e ->
                Log.e("AdminNotif", "Error fetch: ${e.message}")
            }
        )
    }

    // --- ADMIN: APPROVE ---
    fun approveDonation(donation: Donation) {
        viewModelScope.launch {
            try {
                repository.approveDonation(donation)
                Log.d("AdminAction", "Donasi berhasil disetujui.")
            } catch (e: Exception) {
                Log.e("AdminAction", "Gagal approval: ${e.message}")
            }
        }
    }

    // --- ADMIN: REJECT ---
    fun rejectDonation(donationId: String) {
        viewModelScope.launch {
            try {
                repository.rejectDonation(donationId)
                Log.d("AdminAction", "Donasi berhasil ditolak.")
            } catch (e: Exception) {
                Log.e("AdminAction", "Gagal reject: ${e.message}")
            }
        }
    }

    // --- JAMAAH: FETCH NOTIFICATIONS (COMBINED) ---
    fun fetchJamaahNotifications(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val combinedList = mutableListOf<JamaahNotificationItem>()

            try {
                // 1. Ambil Data Donasi
                val donations = repository.fetchUserDonations(userId)
                donations.forEach { combinedList.add(JamaahNotificationItem.DonationStatus(it)) }

                // 2. Ambil Data Jadwal
                val schedules = repository.fetchPublishedSchedules()
                schedules.forEach { combinedList.add(JamaahNotificationItem.NewSchedule(it)) }

                // 3. Ambil Data Wakaf
                val waqfProjects = repository.fetchWaqfProjects()
                waqfProjects.forEach { combinedList.add(JamaahNotificationItem.NewWaqf(it)) }

                // 4. Ambil Data Notifikasi Sistem (FCM Backup)
                val systemNotifs = repository.fetchSystemNotifications()
                systemNotifs.forEach { doc ->
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: Date()
                    val title = doc.getString("title") ?: "Informasi"
                    val body = doc.getString("body") ?: ""
                    val type = doc.getString("type") ?: "system"

                    combinedList.add(
                        JamaahNotificationItem.GeneralNotification(
                            id = doc.id, title = title, body = body, type = type, timestamp = timestamp
                        )
                    )
                }

                // Sorting Gabungan Akhir
                _jamaahNotifications.value = combinedList.sortedByDescending { it.timestamp }
                _isLoading.value = false

            } catch (e: Exception) {
                Log.e("JamaahNotif", "Error General: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Hapus listener agar tidak terjadi memory leak saat ViewModel dihancurkan
        pendingDonationsListener?.remove()
    }
}