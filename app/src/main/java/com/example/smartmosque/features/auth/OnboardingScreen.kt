package com.example.smartmosque.features.auth

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// Warna Tema Baru ---
import com.example.smartmosque.ui.theme.Screen
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.EmeraldLight
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorSecondary

// Model Data
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val accentColor: Color
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user
    val isLoggedIn = authState is AuthState.Success

    val displayName = remember(currentUser) {
        if (isLoggedIn) currentUser?.displayName ?: "Hamba Allah" else "Tamu Allah"
    }

    val pages = listOf(
        OnboardingPage(
            title = "Jadwal Pengajian",
            description = "Pantau jadwal kajian dan kegiatan masjid secara real-time. Jangan lewatkan majelis ilmu.",
            icon = Icons.Default.DateRange,
            primaryColor = Color(0xFF4285F4),
            accentColor = Color(0xFFE3F2FD)
        ),
        OnboardingPage(
            title = "Assalamualaikum,\n$displayName!",
            description = "Selamat datang di Smart Mosque Masjid Agung Manonjaya. Aplikasi manajemen masjid modern.",
            icon = Icons.Default.AccountBox,
            primaryColor = EmeraldDeep,
            accentColor = EmeraldLight.copy(alpha = 0.2f)
        ),
        OnboardingPage(
            title = "Infaq & Donasi",
            description = "Salurkan infaq dan wakaf dengan mudah dan transparan. Dukung kemakmuran masjid.",
            icon = Icons.Default.Favorite,
            primaryColor = Color(0xFFF57C00),
            accentColor = Color(0xFFFFF3E0)
        ),
        OnboardingPage(
            title = "Notifikasi Digital",
            description = "Dapatkan pengingat otomatis untuk waktu sholat dan informasi penting masjid.",
            icon = Icons.Default.Notifications,
            primaryColor = Color(0xFF7B1FA2),
            accentColor = Color(0xFFF3E5F5)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BgPremium,
        contentWindowInsets = WindowInsets(0.dp) // Kita atur manual biar background full
    ) { _ ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- BAGIAN ATAS: GAMBAR/ICON (Flexible - Weight 1f) ---
            // Ini akan mengambil sisa ruang yang ada. Jadi kalau layar pendek, gambar mengecil.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { index ->
                    OnboardingVisualAnimated(page = pages[index])
                }
            }

            // --- BAGIAN BAWAH: KONTEN (Dynamic Height) ---
            // TIDAK PAKAI WEIGHT. Tingginya menyesuaikan isi teks + tombol.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                color = White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 32.dp)
                        // PENTING: Menambahkan padding agar tombol tidak tertutup navigasi HP
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Indikator
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        repeat(pages.size) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 32.dp else 10.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "dot"
                            )
                            val color = if (isSelected) EmeraldDeep else Color.LightGray.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .height(10.dp)
                                    .width(width)
                                    .clip(RoundedCornerShape(50))
                                    .background(color)
                            )
                        }
                    }

                    // 2. Teks Judul & Deskripsi
                    // Diberi minHeight agar posisi tombol stabil saat slide digeser
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        AnimatedContent(
                            targetState = pages[pagerState.currentPage],
                            transitionSpec = {
                                fadeIn(animationSpec = tween(600)) + slideInVertically { it / 2 } togetherWith
                                        fadeOut(animationSpec = tween(400))
                            },
                            label = "textAnim"
                        ) { currentPage ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentPage.title,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextColorSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 32.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = currentPage.description,
                                    fontSize = 14.sp,
                                    color = TextColorSecondary.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 3. Tombol Navigasi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (isLoggedIn) navController.navigate(Screen.Home.route)
                                else navController.navigate(Screen.Login.route)
                            }
                        ) {
                            Text(
                                "Lewati",
                                color = TextColorSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage < pages.size - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    } else {
                                        if (isLoggedIn) navController.navigate(Screen.Home.route)
                                        else navController.navigate(Screen.Login.route)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                            modifier = Modifier.shadow(
                                8.dp,
                                RoundedCornerShape(16.dp),
                                spotColor = EmeraldDeep.copy(alpha = 0.4f)
                            )
                        ) {
                            val isLastPage = pagerState.currentPage == pages.size - 1
                            Text(
                                text = if (isLastPage) "Mulai" else "Lanjut",
                                color = White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isLastPage) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    null,
                                    tint = White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- KOMPONEN VISUAL DENGAN ANIMASI FLOATING ---
@Composable
fun OnboardingVisualAnimated(page: OnboardingPage) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(page.accentColor, BgPremium),
                    startY = 0f,
                    endY = 1000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(scalePulse)
                .clip(CircleShape)
                .background(page.primaryColor.copy(alpha = 0.1f))
        )

        Surface(
            modifier = Modifier
                .offset(y = floatOffset.dp)
                .size(140.dp),
            shape = CircleShape,
            color = page.primaryColor,
            shadowElevation = 10.dp,
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}