package com.example.smartmosque.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.data.model.MosqueProfile
import com.example.smartmosque.data.repository.MosqueProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel untuk Mosque Profile Screen
 * Mengikuti MVVM Pattern - ViewModel hanya mengelola UI state dan delegate ke Repository
 */
class MosqueProfileViewModel : ViewModel() {

    // Repository
    private val mosqueProfileRepository = MosqueProfileRepository()

    // ==================== STATE MANAGEMENT ====================
    
    private val _profile = MutableStateFlow<MosqueProfile?>(null)
    val profile: StateFlow<MosqueProfile?> = _profile

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchProfile()
    }

    // ==================== PROFILE OPERATIONS ====================
    
    /**
     * Fetch mosque profile
     */
    private fun fetchProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            mosqueProfileRepository.getMosqueProfileFlow()
                .catch { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                .collect { profile ->
                    _profile.value = profile
                    _isLoading.value = false
                }
        }
    }

    /**
     * Update mosque profile
     */
    fun updateProfile(
        profile: MosqueProfile,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            mosqueProfileRepository.updateMosqueProfile(profile)
                .onSuccess { onSuccess() }
                .onFailure { exception -> onError(exception.message ?: "Gagal update profil") }
        }
    }
}
