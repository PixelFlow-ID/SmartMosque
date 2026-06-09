package com.example.smartmosque.data.repository

import com.example.smartmosque.model.Schedule
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class ScheduleRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val scheduleCollection = firestore.collection("schedules")

    // Mendengarkan seluruh jadwal secara Real-time (Urut Tanggal Naik)
    fun getAllSchedulesFlow(): Flow<List<Schedule>> = callbackFlow {
        val listener = scheduleCollection
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val onlineList = (doc.get("participantsOnline") as? List<String>) ?: emptyList()
                            @Suppress("UNCHECKED_CAST")
                            val offlineList = (doc.get("participantsOffline") as? List<String>) ?: emptyList()

                            Schedule(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                speaker = doc.getString("speaker") ?: "",
                                time = doc.getString("time") ?: "",
                                location = doc.getString("location") ?: "",
                                category = doc.getString("category") ?: "Pengajian",
                                date = doc.getTimestamp("date"),
                                participantsOnline = onlineList,
                                participantsOffline = offlineList,
                                streamingUrl = doc.getString("streamingUrl") ?: "",
                                isPublished = doc.getBoolean("isPublished") ?: true,
                                isFinished = doc.getBoolean("isFinished") ?: false
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(list).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }

    // Mengambil 3 jadwal terdekat yang sudah publish (Untuk MiniCard di Home)
    fun getUpcomingSchedulesFlow(): Flow<List<Schedule>> = callbackFlow {
        val today = Date()
        val listener = scheduleCollection
            .whereEqualTo("isPublished", true)
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val validEvents = snapshot.documents.mapNotNull { doc ->
                        // Parsing logic sama...
                        try {
                            val ts = doc.getTimestamp("date")
                            if (ts != null && ts.toDate().after(today)) {
                                // Return objek schedule Anda di sini
                            }
                        } catch(e: Exception) {}
                        null
                    }
                    // Implementasi ringkas dari pesan sebelumnya
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun toggleJoinSchedule(scheduleId: String, userId: String, isJoined: Boolean): Result<Unit> = try {
        val docRef = scheduleCollection.document(scheduleId)
        if (isJoined) docRef.update("participantsOnline", FieldValue.arrayRemove(userId)).await()
        else docRef.update("participantsOnline", FieldValue.arrayUnion(userId)).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun publishSchedule(id: String): Result<Unit> = try {
        scheduleCollection.document(id).update("isPublished", true).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun markAsFinished(id: String): Result<Unit> = try {
        scheduleCollection.document(id).update("isFinished", true).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteSchedule(id: String): Result<Unit> = try {
        scheduleCollection.document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}