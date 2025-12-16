package com.example.smartmosque.model

import com.google.firebase.Timestamp

data class WaqfProject(
    // --- TAMBAHKAN BARIS INI (WAJIB) ---
    val id: String = "",
    // -----------------------------------

    val title: String = "",
    val description: String = "",
    val collectedAmount: Long = 0L,
    val targetAmount: Long = 0L,
    val imageUrl: String = "",
    val type: String = "Waqf",
    val status: String = "active",
    val createdAt: Timestamp? = null
){
    constructor() : this("", "", "", 0L, 0L, "")
}