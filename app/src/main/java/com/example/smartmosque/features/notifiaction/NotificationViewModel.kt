package com.example.smartmosque.features.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.model.Donation
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.model.WaqfProject
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

// --- MODEL WRAPPER ---
sealed class JamaahNotificationItem {
    abstract val timestamp: Date
    data class DonationStatus(val donation: Donation) : JamaahNotificationItem() {
        override val timestamp: Date = donation.date?.toDate() ?: Date()
    }
    data class NewSchedule(val schedule: Schedule) : JamaahNotificationItem() {
        override val timestamp: Date = schedule.createdAt?.toDate() ?: Date()
    }
    data class NewWaqf(val waqf: WaqfProject) : JamaahNotificationItem() {
        override val timestamp: Date = waqf.createdAt?.toDate() ?: Date()
    }
}

class NotificationViewModel : ViewModel() {

    private val _pendingDonations = MutableStateFlow<List<Donation>>(emptyList())
    val pendingDonations: StateFlow<List<Donation>> = _pendingDonations

    private val _jamaahNotifications = MutableStateFlow<List<JamaahNotificationItem>>(emptyList())
    val jamaahNotifications: StateFlow<List<JamaahNotificationItem>> = _jamaahNotifications

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchPendingDonationsForAdmin()
    }

    // --- ADMIN: FETCH PENDING DONATIONS ---
    private fun fetchPendingDonationsForAdmin() {
        viewModelScope.launch {
            // HAPUS orderBy("date") agar tidak kena masalah Index Firestore
            FirebaseFirestore.getInstance().collection("donations")
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("AdminNotif", "Error fetch: ${e.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val d = doc.toObject(Donation::class.java)
                                // Pastikan ID terisi
                                d?.copy(id = doc.id)
                            } catch (err: Exception) {
                                Log.e("AdminNotif", "Gagal parse dokumen ${doc.id}: ${err.message}")
                                null
                            }
                        }
                        // Sorting Manual di Kotlin (Lebih Aman)
                        _pendingDonations.value = list.sortedByDescending { it.date }
                    }
                }
        }
    }

    // --- ADMIN: APPROVE DENGAN UPDATE SALDO (LOGIC UPGRADE) ---
    fun approveDonation(donation: Donation) {
        val db = FirebaseFirestore.getInstance()
        val donationRef = db.collection("donations").document(donation.id)

        db.runTransaction { transaction ->
            // 1. Ambil data snapshot terbaru untuk memastikan status belum berubah
            val snapshot = transaction.get(donationRef)
            val currentStatus = snapshot.getString("status")

            if (currentStatus == "APPROVED") {
                // Jika sudah diapprove sebelumnya, hentikan transaksi
                return@runTransaction
            }

            // 2. Update Status Donasi jadi APPROVED
            transaction.update(donationRef, "status", "APPROVED")

            // 3. Tambah Saldo ke Program Wakaf Terkait
            val programId = donation.projectId

            if (!programId.isNullOrEmpty()) {
                val projectRef = db.collection("waqf_programs").document(programId)
                // Gunakan FieldValue.increment agar penambahan saldo akurat (Atomic Operation)
                transaction.update(projectRef, "collectedAmount", FieldValue.increment(donation.amount))
            }
        }.addOnSuccessListener {
            Log.d("AdminAction", "Donasi berhasil disetujui dan saldo program bertambah.")
        }.addOnFailureListener { e ->
            Log.e("AdminAction", "Gagal melakukan approval: ${e.message}")
        }
    }

    // --- ADMIN: REJECT ---
    fun rejectDonation(donationId: String) {
        FirebaseFirestore.getInstance().collection("donations")
            .document(donationId)
            .update("status", "REJECTED")
    }

    // --- JAMAAH: FETCH NOTIFICATIONS (FULL FIX) ---
    fun fetchJamaahNotifications(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val combinedList = mutableListOf<JamaahNotificationItem>()
            val db = FirebaseFirestore.getInstance()

            try {
                // 1. Donasi User (Diurutkan Tanggal Terbaru)
                val donRes = db.collection("donations")
                    .whereEqualTo("userId", userId)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(10)
                    .get().await()

                donRes.documents.forEach { doc ->
                    val data = doc.toObject(Donation::class.java)?.copy(id = doc.id)
                    if (data != null) combinedList.add(JamaahNotificationItem.DonationStatus(data))
                }

                // 2. Jadwal Baru (Diurutkan CreatedAt Terbaru & Filter Draft)
                try {
                    // COBA CARA CEPAT: Filter langsung di Query
                    val schRes = db.collection("schedules")
                        .whereEqualTo("isPublished", true) // Hanya ambil yang published
                        .orderBy("createdAt", Query.Direction.DESCENDING) // Urutkan yang baru dibuat
                        .limit(5)
                        .get().await()

                    schRes.documents.forEach { doc ->
                        val data = doc.toObject(Schedule::class.java)?.copy(id = doc.id)
                        if (data != null) {
                            combinedList.add(JamaahNotificationItem.NewSchedule(data))
                        }
                    }
                } catch (e: Exception) {
                    // JIKA ERROR INDEX (Firestore belum diindex):
                    // Lakukan Fallback manual agar data tetap muncul
                    Log.w("NotifVM", "Index belum ada, melakukan fetch manual: ${e.message}")
                    val fallbackRes = db.collection("schedules")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(10) // Ambil lebih banyak untuk cadangan
                        .get().await()

                    fallbackRes.documents.forEach { doc ->
                        val data = doc.toObject(Schedule::class.java)?.copy(id = doc.id)
                        // Filter manual di Kotlin
                        if (data != null && data.isPublished) {
                            combinedList.add(JamaahNotificationItem.NewSchedule(data))
                        }
                    }
                }

                // 3. Wakaf Baru (Diurutkan CreatedAt Terbaru)
                val waqfRes = db.collection("waqf_programs")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(3)
                    .get().await()

                waqfRes.documents.forEach { doc ->
                    val data = doc.toObject(WaqfProject::class.java)?.copy(id = doc.id)
                    if (data != null) combinedList.add(JamaahNotificationItem.NewWaqf(data))
                }

                // Sort Gabungan Akhir (Terbaru di paling atas)
                _jamaahNotifications.value = combinedList.sortedByDescending { it.timestamp }
                _isLoading.value = false

            } catch (e: Exception) {
                Log.e("JamaahNotif", "Error General: ${e.message}")
                _isLoading.value = false
            }
        }
    }
}