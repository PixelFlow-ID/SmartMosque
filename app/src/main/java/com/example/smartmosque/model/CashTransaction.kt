package com.example.smartmosque.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class CashTransaction(
    val id: String = "",
    val title: String = "",          // Contoh: "Infaq Jumat", "Beli Lampu", dl
    val description: String = "",    // Detail opsional
    val amount: Long = 0L,           // Nominal
    val type: String = "INCOME",     // "INCOME" (Pemasukan) atau "EXPENSE" (Pengeluaran)
    val category: String = "Umum",   // "Operasional", "Pembangunan", "Sosial"
    val date: Timestamp? = null,
    val createdBy: String = ""       // ID Admin yang input
)