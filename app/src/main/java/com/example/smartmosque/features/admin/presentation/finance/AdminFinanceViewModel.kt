package com.example.smartmosque.features.admin.presentation.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.features.admin.data.AdminFinanceRepository
import com.example.smartmosque.model.CashTransaction
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AdminFinanceViewModel : ViewModel() {

    private val repository = AdminFinanceRepository()

    // --- TAMBAHAN STATES UNTUK MENYEMBUHKAN ERROR DI SCREEN ---
    private val _transactions = MutableStateFlow<List<CashTransaction>>(emptyList())
    val transactions: StateFlow<List<CashTransaction>> = _transactions

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    init {
        listenToTransactions() // Otomatis dengarkan database saat ViewModel Admin dihidupkan
    }

    private fun listenToTransactions() {
        viewModelScope.launch {
            repository.getTransactionsFlow()
                .catch { _transactions.value = emptyList() }
                .collect { list ->
                    _transactions.value = list
                }
        }
    }

    // Aksi Tambah Transaksi Kas
    fun addTransaction(
        title: String,
        description: String,
        amount: Long,
        type: String,
        category: String,
        date: Timestamp,
        createdBy: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val transaction = CashTransaction(
                title = title,
                description = description,
                amount = amount,
                type = type,
                category = category,
                date = date,
                createdBy = createdBy
            )
            repository.addTransaction(transaction)
                .onSuccess {
                    _isProcessing.value = false
                    onSuccess()
                }
                .onFailure {
                    _isProcessing.value = false
                    onError(it.message ?: "Gagal menambahkan data transaksi")
                }
        }
    }

    // Aksi Perbarui / Sunting Transaksi Kas
    fun updateTransaction(
        id: String,
        title: String,
        description: String,
        amount: Long,
        type: String,
        category: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val updates = mapOf(
                "title" to title,
                "description" to description,
                "amount" to amount,
                "type" to type,
                "category" to category
            )
            repository.updateTransaction(id, updates)
                .onSuccess {
                    _isProcessing.value = false
                    onSuccess()
                }
                .onFailure {
                    _isProcessing.value = false
                    onError(it.message ?: "Gagal memperbarui data transaksi")
                }
        }
    }
}