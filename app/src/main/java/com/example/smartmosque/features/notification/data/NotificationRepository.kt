package com.example.smartmosque.features.notification.data

import com.example.smartmosque.model.Donation
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.model.WaqfProject
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NotificationRepository {

    private val db = FirebaseFirestore.getInstance()

    // --- ADMIN: LISTEN PENDING DONATIONS (REALTIME STREAM) ---
    fun listenPendingDonations(
        onSuccess: (List<Donation>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("donations")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Donation::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onSuccess(list)
            }
    }

    // --- ADMIN: APPROVE WITH TRANSACTION ---
    suspend fun approveDonation(donation: Donation) {
        val donationRef = db.collection("donations").document(donation.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(donationRef)
            val currentStatus = snapshot.getString("status")

            if (currentStatus == "APPROVED") return@runTransaction

            transaction.update(donationRef, "status", "APPROVED")

            val programId = donation.projectId
            if (!programId.isNullOrEmpty()) {
                val projectRef = db.collection("waqf_programs").document(programId)
                transaction.update(projectRef, "collectedAmount", FieldValue.increment(donation.amount))
            }
        }.await() // Menggunakan coroutines await
    }

    // --- ADMIN: REJECT ---
    suspend fun rejectDonation(donationId: String) {
        db.collection("donations")
            .document(donationId)
            .update("status", "REJECTED")
            .await()
    }

    // --- JAMAAH: FETCH USER DONATIONS ---
    suspend fun fetchUserDonations(userId: String): List<Donation> {
        val snapshot = db.collection("donations")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(10)
            .get().await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Donation::class.java)?.copy(id = doc.id)
        }
    }

    // --- JAMAAH: FETCH PUBLISHED SCHEDULES ---
    suspend fun fetchPublishedSchedules(): List<Schedule> {
        return try {
            val snapshot = db.collection("schedules")
                .whereEqualTo("isPublished", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
                .get().await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Schedule::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            // Fallback manual jika index Firestore belum siap
            val fallbackSnapshot = db.collection("schedules")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get().await()

            fallbackSnapshot.documents.mapNotNull { doc ->
                val data = doc.toObject(Schedule::class.java)?.copy(id = doc.id)
                if (data != null && data.isPublished) data else null
            }
        }
    }

    // --- JAMAAH: FETCH WAQF PROJECTS ---
    suspend fun fetchWaqfProjects(): List<WaqfProject> {
        val snapshot = db.collection("waqf_programs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(3)
            .get().await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(WaqfProject::class.java)?.copy(id = doc.id)
        }
    }

    // --- JAMAAH: FETCH SYSTEM NOTIFICATIONS ---
    suspend fun fetchSystemNotifications(): List<com.google.firebase.firestore.DocumentSnapshot> {
        val snapshot = db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get().await()
        return snapshot.documents
    }
}