package com.example.smartmosque.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties // <--- TAMBAHKAN INI (PENTING)
data class Donation(
    // ID dokumen kita biarkan kosong di constructor, nanti diisi manual
    var id: String = "",

    val amount: Long = 0,
    val category: String = "",
    val status: String = "",
    val proofUrl: String = "",
    val userId: String = "",
    val date: Timestamp? = null,
    val projectId: String = "",
    val type: String = ""
    // projectId dan method tidak perlu ditulis kalau tidak dipakai,
    // karena sudah ada @IgnoreExtraProperties
)