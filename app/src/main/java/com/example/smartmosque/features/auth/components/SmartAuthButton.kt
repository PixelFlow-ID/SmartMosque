package com.example.smartmosque.features.auth.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- IMPORT WARNA DARI THEME (Single Source) ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.White

@Composable
fun SmartAuthButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Gunakan variable dari Theme sebagai default
    containerColor: Color = GreenPrimary,
    contentColor: Color = White
) {
    // 1. Deteksi apakah tombol sedang ditekan
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 2. Animasi Scale (Mengecil saat ditekan)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // Tinggi sedikit lebih besar agar gagah
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp), // Sudut lebih membulat (Modern)
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.7f),
            disabledContentColor = contentColor.copy(alpha = 0.8f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp, // Bayangan tebal saat diam
            pressedElevation = 2.dp  // Bayangan tipis saat ditekan
        ),
        interactionSource = interactionSource,
        enabled = !isLoading
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Konten saat Loading (Spinner)
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = contentColor,
                    strokeWidth = 3.dp
                )
            } else {
                // Konten Normal (Teks)
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}