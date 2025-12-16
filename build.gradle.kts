plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false // Pastikan ini ada jika pakai firebase

    // --- TAMBAHKAN BARIS INI ---
    id("com.google.dagger.hilt.android") version "2.48" apply false
}