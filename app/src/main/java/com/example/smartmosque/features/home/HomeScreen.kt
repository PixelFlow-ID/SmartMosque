package com.example.smartmosque.features.home


import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartmosque.features.finance.FinanceViewModel
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.features.schedule.components.MiniCardSchedule
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

// DATA MODEL
data class InfaqCategoryHome(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val iconColor: Color
)

// Warna Khusus untuk Grafik Emas (Local)
private val GoldAccent = Color(0xFFFFD700)

@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    financeViewModel: com.example.smartmosque.features.finance.FinanceViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val userName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Jamaah"
    val userInitial = userName.take(1).uppercase()
    val context = LocalContext.current
    // Notifikasi State
    val hasUnreadNotifications by homeViewModel.hasUnreadNotifications.collectAsState()
    // Ongoing Event State (BARU)
    val ongoingEvent by homeViewModel.ongoingEvent.collectAsState()
    // --- STATE UNTUK FLOW INFAQ ---
    var showInfaqSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedInfaqCategory by remember { mutableStateOf<InfaqCategoryHome?>(null) }
    var amountToPay by remember { mutableStateOf(0L) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPremium)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 10.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profil User
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Screen.ProfileDetail.route) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(4.dp, CircleShape, spotColor = EmeraldDeep.copy(alpha = 0.2f))
                            .clip(CircleShape)
                            .background(EmeraldDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userInitial, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text("Assalamualaikum,", fontSize = 12.sp, color = TextGrey, fontWeight = FontWeight.Medium)
                        Text("$userName 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }
                }

                // Tombol Notifikasi
                Surface(
                    onClick = {
                        homeViewModel.markNotificationsAsRead()
                        navController.navigate(Screen.Notification.route)
                    },
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = CardSurface,
                    shadowElevation = 3.dp,
                    tonalElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Notifications, "Notifikasi", tint = TextBlack, modifier = Modifier.size(24.dp))
                        if (hasUnreadNotifications) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        }
                    }
                }
            }

            // --- KONTEN SCROLL ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 1. DASHBOARD GRAFIK ANIMASI
                AnimatedEmeraldCard(homeViewModel)

                Spacer(modifier = Modifier.height(24.dp))

                // --- 1.5 FITUR BARU: ONGOING EVENT (SEDANG BERLANGSUNG) ---
                if (ongoingEvent != null) {
                    OngoingLiveCard(
                        schedule = ongoingEvent!!,
                        onClick = {
                            // Navigasi ke Detail Jadwal
                            navController.navigate("schedule_detail/${ongoingEvent!!.id}")
                        }
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                }
                // ----------------------------------------------------------

                // 2. FINANCE SUMMARY
                FinanceSummaryCard(financeViewModel) {
                    navController.navigate(Screen.Finance.route)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. WAKAF & EVENT
                com.example.smartmosque.features.donation.components.MiniWaqfProjectNew(navController)

                Spacer(modifier = Modifier.height(24.dp))
                MiniCardSchedule(navController, authViewModel)
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// --- ONGOING LIVE CARD ---
// --- GANTI KODE LAMA 'OngoingLiveCard' DENGAN INI ---

@Composable
fun OngoingLiveCard(schedule: Schedule, onClick: () -> Unit) {
    // Animasi Pulse untuk titik merah
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFFFF5252).copy(alpha = 0.25f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEBEE)) // Border merah sangat muda
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // --- HEADER: BADGE LIVE & WAKTU ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge LIVE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(50)) // Background merah muda
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = alpha)) // Titik berkedip
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LIVE SEKARANG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        letterSpacing = 0.5.sp
                    )
                }

                // Jam
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${schedule.time} WIB",
                        fontSize = 12.sp,
                        color = TextColorSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CONTENT: JUDUL & PEMBICARA ---
            Row(verticalAlignment = Alignment.Top) {
                // Icon Kategori Besar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFEBEE), // Background icon merah muda
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Mic, // Icon Mic
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColorPrimary,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ust. ${schedule.speaker}",
                        fontSize = 14.sp,
                        color = TextColorSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(12.dp))

            // --- FOOTER: LOKASI & CTA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LocationOn, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = schedule.location,
                        fontSize = 12.sp,
                        color = TextColorSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Tombol "Gabung" Kecil
                Surface(
                    color = EmeraldDeep,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gabung", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceSummaryCard(
    financeViewModel: FinanceViewModel,
    onClick: () -> Unit
) {
    // Collect Data Realtime
    val balance by financeViewModel.currentBalance.collectAsState()
    val income by financeViewModel.totalIncome.collectAsState()
    val expense by financeViewModel.totalExpense.collectAsState()

    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRp.maximumFractionDigits = 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = EmeraldDeep.copy(0.2f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Judul dan Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(EmeraldLight.copy(0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AccountBalanceWallet, null, tint = EmeraldDeep, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Kas Masjid", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                }

                // CTA Button (Small & Elegant)
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(50),
                    color = EmeraldDeep.copy(0.1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Detail", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = EmeraldDeep, modifier = Modifier.size(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Balance
            Text(
                text = formatRp.format(balance),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextBlack
            )
            Text("Saldo saat ini", fontSize = 12.sp, color = TextColorSecondary)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = GrayInputBackground)
            Spacer(modifier = Modifier.height(12.dp))

            // Summary Income / Expense (Small)
            Row(modifier = Modifier.fillMaxWidth()) {
                // Income
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Pemasukan", fontSize = 10.sp, color = TextColorSecondary)
                        Text(formatRp.format(income), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }
                }

                // Expense
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, null, tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Pengeluaran", fontSize = 10.sp, color = TextColorSecondary)
                        Text(formatRp.format(expense), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }
                }
            }
        }
    }
}

// --- KOMPONEN GRAFIK ANIMASI ---
@Composable
fun AnimatedEmeraldCard(homeViewModel: HomeViewModel) {
    val eventsThisMonth by homeViewModel.eventsThisMonth.collectAsState()
    val totalParticipants by homeViewModel.totalParticipants.collectAsState()

    // Trigger animasi saat masuk layar
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    // Animasi Donut (Lingkaran)
    val donutProgress by animateFloatAsState(
        targetValue = if (startAnimation) 0.75f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "donut"
    )

    // Animasi Bar (Grafik Batang) - Bergelombang
    val bar1 by animateDpAsState(if (startAnimation) 15.dp else 0.dp, tween(1000), label = "b1")
    val bar2 by animateDpAsState(if (startAnimation) 30.dp else 0.dp, tween(1000, 100), label = "b2")
    val bar3 by animateDpAsState(if (startAnimation) 20.dp else 0.dp, tween(1000, 200), label = "b3")
    val bar4 by animateDpAsState(if (startAnimation) 35.dp else 0.dp, tween(1000, 300), label = "b4")

    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldDeep)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Hiasan Latar Belakang
            Box(modifier = Modifier.offset(x = 200.dp, y = (-50).dp).size(250.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))

            Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                // KIRI: Grafik Donut
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                            // Track
                            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = Color.White.copy(alpha = 0.1f), strokeWidth = 4.dp)
                            // Progress Animasi
                            CircularProgressIndicator(progress = { donutProgress }, modifier = Modifier.fillMaxSize(), color = EmeraldLight, strokeWidth = 4.dp, strokeCap = StrokeCap.Round)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Kegiatan", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Bulan Ini", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = eventsThisMonth.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // GARIS PEMISAH
                Box(modifier = Modifier.width(1.dp).height(60.dp).background(Color.White.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.width(24.dp))

                // KANAN: Grafik Batang
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Animasi Bar
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(24.dp)) {
                            GoldBar(bar1)
                            GoldBar(bar2)
                            GoldBar(bar3)
                            GoldBar(bar4)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Total", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Kehadiran", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = totalParticipants.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// Helper untuk Batang Emas
@Composable
fun GoldBar(height: Dp) {
    Box(modifier = Modifier.width(5.dp).height(height).clip(RoundedCornerShape(4.dp)).background(GoldAccent))
}
