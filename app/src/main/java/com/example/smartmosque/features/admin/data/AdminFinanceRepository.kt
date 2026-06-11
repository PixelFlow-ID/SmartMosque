package com.example.smartmosque.features.admin.data

import com.example.smartmosque.model.Donation
import com.example.smartmosque.model.CashTransaction
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminFinanceRepository {
    private val db = FirebaseFirestore.getInstance()

    // ==========================================
    // 1. BAGIAN KELOLA KAS MASJID (BARU DISINKRONKAN)
    // ==========================================

    // Aliran data real-time untuk daftar transaksi Kas Masjid
    fun getTransactionsFlow(): Flow<List<CashTransaction>> = callbackFlow {
        val listener = db.collection("cash_transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CashTransaction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Menambah data transaksi Kas Baru
    suspend fun addTransaction(transaction: CashTransaction): Result<Unit> {
        return try {
            db.collection("cash_transactions").add(transaction).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Memperbarui data transaksi Kas Lama
    suspend fun updateTransaction(id: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("cash_transactions").document(id).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 2. BAGIAN APPROVAL DONASI JEMAAH (PINDAHAN KEMARIN)
    // ==========================================

    suspend fun getUserProfile(userId: String): Result<DocumentSnapshot> {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    suspend fun approveDonation(donation: Donation): Result<Unit> {
        return try {
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
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectDonation(donationId: String): Result<Unit> {
        return try {
            db.collection("donations")
                .document(donationId)
                .update("status", "REJECTED")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}