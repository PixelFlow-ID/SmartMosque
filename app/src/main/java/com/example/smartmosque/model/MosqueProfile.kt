package com.example.smartmosque.model

data class MosqueProfile(
    val name: String = "",
    val location: String = "",
    val history: String = "",
    val establishedYear: String = "",
    val areaSize: String = "",
    val jamaahCapacity: String = "",
    val droneImageUrl: String = "",

    // Fasilitas
    val northTowerName: String = "",
    val northTowerUrl: String = "",
    val southTowerName: String = "",
    val southTowerUrl: String = "",
    val publicAreaName: String = "",
    val publicAreaDesc: String = "",
    val publicAreaUrl: String = ""
)