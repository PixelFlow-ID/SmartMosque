package com.example.smartmosque.features.donation.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.model.PaymentMethod
import com.example.smartmosque.model.WaqfProject // <-- Pastikan import model Wakaf kamu sudah benar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class WaqfViewModel : ViewModel() {

    // --- STATE METODE PEMBAYARAN (INFAQ) ---
    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods

    private val _isLoadingMethods = MutableStateFlow(true)
    val isLoadingMethods: StateFlow<Boolean> = _isLoadingMethods

    // --- STATE DAFTAR PROGRAM WAKAF (YANG DIBUTUHKAN DONATION_SCREEN) ---
    private val _waqfProjects = MutableStateFlow<List<WaqfProject>>(emptyList())
    val waqfProjects: StateFlow<List<WaqfProject>> = _waqfProjects

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- STATE UNTUK PROSES UPLOAD BUKTI ---
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    init {
        fetchPaymentMethods()
        fetchWaqfProjects() // <-- Otomatis panggil data saat ViewModel aktif
    }

    // 1. Ambil Metode Pembayaran dari Firestore
    private fun fetchPaymentMethods() {
        viewModelScope.launch {
            _isLoadingMethods.value = true
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("payment_methods").get().await()
                _paymentMethods.value = snapshot.toObjects(PaymentMethod::class.java)
            } catch (e: Exception) {
                _paymentMethods.value = emptyList()
            } finally {
                _isLoadingMethods.value = false
            }
        }
    }

    // 2. Ambil Daftar Program Wakaf dari Firestore (Penyembuh Error di DonationScreen)
    fun fetchWaqfProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("waqf_programs").get().await()

                // Mapping dokumen Firestore ke Objek Model beserta ID Dokumennya
                val projects = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(WaqfProject::class.java)?.copy(id = doc.id)
                }
                _waqfProjects.value = projects
            } catch (e: Exception) {
                _waqfProjects.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 3. Proses Kompresi dan Kirim Bukti Infaq Jamaah
    fun submitInfaq(
        context: Context,
        imageUri: Uri,
        categoryName: String,
        amount: Long,
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.value = true
            try {
                // Kompresi gambar via ImageUtils
                val compressedFile = com.example.smartmosque.utils.ImageUtils.compressImage(context, imageUri)
                if (compressedFile == null) {
                    withContext(Dispatchers.Main) {
                        _isUploading.value = false
                        onError("Gagal melakukan kompresi gambar")
                    }
                    return@launch
                }

                // Upload File Hasil Kompresi ke Firebase Storage
                val compressedUri = Uri.fromFile(compressedFile)
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("proofs/infaq/${UUID.randomUUID()}.jpg")

                storageRef.putFile(compressedUri).await()
                val downloadUrl = storageRef.downloadUrl.await()

                // Simpan Payload Data ke Koleksi Donations Firestore
                val donationPayload = hashMapOf(
                    "type" to "INFAQ",
                    "category" to categoryName,
                    "amount" to amount,
                    "status" to "PENDING",
                    "date" to Timestamp.now(),
                    "userId" to userId,
                    "proofUrl" to downloadUrl.toString(),
                    "method" to "MANUAL"
                )

                FirebaseFirestore.getInstance().collection("donations").add(donationPayload).await()

                // Hapus file temporary lokal
                try { compressedFile.delete() } catch (e: Exception) {}

                withContext(Dispatchers.Main) {
                    _isUploading.value = false
                    onSuccess()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isUploading.value = false
                    onError(e.message ?: "Terjadi kesalahan sistem")
                }
            }
        }
    }

    // 4. Fungsi Hapus Program Wakaf Khusus Admin (Penyembuh Error di DonationScreen)
    fun deleteProject(projectId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance().collection("waqf_programs")
                    .document(projectId)
                    .delete()
                    .await()

                // Refresh data setelah berhasil dihapus agar UI terupdate otomatis
                fetchWaqfProjects()

                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Gagal menghapus program") }
            }
        }
    }
}