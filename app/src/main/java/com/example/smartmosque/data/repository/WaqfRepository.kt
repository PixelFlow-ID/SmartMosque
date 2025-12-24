package com.example.smartmosque.data.repository

import com.example.smartmosque.data.model.Donation
import com.example.smartmosque.data.model.WaqfProject
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk mengelola semua operasi Waqf dan Donation
 * Mengikuti Repository Pattern - Single Source of Truth untuk Waqf/Donation operations
 */
class WaqfRepository {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val waqfCollection = firestore.collection("waqf_programs")
    private val donationCollection = firestore.collection("donations")

    // ==================== WAQF PROJECT OPERATIONS ====================
    
    /**
     * Fetch all waqf projects sebagai Flow (real-time updates)
     */
    fun getAllWaqfProjectsFlow(): Flow<List<WaqfProject>> = callbackFlow {
        val listener = waqfCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val projects = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        WaqfProject(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            targetAmount = doc.getLong("targetAmount") ?: 0L,
                            collectedAmount = doc.getLong("collectedAmount") ?: 0L,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            type = doc.getString("type") ?: "Waqf",
                            status = doc.getString("status") ?: "active",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(projects)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Fetch active waqf projects only
     */
    fun getActiveWaqfProjectsFlow(): Flow<List<WaqfProject>> = callbackFlow {
        val listener = waqfCollection
            .whereEqualTo("status", "active")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val projects = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        WaqfProject(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            targetAmount = doc.getLong("targetAmount") ?: 0L,
                            collectedAmount = doc.getLong("collectedAmount") ?: 0L,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            type = doc.getString("type") ?: "Waqf",
                            status = doc.getString("status") ?: "active",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(projects)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Fetch waqf project by ID
     */
    suspend fun getWaqfProjectById(projectId: String): Result<WaqfProject> {
        return try {
            val doc = waqfCollection.document(projectId).get().await()
            val project = WaqfProject(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                targetAmount = doc.getLong("targetAmount") ?: 0L,
                collectedAmount = doc.getLong("collectedAmount") ?: 0L,
                imageUrl = doc.getString("imageUrl") ?: "",
                type = doc.getString("type") ?: "Waqf",
                status = doc.getString("status") ?: "active",
                createdAt = doc.getTimestamp("createdAt")
            )
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add new waqf project
     */
    suspend fun addWaqfProject(project: WaqfProject): Result<String> {
        return try {
            val docRef = waqfCollection.add(project).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update waqf project
     */
    suspend fun updateWaqfProject(projectId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            waqfCollection.document(projectId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete waqf project
     */
    suspend fun deleteWaqfProject(projectId: String): Result<Unit> {
        return try {
            waqfCollection.document(projectId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== DONATION OPERATIONS ====================
    
    /**
     * Add new donation
     */
    suspend fun addDonation(donation: Donation): Result<String> {
        return try {
            val docRef = donationCollection.add(donation).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update donation collected amount
     */
    suspend fun updateDonationCollectedAmount(
        projectId: String,
        additionalAmount: Long
    ): Result<Unit> {
        return try {
            waqfCollection.document(projectId)
                .update("collectedAmount", com.google.firebase.firestore.FieldValue.increment(additionalAmount))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user donations
     */
    fun getUserDonationsFlow(userId: String): Flow<List<Donation>> = callbackFlow {
        val listener = donationCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val donations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Donation::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(donations)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get all donations (for admin)
     */
    fun getAllDonationsFlow(): Flow<List<Donation>> = callbackFlow {
        val listener = donationCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val donations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Donation::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(donations)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get pending donations (for admin validation)
     */
    fun getPendingDonationsFlow(): Flow<List<Donation>> = callbackFlow {
        val listener = donationCollection
            .whereEqualTo("status", "pending")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val donations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Donation::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(donations)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Update donation status (untuk admin approval - UPDATE: Transactional)
     */
    suspend fun approveDonation(donation: Donation): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val donationRef = donationCollection.document(donation.id)
                
                // 1. Ambil data snapshot terbaru untuk memastikan status belum berubah
                val snapshot = transaction.get(donationRef)
                val currentStatus = snapshot.getString("status")

                if (currentStatus == "APPROVED") {
                    // Jika sudah diapprove sebelumnya, return null/void
                    return@runTransaction
                }

                // 2. Update Status Donasi jadi APPROVED
                transaction.update(donationRef, "status", "APPROVED")

                // 3. Tambah Saldo ke Program Wakaf Terkait
                val programId = donation.projectId
                if (programId.isNotEmpty()) {
                    val projectRef = waqfCollection.document(programId)
                    // Gunakan FieldValue.increment agar penambahan saldo akurat (Atomic Operation)
                    transaction.update(projectRef, "collectedAmount", com.google.firebase.firestore.FieldValue.increment(donation.amount))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reject donation
     */
    suspend fun rejectDonation(donationId: String): Result<Unit> {
        return try {
            donationCollection.document(donationId)
                .update("status", "REJECTED")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
