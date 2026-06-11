package com.example.smartmosque.features.admin.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.features.admin.data.AdminScheduleRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class AdminScheduleViewModel : ViewModel() {
    private val repository = AdminScheduleRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    // 1. Mengambil data lama untuk dimuat ke form edit
    fun fetchScheduleDetail(
        id: String,
        onSuccess: (DocumentSnapshot) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getScheduleById(id)
                .onSuccess { snapshot ->
                    _isLoading.value = false
                    onSuccess(snapshot)
                }
                .onFailure { error ->
                    _isLoading.value = false
                    onError(error.message ?: "Gagal memuat data jadwal")
                }
        }
    }

    // 2. Fungsi Simpan (Add Schedule) - Sinkron dengan AddScheduleScreen
    fun saveSchedule(
        title: String,
        speaker: String,
        location: String,
        category: String,
        streamingUrl: String,
        startTime: String,
        endTime: String,
        selectedDate: Date,
        isPublished: Boolean,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val fullTimeRange = "$startTime - $endTime"

            val scheduleData = hashMapOf(
                "title" to title,
                "speaker" to speaker,
                "location" to location,
                "category" to category,
                "streamingUrl" to streamingUrl,
                "time" to fullTimeRange,
                "date" to Timestamp(selectedDate),
                "isPublished" to isPublished,
                "isFinished" to false,
                "participantsOnline" to emptyList<String>(),
                "participantsOffline" to emptyList<String>(),
                "createdAt" to Timestamp.now()
            )

            repository.addSchedule(scheduleData)
                .onSuccess {
                    _isLoading.value = false
                    val msg = if (isPublished) "Jadwal dipublikasikan!" else "Jadwal disimpan sebagai draft"
                    onSuccess(msg)
                }
                .onFailure { error ->
                    _isLoading.value = false
                    onError(error.message ?: "Gagal menyimpan jadwal")
                }
        }
    }

    // 3. Memperbarui data jadwal lama (Edit Schedule) - Dibuat KONSISTEN dengan fungsi save
    fun updateSchedule(
        id: String,
        title: String,
        speaker: String,
        location: String,
        category: String,
        streamingUrl: String, // Ditambahkan agar link YouTube bisa diedit juga
        startTime: String,    // Dipecah agar UI Edit Screen mudah memakai TimePicker
        endTime: String,      // Dipecah agar UI Edit Screen mudah memakai TimePicker
        selectedDate: Date,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val fullTimeRange = "$startTime - $endTime"

            val updates = hashMapOf<String, Any>(
                "title" to title,
                "speaker" to speaker,
                "location" to location,
                "category" to category,
                "streamingUrl" to streamingUrl,
                "time" to fullTimeRange,
                "date" to Timestamp(selectedDate)
            )

            repository.updateSchedule(id, updates)
                .onSuccess {
                    _isSaving.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    _isSaving.value = false
                    onError(error.message ?: "Gagal memperbarui jadwal")
                }
        }
    }
}