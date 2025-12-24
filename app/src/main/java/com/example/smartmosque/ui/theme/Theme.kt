package com.example.smartmosque.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SmartMosqueColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = White,
    secondary = GreenDark,
    onSecondary = White,
    tertiary = GreenLight,

    // --- KUNCI PERBAIKAN TAMPILAN ---
    background = BackgroundLight, // Latar belakang layar jadi Abu-abu Muda
    onBackground = TextColorPrimary, // Teks di latar belakang jadi Hitam

    surface = White, // Kartu/Card jadi Putih Bersih
    onSurface = TextColorPrimary, // Teks di atas kartu jadi Hitam

    error = RedError,
    onError = White
)

@Composable
fun SmartMosqueTheme(
    darkTheme: Boolean = false, // Paksa Light Mode
    dynamicColor: Boolean = false, // Matikan warna wallpaper HP
    content: @Composable () -> Unit
) {
    val colorScheme = SmartMosqueColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar Putih agar bersih
            window.statusBarColor = Color.White.toArgb()
            // Ikon status bar Hitam
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
