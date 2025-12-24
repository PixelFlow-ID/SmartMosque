package com.example.smartmosque.data.repository

import com.example.smartmosque.data.model.Schedule
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date


class ScheduleRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val scheduleCollection = firestore.collection("schedules")

    /**
     * Fetch all schedules sebagai Flow (real-time updates)
     */
    fun getAllSchedulesFlow(): Flow<List<Schedule>> = callbackFlow {
        val listener = scheduleCollection
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val schedules = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Schedule::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(schedules)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Fetch active schedules only
     * PERBAIKAN UTAMA ADA DI SINI
     */
    fun getActiveSchedulesFlow(): Flow<List<Schedule>> = callbackFlow {
        val listener = scheduleCollection
            .whereEqualTo("isFinished", false)
            .orderBy("date", Query.Direction.ASCENDING)


            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val schedules = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val item = doc.toObject(Schedule::class.java)?.copy(id = doc.id)
                        if (item != null && item.isFinished == false) {
                            item
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(schedules)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Fetch schedule by ID
     */
    suspend fun getScheduleById(scheduleId: String): Result<Schedule> {
        return try {
            val doc = scheduleCollection.document(scheduleId).get().await()
            val schedule = doc.toObject(Schedule::class.java)?.copy(id = doc.id)
            if (schedule != null) {
                Result.success(schedule)
            } else {
                Result.failure(Exception("Schedule not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add new schedule
     */
    suspend fun addSchedule(schedule: Schedule): Result<String> {
        return try {
            val docRef = scheduleCollection.add(schedule).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update existing schedule
     */
    suspend fun updateSchedule(scheduleId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            scheduleCollection.document(scheduleId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete schedule
     */
    suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        return try {
            scheduleCollection.document(scheduleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Join event (tambah user ke participant list)
     */
    suspend fun joinEvent(
        scheduleId: String,
        userId: String,
        isOnline: Boolean
    ): Result<Unit> {
        return try {
            val fieldName = if (isOnline) "participantsOnline" else "participantsOffline"
            scheduleCollection.document(scheduleId)
                .update(fieldName, com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leave event (hapus user dari participant list)
     */
    suspend fun leaveEvent(
        scheduleId: String,
        userId: String,
        isOnline: Boolean
    ): Result<Unit> {
        return try {
            val fieldName = if (isOnline) "participantsOnline" else "participantsOffline"
            scheduleCollection.document(scheduleId)
                .update(fieldName, com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get schedule statistics for current month (REALTIME FLOW)
     */
    fun getMonthlyStatsFlow(): Flow<Pair<Int, Int>> = callbackFlow {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = Timestamp(calendar.time)

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = Timestamp(calendar.time)

        val listener = scheduleCollection
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThan("date", endOfMonth)
            // .whereEqualTo("isFinished", true) // Bisa dihapus jika ingin menghitung yang belum selesai juga, tapi biasanya statistik itu untuk yang sudah selesai/terlaksana.
            // Namun user minta "kegiatan" dan "kehadiran", mungkin termasuk yang sedang berjalan?
            // Mari kita asumsikan SEMUA kegiatan bulan ini (selesai atau tidak).
            // Tapi biasanya logic awal filternya "isFinished = true". Kita pertahankan agar konsisten dengan logic "Laporan".
             .whereEqualTo("isFinished", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val eventCount = snapshot?.size() ?: 0
                var totalParticipants = 0

                snapshot?.documents?.forEach { doc ->
                    val onlineList = doc.get("participantsOnline") as? List<*>
                    val offlineList = doc.get("participantsOffline") as? List<*>
                    totalParticipants += (onlineList?.size ?: 0) + (offlineList?.size ?: 0)
                }

                trySend(Pair(eventCount, totalParticipants))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Listen for ongoing events (sedang berlangsung)
     * FIX: Check Date is Today!
     */
    fun listenForOngoingEvents(): Flow<Schedule?> = callbackFlow {
        val listener = scheduleCollection
            .whereEqualTo("isPublished", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val now = Date()
                val ongoingEvent = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val s = doc.toObject(Schedule::class.java)?.copy(id = doc.id)
                        if (s != null && s.isFinished == false) s else null
                    } catch (e: Exception) {
                        null
                    }
                }?.firstOrNull { schedule ->
                    schedule.date?.toDate()?.let { eventDate ->
                        // 1. Cek Apakah Hari Ini?
                        val isToday = android.text.format.DateUtils.isToday(eventDate.time)
                        
                        if (isToday) {
                            // 2. Cek Waktu (Mulai s/d 2 Jam setelahnya)
                            // Menggunakan timestamp comparison agar lebih akurat
                            val startTime = eventDate.time
                            val endTime = startTime + (2 * 60 * 60 * 1000) // +2 Jam
                            val nowTime = now.time
                            
                            nowTime >= startTime && nowTime <= endTime
                        } else {
                            false
                        }
                    } ?: false
                }

                trySend(ongoingEvent)
            }

        awaitClose { listener.remove() }
    }
}