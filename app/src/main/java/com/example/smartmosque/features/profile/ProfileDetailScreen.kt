package com.example.smartmosque.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.ui.theme.Screen
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.messaging.FirebaseMessaging


// --- IMPORT WARNA DARI TEMA ---
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.RedError
import com.example.smartmosque.ui.theme.GrayInputBackground

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current

    // 1. Ambil Data User Realtime
    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoggedIn = currentUser != null
    val userId = currentUser?.uid

    // --- STATE UNTUK STATISTIK & ROLE ---
    var totalDonation by remember { mutableLongStateOf(0L) }
    var totalEvents by remember { mutableIntStateOf(0) }
    var userRoleLabel by remember { mutableStateOf("Member") }

    // --- LOGIC: HITUNG DONASI, EVENT, & CEK ROLE ---
    LaunchedEffect(userId) {
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()

            // A. CEK ROLE USER (Admin/Member)
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val role = snapshot.getString("role")
                        userRoleLabel = if (role.equals("admin", ignoreCase = true)) "Admin" else "Member"
                    }
                }

            // B. HITUNG TOTAL DONASI (Hanya yang APPROVED)
            db.collection("donations")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "APPROVED")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val sum = snapshot.documents.sumOf { it.getLong("amount") ?: 0L }
                        totalDonation = sum
                    }
                }

            // C. HITUNG TOTAL EVENT (Gabungan Online & Offline)
            db.collection("schedules")
                .whereArrayContains("participantsOffline", userId)
                .addSnapshotListener { offlineSnap, _ ->
                    val offlineIds = offlineSnap?.documents?.map { it.id }?.toSet() ?: emptySet()

                    db.collection("schedules")
                        .whereArrayContains("participantsOnline", userId)
                        .addSnapshotListener { onlineSnap, _ ->
                            val onlineIds = onlineSnap?.documents?.map { it.id }?.toSet() ?: emptySet()
                            totalEvents = (offlineIds + onlineIds).size
                        }
                }
        }
    }

    // 2. Persiapkan Data Tampilan
    val displayName = if (isLoggedIn) (currentUser?.displayName ?: "Jamaah Baru") else "Tamu"
    val email = if (isLoggedIn) (currentUser?.email ?: "-") else "Mode Tamu"

    val donationFormatted = remember(totalDonation) {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        format.format(totalDonation)
    }

    val initials = remember(displayName) {
        if (isLoggedIn) {
            displayName.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("")
                .uppercase()
        } else "?"
    }

    val joinDate = remember(currentUser) {
        if (isLoggedIn) {
            val creationTime = currentUser?.metadata?.creationTimestamp ?: System.currentTimeMillis()
            val date = Date(creationTime)
            val format = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
            "Bergabung sejak ${format.format(date)}"
        } else {
            "Belum terdaftar"
        }
    }

    Scaffold(
        containerColor = BgPremium,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profil Saya", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = { navController.navigate(Screen.EditProfile.route) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EmeraldDeep)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BgPremium,
                    titleContentColor = TextColorPrimary
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. HEADER PROFILE ---
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(10.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.15f))
                        .clip(CircleShape)
                        .background(EmeraldDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = initials, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = White)
                }

                if (isLoggedIn) {
                    val badgeColor = if (userRoleLabel == "Admin") Color(0xFFD4AF37) else EmeraldDeep
                    Box(
                        modifier = Modifier
                            .offset(x = 5.dp, y = 5.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(White)
                            .border(2.dp, badgeColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = userRoleLabel, fontSize = 10.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
            Text(text = email, fontSize = 12.sp, color = TextColorSecondary)
            Spacer(modifier = Modifier.height(30.dp))

            // --- 2. STATISTIK ---
            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                        .background(White, RoundedCornerShape(20.dp))
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PremiumStatColumn(donationFormatted, "Total Infaq")
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(GrayInputBackground))
                    PremiumStatColumn("$totalEvents", "Event Dihadiri")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- 3. INFORMASI KONTAK ---
            Text("Informasi Akun", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    PremiumProfileItem(Icons.Outlined.Phone, "Nomor Telepon", if (isLoggedIn) "+62 8xx-xxxx-xxxx" else "-")
                    PremiumDivider()
                    PremiumProfileItem(Icons.Outlined.LocationOn, "Alamat", if (isLoggedIn) "Tasikmalaya, ID" else "-")
                    PremiumDivider()
                    PremiumProfileItem(Icons.Outlined.DateRange, "Status", joinDate)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 4. PENGATURAN (PREFERENSI NOTIFIKASI YANG DISELARASKAN) ---
            if (isLoggedIn) {
                Text("Preferensi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(10.dp))

                val notifSettings by authViewModel.notifSettings.collectAsState()

                Card(
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {

                        // SWITCH 1: NOTIFIKASI JADWAL KAJIAN
                        PremiumToggleItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifikasi Jadwal",
                            isChecked = notifSettings["schedule"] ?: true, // Diubah dari "events" ke "schedule"
                            onToggle = { isChecked ->
                                // A. Update ke Firestore Database via ViewModel
                                authViewModel.toggleNotification("schedule", isChecked)

                                // B. Sinkronisasi Langsung ke Topik FCM Android System
                                if (isChecked) {
                                    FirebaseMessaging.getInstance().subscribeToTopic("schedule")
                                } else {
                                    FirebaseMessaging.getInstance().unsubscribeFromTopic("schedule")
                                }

                                val msg = if (isChecked) "Notifikasi Jadwal Diaktifkan" else "Notifikasi Jadwal Dimatikan"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )

                        PremiumDivider()

                        // SWITCH 2: NOTIFIKASI PROGRAM WAKAF & DONASI
                        PremiumToggleItem(
                            icon = Icons.Outlined.AttachMoney,
                            title = "Info Donasi & Wakaf",
                            isChecked = notifSettings["wakaf"] ?: true, // Diubah dari "donations" ke "wakaf"
                            onToggle = { isChecked ->
                                // A. Update ke Firestore Database via ViewModel
                                authViewModel.toggleNotification("wakaf", isChecked)

                                // B. Sinkronisasi Langsung ke Topik FCM Android System
                                if (isChecked) {
                                    FirebaseMessaging.getInstance().subscribeToTopic("wakaf")
                                } else {
                                    FirebaseMessaging.getInstance().unsubscribeFromTopic("wakaf")
                                }

                                val msg = if (isChecked) "Info Donasi & Wakaf Diaktifkan" else "Info Donasi & Wakaf Dimatikan"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }

            // --- 5. LOGOUT ---
            if (isLoggedIn) {
                Button(
                    onClick = {
                        // Opsional: Sebelum logout, bersihkan semua subscribe topik agar user lama tidak menerima notif masjid lagi
                        try {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("system")
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("schedule")
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("wakaf")
                        } catch (e: Exception) { /* ignore */ }

                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = RedError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar Akun", color = RedError, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { navController.navigate(Screen.Login.route) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Masuk Sekarang", color = White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- KOMPONEN UI TAMBAHAN (TETAP SAMA) ---
@Composable fun PremiumStatColumn(value: String, label: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep); Text(label, fontSize = 11.sp, color = TextColorSecondary) } }
@Composable fun PremiumProfileItem(icon: ImageVector, title: String, value: String) { Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(36.dp).background(EmeraldDeep, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = White, modifier = Modifier.size(18.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column { Text(title, fontSize = 11.sp, color = TextColorSecondary); Text(value, fontSize = 14.sp, color = TextColorPrimary, fontWeight = FontWeight.Medium) } } }
@Composable fun PremiumDivider() { HorizontalDivider(color = GrayInputBackground.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(start = 72.dp)) }

@Composable
fun PremiumToggleItem(icon: ImageVector, title: String, isChecked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle(!isChecked) }.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(EmeraldDeep, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = White, modifier = Modifier.size(18.dp)) }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextColorPrimary)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = EmeraldDeep, uncheckedThumbColor = White, uncheckedTrackColor = GrayInputBackground, uncheckedBorderColor = Color.Transparent),
            modifier = Modifier.scale(0.8f)
        )
    }
}