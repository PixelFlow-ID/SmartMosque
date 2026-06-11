package com.example.smartmosque.features.home.data

import com.example.smartmosque.model.Schedule
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class HomeRepository {

    private val db = FirebaseFirestore.getInstance()

    // --- 1. AMBIL JUMLAH KEGIATAN BULAN INI (SUSPEND) ---
    suspend fun getEventsThisMonthCount(): Int {
        return try {
            // Mengambil awal bulan sekarang
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfMonth = calendar.time

            val snapshot = db.collection("schedules")
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .get()
                .await()

            snapshot.size()
        } catch (e: Exception) {
            0 // Kembalikan 0 jika gagal/error
        }
    }

    // --- 2. AMBIL TOTAL KEHADIRAN/PARTISIPAN (SUSPEND) ---
    suspend fun getTotalParticipantsCount(): Int {
        return try {
            // Opsi A: Mengambil dokumen agregasi statis dari Firestore (Lebih hemat kuota)
            val doc = db.collection("stats").document("masjid_stats").get().await()
            val total = doc.getLong("totalParticipants")?.toInt() ?: 0

            if (total == 0) {
                // Opsi B: Fallback menghitung manual jika dokumen agregasi belum ada
                val snapshot = db.collection("schedules").get().await()
                snapshot.documents.sumOf { it.getLong("participantsCount")?.toInt() ?: 0 }
            } else {
                total
            }
        } catch (e: Exception) {
            0
        }
    }

    // --- 3. STREAM NOTIFIKASI YANG BELUM DIBACA (REALTIME FLOW) ---
    fun getUnreadNotificationsFlow(): Flow<Boolean> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Jika ada dokumen yang isRead == false, tandanya ada notif belum dibaca
                val hasUnread = snapshot != null && !snapshot.isEmpty
                trySend(hasUnread)
            }

        // Otomatis menghapus listener Firestore jika coroutine dibatalkan/UI ditutup
        awaitClose { listener.remove() }
    }

    // --- 4. STREAM JADWAL YANG SEDANG LIVE (REALTIME FLOW) ---
    fun getOngoingEventFlow(): Flow<Schedule?> = callbackFlow {
        val listener = db.collection("schedules")
            .whereEqualTo("isLive", true) // Pastikan di Firestore ada flag isLive (boolean)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val schedule = snapshot?.documents?.firstOrNull()?.let { doc ->
                    doc.toObject(Schedule::class.java)?.copy(id = doc.id)
                }
                trySend(schedule)
            }

        awaitClose { listener.remove() }
    }

    // --- 5. AKSI: TANDAI SEMUA NOTIFIKASI SUDAH DIBACA (SUSPEND) ---
    suspend fun markAllNotificationsAsRead() {
        val unreadSnapshot = db.collection("notifications")
            .whereEqualTo("isRead", false)
            .get()
            .await()

        if (!unreadSnapshot.isEmpty) {
            val batch = db.batch()
            for (document in unreadSnapshot.documents) {
                val docRef = db.collection("notifications").document(document.id)
                batch.update(docRef, "isRead", true)
            }
            batch.commit().await()
        }
    }
}