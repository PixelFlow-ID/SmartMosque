package com.example.smartmosque.features.donation.data

import android.net.Uri
import com.example.smartmosque.model.PaymentMethod
import com.example.smartmosque.model.WaqfProject
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DonationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Fungsi untuk mengambil daftar program wakaf
    fun getWaqfProjectsFlow(): Flow<List<WaqfProject>> = callbackFlow {
        val listener = db.collection("waqf_programs")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            WaqfProject(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                targetAmount = doc.getLong("targetAmount") ?: 0L,
                                collectedAmount = doc.getLong("collectedAmount") ?: 0L,
                                imageUrl = doc.getString("imageUrl") ?: ""
                            )
                        } catch (err: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // Fungsi untuk mengambil daftar metode pembayaran
    suspend fun getPaymentMethods(): List<PaymentMethod> {
        return db.collection("payment_methods").get().await().toObjects(PaymentMethod::class.java)
    }

    // Fungsi untuk mengunggah gambar bukti transfer
    suspend fun uploadProofImage(imageUri: Uri): String {
        val ref = storage.reference.child("proofs/infaq/${UUID.randomUUID()}.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    // Fungsi untuk menyimpan transaksi infaq
    suspend fun saveInfaqTransaction(categoryName: String, amount: Long, userId: String, proofUrl: String) {
        val data = hashMapOf(
            "type" to "INFAQ",
            "category" to categoryName,
            "amount" to amount,
            "status" to "PENDING",
            "date" to Timestamp.now(),
            "userId" to userId,
            "proofUrl" to proofUrl,
            "method" to "MANUAL"
        )
        db.collection("donations").add(data).await()
    }

// Fungsi untuk menghapus program wakaf
    suspend fun deleteWaqfProject(projectId: String) {
        db.collection("waqf_programs").document(projectId).delete().await()
    }
}