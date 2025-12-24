package com.example.smartmosque.data.repository

import com.example.smartmosque.data.model.MosqueProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk mengelola data Profil Masjid
 * Mengikuti Repository Pattern - Single Source of Truth untuk Mosque Profile operations
 */
class MosqueProfileRepository {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val profileCollection = firestore.collection("mosque_info")
    
    // Document ID tetap untuk profil masjid (hanya ada 1 profil)
    private val PROFILE_DOC_ID = "profile"

    /**
     * Fetch mosque profile sebagai Flow (real-time updates)
     */
    fun getMosqueProfileFlow(): Flow<MosqueProfile?> = callbackFlow {
        val listener = profileCollection.document(PROFILE_DOC_ID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val profile = try {
                    snapshot?.toObject(MosqueProfile::class.java)
                } catch (e: Exception) {
                    null
                }
                
                trySend(profile)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Fetch mosque profile (one-time fetch)
     */
    suspend fun getMosqueProfile(): Result<MosqueProfile> {
        return try {
            val doc = profileCollection.document(PROFILE_DOC_ID).get().await()
            val profile = doc.toObject(MosqueProfile::class.java)
            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(Exception("Mosque profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update mosque profile
     */
    suspend fun updateMosqueProfile(profile: MosqueProfile): Result<Unit> {
        return try {
            profileCollection.document(PROFILE_DOC_ID)
                .set(profile)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update specific fields of mosque profile
     */
    suspend fun updateMosqueProfileFields(updates: Map<String, Any>): Result<Unit> {
        return try {
            profileCollection.document(PROFILE_DOC_ID)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
