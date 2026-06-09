package com.example.smartmosque.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class CashTransaction(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var amount: Long = 0L,
    var type: String = "INCOME",
    var category: String = "Umum",
    var date: Timestamp? = null,
    var createdBy: String = ""
)