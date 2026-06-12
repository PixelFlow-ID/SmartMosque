package com.example.smartmosque.features.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.features.schedule.data.ScheduleRepository
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.core.reminder.ReminderNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class ScheduleViewModel : ViewModel() {
    private val repository = ScheduleRepository()

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules

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
                    _homeSchedules.value = filterSchedulesForHome(list)
                    _isLoading.value = false
                }
        }
    }

    private fun filterSchedulesForHome(list: List<Schedule>): List<Schedule> {
        val currentTime = System.currentTimeMillis()
        val calendarNow = Calendar.getInstance()
        val currentMonth = calendarNow.get(Calendar.MONTH)
        val currentYear = calendarNow.get(Calendar.YEAR)

        return list.filter { schedule ->
            val dateObj = schedule.date?.toDate()
            if (dateObj != null) {
                val calendarEvent = Calendar.getInstance().apply { time = dateObj }

                // Cek apakah bulan dan tahun cocok
                val isSameMonth = calendarEvent.get(Calendar.MONTH) == currentMonth &&
                        calendarEvent.get(Calendar.YEAR) == currentYear

                // Racik jam mulainya agar akurat
                val finalEventTime = try {
                    val hourStart = schedule.time.split("-")[0].trim()
                    val timeParts = hourStart.split(":")
                    calendarEvent.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    calendarEvent.set(Calendar.MINUTE, timeParts[1].toInt())
                    calendarEvent.timeInMillis
                } catch (e: Exception) {
                    calendarEvent.timeInMillis
                }

                // Aturan masuk Home: Belum kelar manual, waktu belum lewat, dan bulan ini
                !schedule.isFinished && currentTime < finalEventTime && isSameMonth
            } else {
                false
            }
        }.sortedBy { it.date }.take(3)
    }

    fun toggleJoin(scheduleId: String, userId: String, isJoined: Boolean, onSuccess: (String) -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.toggleJoinSchedule(scheduleId, userId, isJoined)
                .onSuccess {
                    val message = if (isJoined) "Anda membatalkan kehadiran" else "Berhasil bergabung!"
                    onSuccess(message)
                }
                .onFailure { exception -> onError(exception.message ?: "Terjadi kesalahan") }
        }
    }

    fun publishSchedule(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch { repository.publishSchedule(id).onSuccess { onSuccess() } }
    }

    fun markAsFinished(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch { repository.markAsFinished(id).onSuccess { onSuccess() } }
    }

    fun deleteSchedule(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch { repository.deleteSchedule(id).onSuccess { onSuccess() } }
    }

    // --- REFACTOR: SEKARANG MENGGUNAKAN PARAMETER ISREMINDERACTIVE DARI UI ---
    fun toggleReminder(
        context: Context,
        schedule: Schedule,
        isReminderActive: Boolean, // <--- Parameter baru ditambahkan di sini
        onResult: (String) -> Unit
    ) {
        // Inisialisasi manager pengingat dari core utils
        val reminderManager = ReminderNotificationManager(context)
        val eventId = schedule.id

        // Mengikuti status kiriman dari toggle tombol di UI
        if (!isReminderActive) {
            // Jika di UI dinonaktifkan (false) -> Batalkan alarm di sistem Android
            reminderManager.cancelReminder(eventId)
            onResult("Pengingat kajian dibatalkan")
        } else {
            // Jika di UI diaktifkan (true) -> Pasang Alarm Baru
            val eventDate = schedule.date?.toDate()
            if (eventDate == null) {
                onResult("Gagal mengaktifkan pengingat: Tanggal kosong")
                return
            }

            val calendarTarget = Calendar.getInstance().apply { time = eventDate }

            // Cek apakah waktu acara sudah kedaluwarsa sebelum dipasang
            if (calendarTarget.timeInMillis < System.currentTimeMillis()) {
                onResult("Jadwal sudah lewat, tidak bisa memasang pengingat.")
                return
            }

            // Panggil Helper AlarmManager bawaan file core kita
            reminderManager.scheduleReminder(
                eventId = eventId,
                eventTitle = schedule.title,
                eventTime = schedule.time, // Format string "18:00 - 20:00" diparse di dalam helper
                eventDate = calendarTarget,
                reminderMinutesBefore = 30 // Mengingatkan 30 menit sebelum acara
            )

            onResult("Pengingat diaktifkan! Anda akan diingatkan 30 menit sebelum acara.")
        }
    }
}