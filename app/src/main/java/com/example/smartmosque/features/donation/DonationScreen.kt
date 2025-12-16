package com.example.smartmosque.features.donation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartmosque.features.auth.AuthState
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.model.WaqfProject
import com.example.smartmosque.ui.theme.Screen

// IMPORT WARNA TEMA (Pastikan sesuai dengan file Theme.kt Anda)
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.BackgroundLight
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.RedError
import com.example.smartmosque.ui.theme.BgPremium // Warna Background Soft (Cream/Abu muda)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: WaqfViewModel = viewModel()
) {
    val waqfList by viewModel.waqfProjects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user

    // Logika Admin
    val isAdmin = currentUser?.email == "ramdanidoni244@gmail.com"

    Scaffold(
        containerColor = BgPremium, // Background soft
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddWaqfProgram.route) },
                    containerColor = EmeraldDeep,
                    contentColor = White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Program")
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- 1. HEADER YANG LEBIH CLEAN ---
            DonationHeader()

            // --- 2. LIST CONTENT ---
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = EmeraldDeep)
                        }
                    }
                } else if (waqfList.isEmpty()) {
                    item {
                        EmptyStateDonation()
                    }
                } else {
                    items(waqfList) { project ->
                        PremiumWaqfCard(
                            project = project,
                            isAdmin = isAdmin,
                            onClick = { navController.navigate(Screen.createRoute(project.id)) },
                            onDelete = { viewModel.deleteProject(project.id, {}, {}) }
                        )
                    }
                    // Spacer bawah agar FAB tidak menutupi item terakhir
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// --- HEADER COMPONENT ---
@Composable
fun DonationHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                // Gradient halus di header agar terlihat menyatu
                Brush.verticalGradient(
                    colors = listOf(Color.White, BgPremium)
                )
            )
            .padding(top = 24.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = EmeraldDeep,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Mari Berwakaf",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextColorPrimary,
                letterSpacing = (-0.5).sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kekalkan hartamu dengan membangun rumah Allah dan fasilitas umat.",
            fontSize = 14.sp,
            color = TextColorSecondary,
            lineHeight = 20.sp
        )
    }
}

// --- CARD UTAMA (TAMPILAN BARU PREMIUM) ---
@Composable
fun PremiumWaqfCard(
    project: WaqfProject,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Animasi Progress Bar
    val progressTarget = if (project.targetAmount > 0) project.collectedAmount.toFloat() / project.targetAmount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    // Format Rupiah
    val formatRupiah = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    formatRupiah.maximumFractionDigits = 0
    val collectedStr = formatRupiah.format(project.collectedAmount)
    val targetStr = formatRupiah.format(project.targetAmount)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.05f) // Bayangan sangat halus
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White),
    ) {
        Column {
            // 1. GAMBAR (FULL WIDTH - HERO IMAGE)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // Gambar lebih besar agar imersif
            ) {
                if (project.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = project.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF0FDF4)), // Background hijau sangat muda
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, null, tint = EmeraldDeep.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    }
                }

                // Badge "Sedang Berjalan"
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(50),
                    color = White.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "Sedang Berjalan",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDeep,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Tombol Delete Admin (Overlay)
                if (isAdmin) {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                            .clickable { onDelete() },
                        shape = CircleShape,
                        color = White,
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = RedError,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                }
            }

            // 2. KONTEN DESKRIPSI
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = project.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColorPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar Custom yang Tebal dan Bulat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF1F5F9)) // Abu sangat muda
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(EmeraldDeep, GreenPrimary)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Info Angka
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Terkumpul", fontSize = 11.sp, color = TextColorSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = collectedStr,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDeep
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target", fontSize = 11.sp, color = TextColorSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = targetStr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextColorSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// --- STATE KOSONG ---
@Composable
fun EmptyStateDonation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Verified,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Belum ada program wakaf.",
            fontSize = 16.sp,
            color = TextColorSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}