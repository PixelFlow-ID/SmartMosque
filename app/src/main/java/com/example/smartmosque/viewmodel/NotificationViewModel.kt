package com.example.smartmosque.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.data.model.Donation
import com.example.smartmosque.data.model.Schedule
import com.example.smartmosque.data.model.WaqfProject
import com.example.smartmosque.data.repository.ScheduleRepository
import com.example.smartmosque.data.repository.WaqfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

// --- MODEL WRAPPER FOR UI ---
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
}

/**
 * ViewModel untuk Notification Screen (Admin & Jamaah)
 */
class NotificationViewModel : ViewModel() {

    private val waqfRepository = WaqfRepository()
    private val scheduleRepository = ScheduleRepository()

    // --- STATE ---
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
        viewModelScope.launch {
            waqfRepository.getPendingDonationsFlow()
                .catch { /* handle error */ }
                .collect { donations ->
                    _pendingDonations.value = donations
                }
        }
    }

    // --- ADMIN: APPROVE DONATION ---
    fun approveDonation(donation: Donation, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            waqfRepository.approveDonation(donation)
                .onSuccess {
                    onSuccess()
                }
                .onFailure {
                    onFailure(it.message ?: "Gagal memproses donasi")
                }
        }
    }

    // --- ADMIN: REJECT DONATION ---
    fun rejectDonation(donationId: String, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            waqfRepository.rejectDonation(donationId)
                .onSuccess {
                    onSuccess()
                }
                .onFailure {
                    onFailure(it.message ?: "Gagal menolak donasi")
                }
        }
    }

    // --- JAMAAH: FETCH NOTIFICATIONS ---
    fun fetchJamaahNotifications(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val combinedList = mutableListOf<JamaahNotificationItem>()

            try {
                // 1. Get User Donations (take 10 most recent)
                // Note: repositories allow flows, but here we want a one-time fetch or combined stream
                // For simplicity and matching original logic (which was one-time fetch on function call),
                // we will collect the first emission of the flows.
                // Ideally this should be a combined flow, but to keep "LOGIC SAME" constraint and complexity low:

                val donations = waqfRepository.getUserDonationsFlow(userId).first()
                donations.take(10).forEach { 
                    combinedList.add(JamaahNotificationItem.DonationStatus(it)) 
                }

                // 2. Get New Schedules (take 5 published)
                val schedules = scheduleRepository.getAllSchedulesFlow().first()
                schedules.filter { it.isPublished }
                    .sortedByDescending { it.createdAt }
                    .take(5)
                    .forEach { 
                        combinedList.add(JamaahNotificationItem.NewSchedule(it)) 
                    }

                // 3. Get New Waqf Projects (take 3)
                val projects = waqfRepository.getAllWaqfProjectsFlow().first()
                projects.sortedByDescending { it.createdAt }
                    .take(3)
                    .forEach {
                        combinedList.add(JamaahNotificationItem.NewWaqf(it))
                    }

                // Sort Gabungan Akhir
                _jamaahNotifications.value = combinedList.sortedByDescending { it.timestamp }
                _isLoading.value = false

            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}
