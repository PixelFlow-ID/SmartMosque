package com.example.smartmosque.model

data class PaymentMethod(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val accountNumber: String = "",
    val accountName: String = "",
    val logoUrl: String = ""
)