package com.example.smartmosque.features.admin.data

import com.example.smartmosque.model.Schedule
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminScheduleRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val schedulesCollection = firestore.collection("schedules")

    // Menyimpan jadwal baru
    suspend fun addSchedule(scheduleData: Map<String, Any>): Result<Unit> {
        return try {
            schedulesCollection.add(scheduleData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mengambil jadwal berdasarkan ID
    suspend fun getScheduleById(id: String): Result<DocumentSnapshot> {
        return try {
            val snapshot = schedulesCollection.document(id).get().await()
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- TAMBAHAN UNTUK EDIT: Update Dokumen Berdasarkan ID ---
    suspend fun updateSchedule(id: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            schedulesCollection.document(id).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- SEKARANG SUDAH MASUK DI DALAM KELAS & MEMAKAI VARIABEL 'firestore' ---
    suspend fun fetchPublishedSchedules(): List<Schedule> {
        return try {
            val snapshot = firestore.collection("schedules") // PERBAIKAN: db diganti firestore
                .whereEqualTo("isPublished", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get().await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Schedule::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            // Fallback jika komposit indeks belum digenerate di Firebase Console
            val fallbackSnapshot = firestore.collection("schedules") // PERBAIKAN: db diganti firestore
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get().await()

            fallbackSnapshot.documents.mapNotNull { doc ->
                val data = doc.toObject(Schedule::class.java)?.copy(id = doc.id)
                if (data != null && data.isPublished) data else null
            }
        }
    }
} // <--- Kurung kurawal penutup kelas harus di paling bawah file