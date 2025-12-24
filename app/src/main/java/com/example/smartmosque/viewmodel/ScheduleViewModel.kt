package com.example.smartmosque.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.data.model.Schedule
import com.example.smartmosque.data.repository.ScheduleRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel untuk Schedule Screen
 * Mengikuti MVVM Pattern - ViewModel hanya mengelola UI state dan delegate ke Repository
 */
class ScheduleViewModel : ViewModel() {

    // Repository
    private val scheduleRepository = ScheduleRepository()

    // ==================== STATE MANAGEMENT ====================

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchSchedules()
    }

    // ==================== SCHEDULE OPERATIONS ====================

    /**
     * Fetch all active schedules
     * Mengambil data dari Repository yang sudah diperbaiki query-nya
     */
    fun fetchSchedules() {
        viewModelScope.launch {
            _isLoading.value = true
            // Repository sudah menangani filter 'isPublished' dan 'isFinished'
            scheduleRepository.getAllSchedulesFlow()
                .catch { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                .collect { scheduleList ->
                    _schedules.value = scheduleList
                    _isLoading.value = false
                }
        }
    }

    /**
     * Add new schedule
     */
    fun addSchedule(
        title: String,
        speaker: String,
        time: String,
        location: String,
        category: String,
        date: Timestamp,
        description: String,
        streamingUrl: String,
        isPublished: Boolean,
        createdBy: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // FIX: Menghapus 'isActive = true' agar sesuai dengan struktur Database Anda
            val schedule = Schedule(
                title = title,
                speaker = speaker,
                time = time,
                location = location,
                category = category,
                date = date,
                description = description,
                streamingUrl = streamingUrl,
                isPublished = isPublished,
                createdBy = createdBy,
                createdAt = Timestamp.now(),
                // isActive = true, <--- DIHAPUS: Field ini tidak ada di Firestore Anda
                isFinished = false
            )

            scheduleRepository.addSchedule(schedule)
                .onSuccess { schedId -> onSuccess(schedId) }
                .onFailure { exception -> onError(exception.message ?: "Gagal menyimpan jadwal") }
        }
    }

    /**
     * Update existing schedule
     */
    fun updateSchedule(
        scheduleId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            scheduleRepository.updateSchedule(scheduleId, updates)
                .onSuccess { onSuccess() }
                .onFailure { exception -> onError(exception.message ?: "Gagal update jadwal") }
        }
    }

    /**
     * Delete schedule
     */
    fun deleteSchedule(
        scheduleId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(scheduleId)
                .onSuccess { onSuccess() }
                .onFailure { exception -> onError(exception.message ?: "Gagal menghapus jadwal") }
        }
    }

    /**
     * Join event (online or offline)
     */
    fun joinEvent(
        scheduleId: String,
        userId: String,
        isOnline: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            scheduleRepository.joinEvent(scheduleId, userId, isOnline)
                .onSuccess { onSuccess() }
                .onFailure { exception -> onError(exception.message ?: "Gagal join event") }
        }
    }

    /**
     * Leave event
     */
    fun leaveEvent(
        scheduleId: String,
        userId: String,
        isOnline: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            scheduleRepository.leaveEvent(scheduleId, userId, isOnline)
                .onSuccess { onSuccess() }
                .onFailure { exception -> onError(exception.message ?: "Gagal leave event") }
        }
    }
}