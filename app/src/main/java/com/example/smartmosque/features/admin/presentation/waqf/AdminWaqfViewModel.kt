package com.example.smartmosque.features.admin.presentation.waqf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.features.admin.data.AdminWaqfRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val repository = AdminWaqfRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun publishProgram(
        title: String,
        description: String,
        imageUrl: String,
        targetAmountStr: String,
        programType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!validateInput(title, description, targetAmountStr, onError)) return

        val targetAmount = targetAmountStr.toLongOrNull() ?: 0L

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createWaqfProgram(
                    title = title,
                    description = description,
                    imageUrl = imageUrl,
                    targetAmount = targetAmount,
                    programType = programType
                )
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                onError(e.message ?: "Gagal menerbitkan program")
            }
        }
    }

    // --- TAMBAHAN LOGIKA: EDIT PROGRAM WAKAF (KHUSUS ADMIN) ---
    fun updateProgram(
        id: String,
        title: String,
        description: String,
        imageUrl: String,
        targetAmountStr: String,
        programType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (id.isBlank()) { onError("ID Program tidak valid"); return }
        if (!validateInput(title, description, targetAmountStr, onError)) return

        val targetAmount = targetAmountStr.toLongOrNull() ?: 0L

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateWaqfProgram(
                    id = id,
                    title = title,
                    description = description,
                    imageUrl = imageUrl,
                    targetAmount = targetAmount,
                    programType = programType
                )
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                onError(e.message ?: "Gagal memperbarui program")
            }
        }
    }

    fun fetchWaqfDetail(
        id: String,
        onSuccess: (DocumentSnapshot) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getWaqfById(id)
                .onSuccess {
                    _isLoading.value = false
                    onSuccess(it)
                }
                .onFailure {
                    _isLoading.value = false
                    onError(it.message ?: "Gagal mengambil data")
                }
        }
    }

    // Reusable helper validation function
    private fun validateInput(title: String, description: String, targetAmountStr: String, onError: (String) -> Unit): Boolean {
        return when {
            title.isBlank() -> { onError("Judul wajib diisi"); false }
            description.isBlank() -> { onError("Deskripsi wajib diisi"); false }
            targetAmountStr.isBlank() -> { onError("Target dana wajib diisi"); false }
            else -> true
        }
    }
}