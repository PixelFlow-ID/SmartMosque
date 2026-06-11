package com.example.smartmosque.features.admin.presentation.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.features.admin.data.AdminFinanceRepository
import com.example.smartmosque.model.Donation
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminApprovalViewModel : ViewModel() {
    private val repository = AdminFinanceRepository()

    private val _pendingDonations = MutableStateFlow<List<Donation>>(emptyList())
    val pendingDonations: StateFlow<List<Donation>> = _pendingDonations

    private val _donorNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val donorNames: StateFlow<Map<String, String>> = _donorNames

    private var donationListener: ListenerRegistration? = null

    init {
        startListeningPendingDonations()
    }

    // Mengaktifkan stream real-time update dari Firestore
    private fun startListeningPendingDonations() {
        donationListener = repository.listenPendingDonations(
            onSuccess = { list ->
                _pendingDonations.value = list
            },
            onError = {
                // Handle error atau log di sini jika diperlukan
            }
        )
    }

    fun fetchDonorName(userId: String) {
        if (_donorNames.value.containsKey(userId) || userId.isEmpty()) return
        viewModelScope.launch {
            repository.getUserProfile(userId)
                .onSuccess { snapshot ->
                    val fullName = snapshot.getString("fullName") ?: "Hamba Allah"
                    _donorNames.value = _donorNames.value + (userId to fullName)
                }
        }
    }

    fun approveDonation(donation: Donation, onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.approveDonation(donation)
                .onSuccess { onResult("Donasi berhasil disetujui") }
                .onFailure { onResult("Gagal menyetujui: ${it.message}") }
        }
    }

    fun rejectDonation(donationId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.rejectDonation(donationId)
                .onSuccess { onResult("Donasi berhasil ditolak") }
                .onFailure { onResult("Gagal menolak: ${it.message}") }
        }
    }

    // Wajib dibersihkan saat ViewModel dihancurkan agar tidak terjadi memory leak (kebocoran RAM HP)
    override fun onCleared() {
        super.onCleared()
        donationListener?.remove()
    }
}