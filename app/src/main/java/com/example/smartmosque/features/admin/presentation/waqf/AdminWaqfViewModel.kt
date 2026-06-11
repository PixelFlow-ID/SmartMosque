package com.example.smartmosque.features.admin.presentation.waqf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.features.admin.data.AdminWaqfRepository
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
        // Validasi input di level ViewModel (Bukan di UI lagi)
        when {
            title.isBlank() -> { onError("Judul wajib diisi"); return }
            description.isBlank() -> { onError("Deskripsi wajib diisi"); return }
            targetAmountStr.isBlank() -> { onError("Target dana wajib diisi"); return }
        }

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
}