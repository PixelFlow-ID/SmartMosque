package com.example.smartmosque.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.data.model.Schedule
import com.example.smartmosque.data.repository.NotificationRepository
import com.example.smartmosque.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel untuk Home Screen
 * Mengikuti MVVM Pattern - ViewModel hanya mengelola UI state dan delegate ke Repository
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // Repositories
    private val scheduleRepository = ScheduleRepository()
    private val notificationRepository = NotificationRepository(application)

    // ==================== STATE MANAGEMENT ====================
    
    // State Statistik & Notifikasi
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications

    private val _eventsThisMonth = MutableStateFlow(0)
    val eventsThisMonth: StateFlow<Int> = _eventsThisMonth

    private val _totalParticipants = MutableStateFlow(0)
    val totalParticipants: StateFlow<Int> = _totalParticipants

    // State untuk Acara Sedang Berlangsung (LIVE)
    private val _ongoingEvent = MutableStateFlow<Schedule?>(null)
    val ongoingEvent: StateFlow<Schedule?> = _ongoingEvent

    init {
        // Inisialisasi data
        checkUnreadNotifications()
        fetchScheduleStats()
        listenForOngoingEvent()
    }

    // ==================== NOTIFICATION OPERATIONS ====================
    
    /**
     * Check if there are unread notifications
     */
    private fun checkUnreadNotifications() {
        val unreadCount = notificationRepository.getUnreadNotificationCount()
        _hasUnreadNotifications.value = unreadCount > 0
    }

    /**
     * Mark notifications as read
     */
    fun markNotificationsAsRead() {
        notificationRepository.markNotificationsAsRead()
        _hasUnreadNotifications.value = false
    }

    // ==================== SCHEDULE STATISTICS ====================
    
    /**
     * Fetch schedule statistics untuk bulan ini
     */
    /**
     * Fetch schedule statistics untuk bulan ini (REALTIME)
     */
    private fun fetchScheduleStats() {
        viewModelScope.launch {
            scheduleRepository.getMonthlyStatsFlow()
                .catch { exception ->
                    _eventsThisMonth.value = 0
                    _totalParticipants.value = 0
                }
                .collect { (eventCount, participantCount) ->
                    _eventsThisMonth.value = eventCount
                    _totalParticipants.value = participantCount
                }
        }
    }

    // ==================== ONGOING EVENT LISTENER ====================
    
    /**
     * Listen for ongoing events (sedang berlangsung)
     */
    private fun listenForOngoingEvent() {
        viewModelScope.launch {
            scheduleRepository.listenForOngoingEvents()
                .catch { exception ->
                    // Handle error
                    _ongoingEvent.value = null
                }
                .collect { event ->
                    _ongoingEvent.value = event
                }
        }
    }
}
