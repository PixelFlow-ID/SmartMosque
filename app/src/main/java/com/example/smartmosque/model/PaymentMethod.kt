package com.example.smartmosque.model

import androidx.annotation.Keep

@Keep
data class PaymentMethod(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var accountNumber: String = "",
    var accountName: String = "",
    var logoUrl: String = "" //"bank atau qris"
)