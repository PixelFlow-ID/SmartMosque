package com.example.smartmosque.features.donation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.model.WaqfProject
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WaqfViewModel : ViewModel() {

    private val _waqfProjects = MutableStateFlow<List<WaqfProject>>(emptyList())
    val waqfProjects: StateFlow<List<WaqfProject>> = _waqfProjects

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchWaqfProjects()
    }

    private fun fetchWaqfProjects() {
        viewModelScope.launch {
            // Pastikan nama collection SAMA dengan di database ("waqf_programs")
            Firebase.firestore.collection("waqf_programs")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        _isLoading.value = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                WaqfProject(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    description = doc.getString("description") ?: "",
                                    targetAmount = doc.getLong("targetAmount") ?: 0L,
                                    collectedAmount = doc.getLong("collectedAmount") ?: 0L,
                                    imageUrl = doc.getString("imageUrl") ?: ""
                                )
                            } catch (err: Exception) {
                                null
                            }
                        }
                        _waqfProjects.value = list
                        _isLoading.value = false
                    }
                }
        }
    }

    // Fungsi Hapus (Khusus Admin)
    fun deleteProject(projectId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Firebase.firestore.collection("waqf_programs").document(projectId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Gagal menghapus") }
    }
}