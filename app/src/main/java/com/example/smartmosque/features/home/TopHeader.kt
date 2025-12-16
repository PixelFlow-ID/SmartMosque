package com.example.smartmosque.features.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.ui.theme.Screen
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import java.util.Calendar

// --- IMPORT WARNA DARI THEME ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.RedError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopHeader(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ambil Data User & Role
    val currentUser by authViewModel.currentUser.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()
    val isAdmin = userRole == "admin"

    val mainTabs = listOf(
        Screen.Home.route,
        Screen.Schedule.route,
        Screen.Donation.route,
        Screen.AboutMosque.route,
        Screen.ProfileDetail.route
    )
    val isMainTab = currentRoute in mainTabs

    // --- LOGIKA PINTAR TITIK MERAH (BADGE) ---
    var hasPendingTransaction by remember { mutableStateOf(false) }
    var hasNewAnnouncement by remember { mutableStateOf(false) }

    // Titik merah muncul jika: Ada Transaksi Pending ATAU Ada Info Baru
    val showRedDot = hasPendingTransaction || hasNewAnnouncement

    LaunchedEffect(currentUser, userRole) {
        val uid = currentUser?.uid
        val db = Firebase.firestore

        if (uid != null) {
            // 1. CEK TRANSAKSI PENDING (Donasi)
            if (isAdmin) {
                // Admin: Cek SEMUA donasi pending (Tugas Validasi)
                db.collection("donations")
                    .whereEqualTo("status", "PENDING")
                    .addSnapshotListener { snap, _ ->
                        hasPendingTransaction = snap != null && !snap.isEmpty
                    }
            } else {
                // User: Cek donasi SAYA yang pending
                db.collection("donations")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("status", "PENDING")
                    .addSnapshotListener { snap, _ ->
                        hasPendingTransaction = snap != null && !snap.isEmpty
                    }
            }

            // 2. CEK INFO APLIKASI BARU (Announcements)
            val threeDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -3)
            }.time

            db.collection("announcements")
                .whereGreaterThan("date", threeDaysAgo)
                .addSnapshotListener { snap, _ ->
                    hasNewAnnouncement = snap != null && !snap.isEmpty
                }
        }
    }

    TopAppBar(
        title = {
            if (isMainTab) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Masjid Agung Manonjaya", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Aplikasi Smart Mosque",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColorPrimary // Menggantikan Color.Black
                        )
                        Text(
                            text = "Masjid Agung Manonjaya",
                            fontSize = 12.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                val titleText = when(currentRoute) {
                    "admin_validation" -> "Validasi Admin"
                    Screen.Notification.route -> "Notifikasi"
                    Screen.WaqfDetail.route -> "Detail Wakaf"
                    Screen.EditProfile.route -> "Edit Profil"
                    Screen.AddSchedule.route -> "Tambah Jadwal"
                    else -> "Kembali"
                }
                Text(
                    text = titleText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColorPrimary
                )
            }
        },
        navigationIcon = {
            if (!isMainTab) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = GreenPrimary
                    )
                }
            }
        },
        actions = {
            if (isMainTab) {
                IconButton(onClick = {
                    if (isAdmin) {
                        navController.navigate("admin_validation")
                    } else {
                        navController.navigate(Screen.Notification.route)
                    }
                }) {
                    Box {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifikasi",
                            tint = TextColorPrimary, // Menggantikan Color.Black
                            modifier = Modifier.size(26.dp)
                        )
                        // Titik Merah
                        if (showRedDot) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(RedError) // Menggantikan Color.Red
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White), // White dari Theme
        modifier = Modifier.shadow(4.dp)
    )
}