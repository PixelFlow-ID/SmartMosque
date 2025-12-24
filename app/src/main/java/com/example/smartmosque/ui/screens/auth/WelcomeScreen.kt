package com.example.smartmosque.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartmosque.ui.theme.Screen
import com.example.smartmosque.viewmodel.AuthViewModel
import com.example.smartmosque.viewmodel.AuthState

// --- IMPORT WARNA DARI TEMA ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.White

@Composable
fun WelcomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user

    // Ambil Nama User
    val displayName = remember(currentUser) {
        val name = currentUser?.displayName
        if (!name.isNullOrBlank()) {
            name
        } else {
            // Jika nama kosong, ambil dari email (sebelum @)
            currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                ?: "Jamaah"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White) // Pastikan background putih bersih
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Teks Sapaan Personal
        Text(
            text = "Assalamualaikum,\n$displayName 👋",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary, // Dari Theme
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Senang bertemu Anda kembali. Mari lanjutkan ibadah dan kegiatan masjid.",
            fontSize = 16.sp,
            color = TextColorSecondary, // Menggantikan Color.Gray
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Tombol Lanjut ke Home
        Button(
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true } // Hapus history agar tidak bisa back
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary) // Dari Theme
        ) {
            Text(
                text = "Masuk ke Dashboard",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White // Pastikan teks tombol putih
            )
        }
    }
}
