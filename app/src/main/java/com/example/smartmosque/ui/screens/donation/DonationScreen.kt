package com.example.smartmosque.ui.screens.donation

// --- IMPORT WARNA TEMA ---
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartmosque.data.model.WaqfProject
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.RedError
import com.example.smartmosque.ui.theme.Screen
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.viewmodel.AuthState
import com.example.smartmosque.viewmodel.AuthViewModel
import com.example.smartmosque.viewmodel.WaqfViewModel

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

    // Logika Admin (Tetap Sesuai Permintaan)
    val isAdmin = currentUser?.email == "ramdanidoni244@gmail.com"

    Scaffold(
        containerColor = BgPremium,
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
            // --- HEADER BERSIH & ELEGANT ---
            CleanDonationHeader()

            // --- LIST KONTEN ---
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
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
                    // Spacer agar FAB tidak menutupi item terakhir
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// --- KOMPONEN HEADER ---
@Composable
fun CleanDonationHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Watermark Icon di Kanan (Sangat Samar & Elegan)
        Icon(
            imageVector = Icons.Outlined.VolunteerActivism,
            contentDescription = null,
            tint = EmeraldDeep.copy(alpha = 0.05f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(110.dp)
                .offset(x = 20.dp, y = 10.dp)
                .rotate(-10f)
        )

        Column {
            Text(
                text = "INVESTASI AKHIRAT",
                fontSize = 11.sp,
                color = EmeraldDeep,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Program Wakaf",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextColorPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Salurkan harta terbaikmu untuk\nkebaikan yang mengalir abadi.",
                fontSize = 14.sp,
                color = TextColorSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

// --- KOMPONEN CARD UTAMA ---
@Composable
fun PremiumWaqfCard(
    project: WaqfProject,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Hitung Progress Persen
    val progressRaw = if (project.targetAmount > 0) project.collectedAmount.toDouble() / project.targetAmount.toDouble() else 0.0
    val progressPercent = (progressRaw * 100).toInt().coerceIn(0, 100)

    // Format Rupiah
    val formatRupiah = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    formatRupiah.maximumFractionDigits = 0
    val collectedStr = formatRupiah.format(project.collectedAmount)
    val targetStr = formatRupiah.format(project.targetAmount)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
    ) {
        Column {
            // 1. IMAGE HERO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
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
                            .background(Color(0xFFF0FDF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, null, tint = EmeraldDeep.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    }
                }

                // Badge Percent (Floating di atas gambar)
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomEnd),
                    shape = RoundedCornerShape(12.dp),
                    color = White,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$progressPercent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDeep
                        )
                    }
                }

                // Tombol Delete Admin
                if (isAdmin) {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                            .clickable { onDelete() },
                        shape = CircleShape,
                        color = White.copy(alpha = 0.9f),
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

            // 2. KONTEN INFO
            Column(modifier = Modifier.padding(20.dp)) {
                // Label Status
                Text(
                    text = "SEDANG BERJALAN",
                    fontSize = 10.sp,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Judul
                Text(
                    text = project.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColorPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- PROGRESS BAR DENGAN ANIMASI SHIMMER ---
                ShimmerProgressBar(
                    currentAmount = project.collectedAmount.toDouble(),
                    targetAmount = project.targetAmount.toDouble()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Angka Donasi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Terkumpul", fontSize = 11.sp, color = TextColorSecondary)
                        Text(
                            text = collectedStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColorPrimary
                        )
                    }

                    // Garis Pemisah Kecil
                    Box(modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f)))

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target", fontSize = 11.sp, color = TextColorSecondary)
                        Text(
                            text = targetStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColorSecondary
                        )
                    }
                }
            }
        }
    }
}

// --- KOMPONEN ANIMASI PROGRESS BAR (SHIMMER) ---
@Composable
fun ShimmerProgressBar(
    currentAmount: Double,
    targetAmount: Double,
    modifier: Modifier = Modifier
) {
    // Hitung progress 0.0 - 1.0
    val progressRaw = if (targetAmount > 0) currentAmount / targetAmount else 0.0
    val progressClamped = progressRaw.toFloat().coerceIn(0f, 1f)

    // Animasi 'Isi' Bar saat pertama muncul
    val animatedProgress by animateFloatAsState(
        targetValue = progressClamped,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "fill"
    )

    // Animasi 'Kilau' (Shimmer) bergerak
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_move"
    )

    // Warna gradient shimmer (Hijau Tua -> Hijau Terang -> Hijau Tua)
    val shimmerColors = listOf(
        EmeraldDeep,
        Color(0xFF4ADE80), // Lime Green cerah
        EmeraldDeep
    )

    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(shimmerTranslate - 300f, 0f),
        end = Offset(shimmerTranslate, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE2E8F0)) // Background track abu-abu
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(brush = shimmerBrush) // Gunakan brush animasi
        )
    }
}

// --- TAMPILAN KOSONG ---
@Composable
fun EmptyStateDonation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.VolunteerActivism,
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
