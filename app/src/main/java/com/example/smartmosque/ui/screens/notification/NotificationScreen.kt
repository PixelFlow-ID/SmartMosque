package com.example.smartmosque.ui.screens.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartmosque.viewmodel.AuthState
import com.example.smartmosque.viewmodel.AuthViewModel
import com.example.smartmosque.data.model.Donation
import com.example.smartmosque.data.model.Schedule
import com.example.smartmosque.data.model.WaqfProject
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.smartmosque.viewmodel.NotificationViewModel

// --- GANTI IMPORT INI SESUAI TEMA APLIKASI ANDA ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.RedError
import com.example.smartmosque.ui.theme.GrayInputBackground
import com.example.smartmosque.viewmodel.JamaahNotificationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    notifViewModel: NotificationViewModel = viewModel()
) {
    val pendingDonations by notifViewModel.pendingDonations.collectAsState()
    val jamaahNotifications by notifViewModel.jamaahNotifications.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user

    // GANTI EMAIL INI SESUAI EMAIL ADMIN ANDA
    val isAdmin = currentUser?.email == "ramdanidoni244@gmail.com"

    // Trigger Load Data (Otomatis saat layar dibuka)
    LaunchedEffect(currentUser) {
        if (!isAdmin && currentUser != null) {
            notifViewModel.fetchJamaahNotifications(currentUser.uid)
        }
    }

    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedDonation by remember { mutableStateOf<Donation?>(null) }

    Scaffold(
        containerColor = BgPremium,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if(isAdmin) "Admin Panel" else "Kotak Masuk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BgPremium)
            )
        }
    ) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            if (isAdmin) {
                // ==========================================
                // TAMPILAN ADMIN
                // ==========================================
                if (pendingDonations.isNotEmpty()) {
                    PaddingValues(horizontal = 20.dp, vertical = 10.dp).let {
                        Text(
                            "Menunggu Verifikasi (${pendingDonations.size})",
                            fontWeight = FontWeight.Bold,
                            color = TextColorPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingDonations) { donation ->
                            AdminVerificationCard(
                                donation = donation,
                                onClick = {
                                    selectedDonation = donation
                                    showDetailDialog = true
                                },
                                onApprove = { notifViewModel.approveDonation(donation) },
                                onReject = { notifViewModel.rejectDonation(donation.id) }
                            )
                        }
                    }
                } else {
                    EmptyStateNotif(message = "Tidak ada donasi pending saat ini.")
                }
            } else {
                // ==========================================
                // TAMPILAN JAMAAH (USER)
                // ==========================================
                if (jamaahNotifications.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(jamaahNotifications) { item ->
                            when (item) {
                                is JamaahNotificationItem.DonationStatus -> {
                                    JamaahDonationCard(item.donation)
                                }
                                is JamaahNotificationItem.NewSchedule -> {
                                    JamaahScheduleCard(item.schedule)
                                }
                                is JamaahNotificationItem.NewWaqf -> {
                                    JamaahWaqfCard(item.waqf)
                                }
                            }
                        }
                    }
                } else {
                    EmptyStateNotif(message = "Belum ada notifikasi baru dalam 30 hari terakhir.")
                }
            }
        }
    }

    // --- DIALOG DETAIL (ADMIN) ---
    if (showDetailDialog && selectedDonation != null) {
        DonationDetailDialog(
            donation = selectedDonation!!,
            onDismiss = { showDetailDialog = false },
            onApprove = {
                notifViewModel.approveDonation(selectedDonation!!)
                showDetailDialog = false
            },
            onReject = {
                notifViewModel.rejectDonation(selectedDonation!!.id)
                showDetailDialog = false
            }
        )
    }
}

// ==========================================
// KOMPONEN ITEM LIST (CARDS)
// ==========================================

@Composable
fun JamaahDonationCard(donation: Donation) {
    val amount = try { NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(donation.amount) } catch (e: Exception) { "Rp ${donation.amount}" }

    val (statusColor, statusText, icon) = when(donation.status) {
        "APPROVED" -> Triple(GreenPrimary, "Diterima", Icons.Outlined.CheckCircle)
        "REJECTED" -> Triple(RedError, "Ditolak", Icons.Outlined.Close)
        else -> Triple(Color(0xFFF59E0B), "Menunggu Konfirmasi", Icons.Outlined.AccessTime)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = statusColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Infaq: ${donation.category}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColorPrimary)
                Text(statusText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(amount, fontSize = 13.sp, color = TextColorSecondary)
            }
        }
    }
}

@Composable
fun JamaahScheduleCard(schedule: Schedule) {
    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(EmeraldDeep.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CalendarToday, null, tint = EmeraldDeep)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(color = EmeraldDeep, shape = RoundedCornerShape(4.dp)) {
                    Text("JADWAL BARU", fontSize = 9.sp, color = White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(schedule.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColorPrimary)
                Text("Bersama: ${schedule.speaker}", fontSize = 12.sp, color = TextColorSecondary)
            }
        }
    }
}

@Composable
fun JamaahWaqfCard(waqf: WaqfProject) {
    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.FavoriteBorder, null, tint = Color(0xFF8B5CF6))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(color = Color(0xFF8B5CF6), shape = RoundedCornerShape(4.dp)) {
                    Text("PROGRAM WAKAF", fontSize = 9.sp, color = White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(waqf.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColorPrimary)
                Text("Mari bantu pembangunan umat", fontSize = 12.sp, color = TextColorSecondary)
            }
        }
    }
}

@Composable
fun AdminVerificationCard(
    donation: Donation,
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var donorName by remember { mutableStateOf("Memuat...") }
    LaunchedEffect(donation.userId) {
        if (donation.userId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(donation.userId).get()
                .addOnSuccessListener { donorName = it.getString("fullName") ?: "Hamba Allah" }
        }
    }

    val formattedAmount = try { NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(donation.amount) } catch (e: Exception) { "Rp ${donation.amount}" }

    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(BgPremium, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ReceiptLong, null, tint = EmeraldDeep)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(donation.category, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColorPrimary)
                    Text("Oleh: $donorName", fontSize = 12.sp, color = TextColorSecondary)
                }
                Text(formattedAmount, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldDeep)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = GrayInputBackground, contentColor = TextColorSecondary), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(36.dp), contentPadding = PaddingValues(0.dp)) { Text("Detail", fontSize = 12.sp) }
                Button(onClick = onReject, colors = ButtonDefaults.buttonColors(containerColor = RedError.copy(alpha = 0.1f), contentColor = RedError), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(36.dp), contentPadding = PaddingValues(0.dp)) { Text("Tolak", fontSize = 12.sp) }
                Button(onClick = onApprove, colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(36.dp), contentPadding = PaddingValues(0.dp)) { Text("Terima", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun DonationDetailDialog(
    donation: Donation,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var donorName by remember { mutableStateOf("Memuat...") }
    var donorEmail by remember { mutableStateOf("-") }
    LaunchedEffect(donation.userId) {
        if (donation.userId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(donation.userId).get()
                .addOnSuccessListener {
                    donorName = it.getString("fullName") ?: "Hamba Allah"
                    donorEmail = it.getString("email") ?: "-"
                }
        }
    }
    val formattedAmount = try { NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(donation.amount) } catch (e: Exception) { "Rp ${donation.amount}" }
    val fullDate = try {
        val date = donation.date?.toDate() ?: java.util.Date()
        SimpleDateFormat("EEEE, dd MMMM yyyy - HH:mm", Locale("id", "ID")).format(date)
    } catch (e: Exception) { "-" }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = White), modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.Black)) {
                    if (donation.proofUrl.isNotEmpty()) {
                        AsyncImage(model = donation.proofUrl, contentDescription = "Bukti", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    } else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tidak ada bukti", color = Color.White) } }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) { Icon(Icons.Default.Close, null, tint = White) }
                }
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(donation.category, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Text("ID: ${donation.id.take(8)}...", fontSize = 12.sp, color = TextColorSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailRow(label = "Nominal", value = formattedAmount, isBold = true, color = EmeraldDeep)
                    DetailRow(label = "Tanggal", value = fullDate)
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = GrayInputBackground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = TextColorSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Pengirim", fontSize = 11.sp, color = TextColorSecondary)
                            Text(donorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColorPrimary)
                            Text(donorEmail, fontSize = 12.sp, color = TextColorSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onReject, colors = ButtonDefaults.buttonColors(containerColor = RedError.copy(alpha = 0.1f), contentColor = RedError), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(45.dp)) { Text("Tolak") }
                        Button(onClick = onApprove, colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(45.dp)) { Text("Konfirmasi") }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false, color: Color = TextColorPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextColorSecondary)
        Text(value, fontSize = 14.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = color)
    }
}

@Composable
fun EmptyStateNotif(message: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.NotificationsNone, null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = TextColorSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
