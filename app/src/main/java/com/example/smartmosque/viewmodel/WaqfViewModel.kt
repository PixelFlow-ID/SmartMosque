package com.example.smartmosque.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.data.model.Donation
import com.example.smartmosque.data.model.WaqfProject
import com.example.smartmosque.data.repository.WaqfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel untuk Waqf/Donation Screen
 * Mengikuti MVVM Pattern - ViewModel hanya mengelola UI state dan delegate ke Repository
 */
class WaqfViewModel : ViewModel() {

    // Repository
    private val waqfRepository = WaqfRepository()

    // ==================== STATE MANAGEMENT ====================
    
    private val _waqfProjects = MutableStateFlow<List<WaqfProject>>(emptyList())
    val waqfProjects: StateFlow<List<WaqfProject>> = _waqfProjects

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchWaqfProjects()
    }

    // ==================== WAQF PROJECT OPERATIONS ====================
    
    /**
     * Fetch all waqf projects
     */
    fun fetchWaqfProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            waqfRepository.getAllWaqfProjectsFlow()
                .catch { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                .collect { projects ->
                    _waqfProjects.value = projects
                    _isLoading.value = false
                }
        }
    }

    /**
     * Add new waqf project
     */
    fun addWaqfProject(
        project: WaqfProject,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            waqfRepository.addWaqfProject(project)
                .onSuccess { projectId -> onSuccess(projectId) }
                .onFailure { exception -> onError(exception.message ?: "Gagal menambah project") }
        }
    }

    /**
     * Delete waqf project (Admin only)
     */
    fun deleteProject(
        projectId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            waqfRepository.deleteWaqfProject(projectId)
                .onSuccess { onSuccess() }
                .onFailure { exception -> onError(exception.message ?: "Gagal menghapus project") }
        }
    }

    // ==================== DONATION OPERATIONS ====================
    
    /**
     * Add new donation
     */
    fun addDonation(
        donation: Donation,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            waqfRepository.addDonation(donation)
                .onSuccess { donationId -> onSuccess(donationId) }
                .onFailure { exception -> onError(exception.message ?: "Gagal menambah donasi") }
        }
    }

    /**
     * Update collected amount for a project (when donation is approved)
     */
    fun updateCollectedAmount(
        projectId: String,
        amount: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            waqfRepository.updateDonationCollectedAmount(projectId, amount)
                .onSuccess { onSuccess() }
                .onFailure { exception -> onError(exception.message ?: "Gagal update amount") }
        }
    }
}
