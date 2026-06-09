package com.example.smartmosque.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Keep
data class Donation(
    // ID dokumen kita biarkan kosong di constructor, nanti diisi manual
    var id: String = "",

    var amount: Long = 0,
    var category: String = "",
    var status: String = "",
    var proofUrl: String = "",
    var userId: String = "",
    var date: Timestamp? = null,
    var projectId: String = "",
    var type: String = ""
    // projectId dan method tidak perlu ditulis kalau tidak dipakai,
    // karena sudah ada @IgnoreExtraProperties
)