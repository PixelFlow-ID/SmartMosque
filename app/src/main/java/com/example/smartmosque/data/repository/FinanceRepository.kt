package com.example.smartmosque.data.repository

import com.example.smartmosque.model.CashTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FinanceRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val financeCollection = firestore.collection("finance_transactions")

    /**
     * Get Realtime Transactions (Urutkan dari yang terbaru)
     */
    fun getTransactionsFlow(): Flow<List<CashTransaction>> = callbackFlow {
        val listener = financeCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(CashTransaction::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(transactions)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Add Transaction
     */
    suspend fun addTransaction(transaction: CashTransaction): Result<String> {
        return try {
            val docRef = financeCollection.add(transaction).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update Transaction
     */
    suspend fun updateTransaction(transactionId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            financeCollection.document(transactionId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete Transaction
     */
    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            financeCollection.document(transactionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
