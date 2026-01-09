package com.example.smartmosque.features.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.model.CashTransaction
import com.example.smartmosque.data.repository.FinanceRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class FinanceViewModel : ViewModel() {

    private val repository = FinanceRepository()

    // --- STATES ---
    private val _transactions = MutableStateFlow<List<CashTransaction>>(emptyList())
    val transactions: StateFlow<List<CashTransaction>> = _transactions

    private val _totalIncome = MutableStateFlow(0L)
    val totalIncome: StateFlow<Long> = _totalIncome

    private val _totalExpense = MutableStateFlow(0L)
    val totalExpense: StateFlow<Long> = _totalExpense

    private val _currentBalance = MutableStateFlow(0L)
    val currentBalance: StateFlow<Long> = _currentBalance

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        listenToTransactions()
    }

    private fun listenToTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTransactionsFlow()
                .catch { _isLoading.value = false }
                .collect { list ->
                    _transactions.value = list
                    calculateStats(list)
                    _isLoading.value = false
                }
        }
    }

    private fun calculateStats(list: List<CashTransaction>) {
        var income = 0L
        var expense = 0L

        list.forEach { item ->
            if (item.type == "INCOME") income += item.amount
            else expense += item.amount
        }

        _totalIncome.value = income
        _totalExpense.value = expense
        _currentBalance.value = income - expense
    }

    // --- ACTIONS ---

    fun addTransaction(
        title: String,
        description: String,
        amount: Long,
        type: String, // INCOME / EXPENSE
        category: String, // Operasional / Pembangunan
        date: Timestamp,
        createdBy: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
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
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Gagal") }
        }
    }

    fun deleteTransaction(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Gagal") }
        }
    }

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
            val updates = mapOf(
                "title" to title,
                "description" to description,
                "amount" to amount,
                "type" to type,
                "category" to category
            )
            repository.updateTransaction(id, updates)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Gagal") }
        }
    }
}

