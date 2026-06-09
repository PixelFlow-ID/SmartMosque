package com.example.smartmosque.features.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.data.repository.ScheduleRepository
import com.example.smartmosque.model.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class ScheduleViewModel : ViewModel() {
    private val repository = ScheduleRepository()

    // State untuk Semua Jadwal (Dipakai di ScheduleScreen)
    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules

    // State Khusus Home Screen (Hanya 3, Belum Mulai, Bulan Ini)
    private val _homeSchedules = MutableStateFlow<List<Schedule>>(emptyList())
    val homeSchedules: StateFlow<List<Schedule>> = _homeSchedules

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchAllSchedules()
    }

    private fun fetchAllSchedules() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllSchedulesFlow()
                .catch { _isLoading.value = false }
                .collect { list ->
                    _schedules.value = list

                    // Filter data secara real-time untuk kebutuhan Home Screen
                    _homeSchedules.value = filterSchedulesForHome(list)

                    _isLoading.value = false
                }
        }
    }

    // --- LOGIKA FILTER HOME SCREEN ---
    private fun filterSchedulesForHome(list: List<Schedule>): List<Schedule> {
        val now = Date()
        val calendarNow = Calendar.getInstance()
        val currentMonth = calendarNow.get(Calendar.MONTH)
        val currentYear = calendarNow.get(Calendar.YEAR)

        return list.filter { schedule ->
            val eventDate = schedule.date?.toDate()
            if (eventDate != null) {
                val calendarEvent = Calendar.getInstance().apply { time = eventDate }
                val isSameMonth = calendarEvent.get(Calendar.MONTH) == currentMonth &&
                        calendarEvent.get(Calendar.YEAR) == currentYear

                // Syarat: Belum ditandai selesai, waktu belum terlewat, dan berada di bulan ini
                !schedule.isFinished && eventDate.after(now) && isSameMonth
            } else {
                false
            }
        }.take(3) // Batasi maksimal hanya 3 item saja
    }

    fun toggleJoin(
        scheduleId: String,
        userId: String,
        isJoined: Boolean,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.toggleJoinSchedule(scheduleId, userId, isJoined)
                .onSuccess {
                    val message = if (isJoined) "Anda membatalkan kehadiran" else "Berhasil bergabung!"
                    onSuccess(message)
                }
                .onFailure { exception ->
                    onError(exception.message ?: "Terjadi kesalahan")
                }
        }
    }

    fun publishSchedule(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.publishSchedule(id).onSuccess { onSuccess() }
        }
    }

    fun markAsFinished(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.markAsFinished(id).onSuccess { onSuccess() }
        }
    }

    fun deleteSchedule(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteSchedule(id).onSuccess { onSuccess() }
        }
    }
}