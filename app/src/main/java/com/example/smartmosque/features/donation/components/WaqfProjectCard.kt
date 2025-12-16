package com.example.smartmosque.features.donation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartmosque.features.donation.WaqfViewModel
import com.example.smartmosque.ui.theme.Screen

// --- IMPORT WARNA DARI THEME ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.GreenLight
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.GrayInputBackground

@Composable
fun WaqfProjectCard(
    navController: NavController,
    // Inject ViewModel (PENTING)
    viewModel: WaqfViewModel = viewModel()
) {
    // Ambil daftar wakaf secara realtime
    val waqfList by viewModel.waqfProjects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Ambil 1 data terbaru
    val latestProject = waqfList.firstOrNull()

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Program Wakaf",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextColorPrimary
            )
            Text(
                text = "Lihat Semua",
                fontSize = 12.sp,
                color = GreenPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { navController.navigate(Screen.Donation.route) }
            )
        }

        // --- KONDISI DATA ---
        if (isLoading) {
            // Tampilan Loading
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(GrayInputBackground, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GreenPrimary)
            }
        } else if (latestProject != null) {
            // ADA DATA -> TAMPILKAN KARTU
            val progress = if (latestProject.targetAmount > 0) {
                latestProject.collectedAmount.toFloat() / latestProject.targetAmount.toFloat()
            } else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable {
                        navController.navigate(Screen.createRoute(latestProject.id))
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Gambar Kiri (Placeholder Icon)
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .fillMaxHeight()
                            .background(GreenLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Info Kanan
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = latestProject.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextColorPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = GreenPrimary,
                            trackColor = GreenLight,
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                navController.navigate(Screen.createRoute(latestProject.id))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Wakaf Sekarang", fontSize = 12.sp, color = White)
                        }
                    }
                }
            }
        } else {
            // TIDAK ADA DATA -> TAMPILKAN INFO KOSONG
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GrayInputBackground)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada program wakaf aktif",
                        color = TextColorSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}