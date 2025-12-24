package com.example.smartmosque.data.model

data class PaymentMethod(
    val name: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val logoUrl: String = "",
    val type: String = "" // "BANK" atau "QRIS"
)
