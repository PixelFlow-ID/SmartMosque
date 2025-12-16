package com.example.smartmosque.features.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smartmosque.features.auth.AuthViewModel

// --- IMPORT WARNA DARI TEMA ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // <--- SOLUSI AGAR TIDAK MERAH
@Composable
fun AboutMosqueScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: MosqueProfileViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = BgPremium,
        // Kita set insets ke 0 agar benar-benar full screen
        contentWindowInsets = WindowInsets(0.dp)
    ) { _ -> // Ganti paddingValues dengan _ (underscore) agar lebih rapi

        // Kita ignore paddingValues (_) agar gambar bisa tembus ke status bar (Immersive)
        Box(modifier = Modifier.fillMaxSize()) {

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldDeep)
                }
            } else if (profile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Gagal memuat profil masjid.", color = TextColorSecondary)
                }
            } else {
                val p = profile!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- 1. HERO SECTION (GAMBAR UTAMA) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        // Gambar Background
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(p.droneImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Drone View",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.1f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.8f)
                                        ),
                                        startY = 0f
                                    )
                                )
                        )

                        // Konten Teks di atas Gambar
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 24.dp, vertical = 40.dp)
                        ) {
                            // Badge Lokasi
                            Surface(
                                color = White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50),
                                border = null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(p.location, color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = p.name,
                                color = White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 34.sp
                            )
                        }
                    }

                    // --- 2. FLOATING STATISTICS CARD ---
                    Box(modifier = Modifier.padding(horizontal = 24.dp).offset(y = (-30).dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 20.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                PremiumStatItem(Icons.Default.CalendarToday, p.establishedYear, "Berdiri")
                                VerticalDivider()
                                PremiumStatItem(Icons.Default.Groups, p.jamaahCapacity, "Kapasitas")
                                VerticalDivider()
                                PremiumStatItem(Icons.Default.SquareFoot, p.areaSize, "Luas")
                            }
                        }
                    }

                    // --- 3. KONTEN BODY ---
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .offset(y = (-10).dp)
                    ) {

                        // SEJARAH
                        PremiumSectionTitle("Sejarah & Visi")
                        Text(
                            text = p.history,
                            fontSize = 15.sp,
                            color = TextColorSecondary,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Justify,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // FASILITAS
                        PremiumSectionTitle("Fasilitas Utama")
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            PremiumFacilityCard(
                                modifier = Modifier.weight(1f),
                                title = "Menara\nSelatan",
                                subtitle = p.southTowerName,
                                icon = Icons.Default.MeetingRoom,
                                imageUrl = p.southTowerUrl
                            )
                            PremiumFacilityCard(
                                modifier = Modifier.weight(1f),
                                title = "Menara\nUtara",
                                subtitle = p.northTowerName,
                                icon = Icons.Default.Book,
                                imageUrl = p.northTowerUrl
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // AREA PUBLIK
                        PremiumSectionTitle("Area Publik")
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = White)
                        ) {
                            Column {
                                Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current).data(p.publicAreaUrl).crossfade(true).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Badge
                                    Surface(
                                        modifier = Modifier.padding(16.dp).align(Alignment.TopEnd),
                                        shape = RoundedCornerShape(8.dp),
                                        color = White
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            // SUDAH DIPERBAIKI (Modifier.size)
                                            Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(12.dp), tint = EmeraldDeep)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Foodcourt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                                        }
                                    }
                                }
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = p.publicAreaName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = TextColorPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = p.publicAreaDesc,
                                        fontSize = 14.sp,
                                        color = TextColorSecondary,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }

            // --- 4. CUSTOM BACK BUTTON (OVERLAY) ---
            Box(
                modifier = Modifier
                    .padding(top = 40.dp, start = 20.dp) // Sesuaikan status bar
            ) {
                Surface(
                    onClick = { navController.popBackStack() },
                    shape = CircleShape,
                    color = White.copy(alpha = 0.2f),
                    contentColor = White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            }
        }
    }
}

// --- KOMPONEN UI PREMIUM ---

@Composable
fun PremiumSectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(EmeraldDeep)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextColorPrimary,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
fun PremiumStatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BgPremium),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = EmeraldDeep, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary)
        Text(label, fontSize = 11.sp, color = TextColorSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color.LightGray.copy(alpha = 0.4f))
    )
}

@Composable
fun PremiumFacilityCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    imageUrl: String
) {
    Card(
        modifier = modifier
            .height(220.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.9f))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = EmeraldDeep, modifier = Modifier.size(16.dp))
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColorPrimary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextColorSecondary,
                    maxLines = 1
                )
            }
        }
    }
}