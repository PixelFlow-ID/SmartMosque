package com.example.smartmosque.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.model.MosqueProfile
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MosqueProfileViewModel : ViewModel() {

    private val _profile = MutableStateFlow<MosqueProfile?>(null)
    val profile: StateFlow<MosqueProfile?> = _profile

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            // Mengambil dokumen spesifik: mosque_info/profile
            Firebase.firestore.collection("mosque_info").document("profile")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        _isLoading.value = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.toObject(MosqueProfile::class.java)
                        _profile.value = data
                    }
                    _isLoading.value = false
                }
        }
    }
}