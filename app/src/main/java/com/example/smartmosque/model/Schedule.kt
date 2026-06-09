package com.example.smartmosque.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Keep
data class Schedule(
    val id: String = "",
    val title: String = "",
    val speaker: String = "",
    val time: String = "",
    val location: String = "",
    val category: String = "Pengajian",
    val date: Timestamp? = null,
    val description: String = "",
    val isActive: Boolean = true,
    val streamingUrl: String = "",

    // --- SUMBER MASALAH ADA DI SINI ---
    // Pastikan di Firestore field ini tipe datanya ARRAY, bukan String.
    val participantsOnline: List<String> = emptyList(),
    val participantsOffline: List<String> = emptyList(),

    val reminderEnabled: Boolean = false,
    val reminderTime: Long = 0L,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val isPublished: Boolean = false,
    val isFinished: Boolean = false
)