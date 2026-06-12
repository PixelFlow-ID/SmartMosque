package com.example.smartmosque.features.admin.data

import com.example.smartmosque.model.Donation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminWaqfRepository {

    private val db = FirebaseFirestore.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val waqfCollection = firestore.collection("waqf_programs")

    // Fungsi menyimpan program baru ke Firestore menggunakan Coroutines (suspend)
    suspend fun createWaqfProgram(
        title: String,
        description: String,
        imageUrl: String,
        targetAmount: Long,
        programType: String
    ) {
        val programData = hashMapOf(
            "title" to title,
            "description" to description,
            "imageUrl" to imageUrl,
            "targetAmount" to targetAmount,
            "collectedAmount" to 0L,
            "type" to programType,
            "status" to "active",
            "createdAt" to Timestamp.now()
        )

        db.collection("waqf_programs").add(programData).await()
    }

    suspend fun getWaqfById(id: String): Result<DocumentSnapshot> {
        return try {
            val snapshot = firestore.collection("waqf_programs").document(id).get().await()
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun updateWaqfProgram(
        id: String,
        title: String,
        description: String,
        imageUrl: String,
        targetAmount: Long,
        programType: String
    ) {
        val updateData = mapOf(
            "title" to title,
            "description" to description,
            "imageUrl" to imageUrl,
            "targetAmount" to targetAmount,
            "programType" to programType
        )
        waqfCollection.document(id).update(updateData).await()
    }


    // --- SEKARANG SUDAH MASUK DI DALAM KELAS (Bisa membaca variabel 'db') ---
    suspend fun fetchUserDonations(userId: String): Result<List<Donation>> {
        return try {
            val snapshot = db.collection("donations")
                .whereEqualTo("userId", userId)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get().await()

            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Donation::class.java)?.copy(id = doc.id)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}