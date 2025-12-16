package com.example.smartmosque.features.schedule

import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Pastikan ini ada
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.ui.theme.Screen
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- IMPORT WARNA TEMA ---
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.EmeraldLight
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.RedError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val userRole by authViewModel.userRole.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userId = currentUser?.uid
    val isAdmin = userRole == "admin"

    var scheduleList by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Akan Datang") }

    // --- FETCH DATA ---
    LaunchedEffect(Unit) {
        Firebase.firestore.collection("schedules")
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val onlineList = (doc.get("participantsOnline") as? List<String>) ?: emptyList()
                            @Suppress("UNCHECKED_CAST")
                            val offlineList = (doc.get("participantsOffline") as? List<String>) ?: emptyList()

                            Schedule(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                speaker = doc.getString("speaker") ?: "",
                                time = doc.getString("time") ?: "",
                                location = doc.getString("location") ?: "",
                                category = doc.getString("category") ?: "Pengajian",
                                date = doc.getTimestamp("date"),
                                participantsOnline = onlineList,
                                participantsOffline = offlineList
                            )
                        } catch (err: Exception) { null }
                    }
                    scheduleList = list
                }
            }
    }

    // --- LOGIKA FILTER DIPERBAIKI ---
    val filteredList = remember(scheduleList, selectedFilter) {
        // Toleransi waktu 3 JAM setelah jadwal dimulai masih dianggap "Akan Datang/Sedang Berlangsung"
        // Logikanya: Event belum selesai kalau belum lewat 3 jam dari start time.
        val threeHoursInMillis = 3 * 60 * 60 * 1000
        val cutoffTime = Date(System.currentTimeMillis() - threeHoursInMillis)

        when (selectedFilter) {
            "Akan Datang" -> scheduleList.filter {
                // Tampilkan jika tanggal jadwal LEBIH BESAR dari (Sekarang - 3 jam)
                it.date?.toDate()?.after(cutoffTime) == true
            }
            "Selesai" -> scheduleList.filter {
                // Tampilkan jika tanggal jadwal LEBIH KECIL dari (Sekarang - 3 jam)
                it.date?.toDate()?.before(cutoffTime) == true
            }.reversed()
            else -> scheduleList
        }
    }

    Scaffold(
        containerColor = BgPremium,
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddSchedule.route) },
                    containerColor = EmeraldDeep,
                    contentColor = White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScheduleHeader()

            Column(modifier = Modifier.fillMaxSize()) {
                // Filter Tabs
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumFilterButton("Akan Datang", selectedFilter) { selectedFilter = it }
                    PremiumFilterButton("Selesai", selectedFilter) { selectedFilter = it }
                    PremiumFilterButton("Semua", selectedFilter) { selectedFilter = it }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldDeep)
                    }
                } else if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Event, null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
                            Text("Tidak ada jadwal.", color = TextColorSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredList) { schedule ->
                            val isJoined = if (userId != null) schedule.participantsOnline.contains(userId) else false

                            PremiumScheduleCard(
                                schedule = schedule,
                                isAdmin = isAdmin,
                                isJoined = isJoined,
                                // 1. PERBAIKAN: Tambahkan onClick Naviagasi ke Detail
                                onClick = {
                                    // Asumsi route Anda: "schedule_detail/{scheduleId}"
                                    // Sesuaikan string route ini dengan NavHost Anda
                                    navController.navigate("schedule_detail/${schedule.id}")
                                          },
                                onDelete = {
                                    Firebase.firestore.collection("schedules").document(schedule.id).delete()
                                        .addOnSuccessListener { Toast.makeText(context, "Jadwal dihapus", Toast.LENGTH_SHORT).show() }
                                },
                                onJoinToggle = {
                                    if (userId == null) {
                                        Toast.makeText(context, "Silakan login untuk bergabung", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val docRef = Firebase.firestore.collection("schedules").document(schedule.id)
                                        if (isJoined) {
                                            docRef.update("participantsOnline", FieldValue.arrayRemove(userId))
                                        } else {
                                            docRef.update("participantsOnline", FieldValue.arrayUnion(userId))
                                            Toast.makeText(context, "Berhasil bergabung!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onReminderClick = {
                                    val date = schedule.date?.toDate()
                                    if (date != null) {
                                        val intent = Intent(Intent.ACTION_INSERT).apply {
                                            data = CalendarContract.Events.CONTENT_URI
                                            putExtra(CalendarContract.Events.TITLE, schedule.title)
                                            putExtra(CalendarContract.Events.EVENT_LOCATION, schedule.location)
                                            putExtra(CalendarContract.Events.DESCRIPTION, "Pembicara: ${schedule.speaker}")
                                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.time)
                                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, date.time + (2 * 60 * 60 * 1000))
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ... Header & Filter Button sama ...

@Composable
fun ScheduleHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(White, BgPremium)))
            .padding(top = 24.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
    ) {
        Text("Jadwal Kajian", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
        Text("Jangan lewatkan majelis ilmu taman surga.", fontSize = 14.sp, color = TextColorSecondary)
    }
}

@Composable
fun PremiumFilterButton(text: String, selectedFilter: String, onClick: (String) -> Unit) {
    val isSelected = text == selectedFilter
    Surface(
        onClick = { onClick(text) },
        shape = RoundedCornerShape(50),
        color = if (isSelected) EmeraldDeep else White,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text, color = if (isSelected) White else TextColorSecondary.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
        }
    }
}

// --- CARD JADWAL DIPERBAIKI ---
@Composable
fun PremiumScheduleCard(
    schedule: Schedule,
    isAdmin: Boolean,
    isJoined: Boolean,
    onClick: () -> Unit, // 2. Parameter Baru
    onDelete: () -> Unit,
    onJoinToggle: () -> Unit,
    onReminderClick: () -> Unit
) {
    val dateObj = schedule.date?.toDate() ?: Date()
    val dayStr = SimpleDateFormat("dd", Locale("id", "ID")).format(dateObj)
    val monthStr = SimpleDateFormat("MMM", Locale("id", "ID")).format(dateObj).uppercase()
    val totalAttendees = schedule.participantsOnline.size + schedule.participantsOffline.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .clickable { onClick() }, // 3. Logika Klik Diterapkan di Sini
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ... (Isi Card Sama seperti sebelumnya) ...
            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EmeraldLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(width = 56.dp, height = 64.dp)
                ) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(dayStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                        Text(monthStr, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EmeraldDeep)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(color = EmeraldDeep, shape = RoundedCornerShape(6.dp)) {
                        Text(schedule.category, fontSize = 10.sp, color = White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(schedule.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.clickable { onDelete() }) {
                            Icon(Icons.Default.Delete, null, tint = RedError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus", fontSize = 12.sp, color = RedError)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumInfoRow(Icons.Default.AccessTime, schedule.time)
                PremiumInfoRow(Icons.Default.Mic, schedule.speaker)
                PremiumInfoRow(Icons.Default.LocationOn, schedule.location)
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BgPremium, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$totalAttendees Jamaah Hadir", fontSize = 12.sp, color = TextColorSecondary, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onReminderClick) {
                        Icon(Icons.Outlined.NotificationsActive, "Ingatkan", tint = Color(0xFFFF9800))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    // Hentikan propagasi klik ke parent card saat tombol "Hadir" ditekan
                    Button(
                        onClick = onJoinToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isJoined) EmeraldLight.copy(alpha = 0.3f) else EmeraldDeep,
                            contentColor = if (isJoined) EmeraldDeep else White
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (isJoined) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Terdaftar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Hadir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextColorSecondary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = TextColorSecondary)
    }
}