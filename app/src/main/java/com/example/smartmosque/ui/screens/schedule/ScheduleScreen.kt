package com.example.smartmosque.ui.screens.schedule

import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmosque.viewmodel.AuthViewModel
import com.example.smartmosque.viewmodel.ScheduleViewModel
import com.example.smartmosque.data.model.Schedule
import com.example.smartmosque.ui.theme.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- IMPORTS TEMA ---
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.RedError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    scheduleViewModel: ScheduleViewModel = viewModel()
) {
    val context = LocalContext.current

    // --- STATE USER & ADMIN ---
    val userRole by authViewModel.userRole.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userId = currentUser?.uid

    // LOGIKA PENENTU ADMIN
    // Pastikan di database Firestore collection 'users', field 'role' isinya benar-benar "admin"
    // LOGIKA PENENTU ADMIN (Email Hardcoded + Firestore Role)
    val isAdmin = userRole == "admin" || currentUser?.email == "ramdanidoni244@gmail.com"

    val scheduleList by scheduleViewModel.schedules.collectAsState()
    val isLoading by scheduleViewModel.isLoading.collectAsState()

    var selectedFilter by remember { mutableStateOf("Akan Datang") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }

    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    LaunchedEffect(Unit) {
        scheduleViewModel.fetchSchedules()
    }

    // --- FILTER DATA ---
    // --- FILTER DATA ---
    val filteredList = remember(scheduleList, selectedFilter, isAdmin) {
        val now = System.currentTimeMillis()
        val toleransiMs = 2 * 60 * 60 * 1000 // 2 Jam (Sesuai Logika Button)

        // Base List (Admin lihat semua, User hanya Published ATAU isActive)
        // FIX: Tambahkan OR isActive agar data lama yang belum punya field isPublished tetap muncul
        val baseList = if (isAdmin) scheduleList else scheduleList.filter { it.isPublished || it.isActive }

        when (selectedFilter) {
            "Semua" -> baseList.sortedBy { it.date }
            "Akan Datang" -> {
                baseList.filter {
                    val t = it.date?.toDate()?.time ?: 0L
                    // Tampilkan jika BELUM selesai (manual) DAN (Waktunya masa depan ATAU masih dalam toleransi durasi)
                    !it.isFinished && (t > (now - toleransiMs))
                }.sortedBy { it.date }
            }
            "Selesai" -> {
                baseList.filter {
                    val t = it.date?.toDate()?.time ?: 0L
                    // Tampilkan jika SUDAH selesai (manual) ATAU (Waktunya sudah lewat toleransi)
                    it.isFinished || (t <= (now - toleransiMs))
                }.sortedByDescending { it.date }
            }
            else -> baseList
        }
    }

    Scaffold(
        containerColor = BgPremium,
        floatingActionButton = {
            // Tombol Tambah HANYA muncul jika Admin
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddSchedule.route) },
                    containerColor = EmeraldDeep,
                    contentColor = White
                ) { Icon(Icons.Default.Add, "Tambah") }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // --- HEADER ---
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Dekorasi
                Icon(Icons.Outlined.CalendarToday, null, tint = EmeraldDeep.copy(0.05f),
                    modifier = Modifier.align(Alignment.CenterEnd).size(100.dp).rotate(-15f))

                Column {
                    // Indikator Role / Tanggal
                    Text(
                        text = if (isAdmin) "Mode: ADMIN" else todayDate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdmin) RedError else EmeraldDeep,
                        modifier = Modifier
                            .background(if (isAdmin) RedError.copy(0.1f) else EmeraldDeep.copy(0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                        Column {
                            Text("Jadwal Kajian", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                            Text("Temukan majelis ilmu terdekat.", fontSize = 14.sp, color = TextColorSecondary)
                        }
                }
            }

            // --- TABS ---
            Row(modifier = Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Tombol 'Semua' HANYA muncul jika Admin
                if (isAdmin) {
                    CleanFilterButton("Semua", selectedFilter) { selectedFilter = it }
                }
                CleanFilterButton("Akan Datang", selectedFilter) { selectedFilter = it }
                CleanFilterButton("Selesai", selectedFilter) { selectedFilter = it }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- LIST ---
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = EmeraldDeep) }
            } else if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada jadwal.", color = TextColorSecondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList) { schedule ->
                        val isJoined = if (userId != null) schedule.participantsOnline.contains(userId) else false

                        CleanScheduleCard(
                            schedule = schedule,
                            isAdmin = isAdmin, // <--- STATE INI SANGAT PENTING
                            isJoined = isJoined,
                            onClick = { navController.navigate("schedule_detail/${schedule.id}") },

                            // Event Handlers
                            onDelete = { scheduleToDelete = schedule; showDeleteDialog = true },
                            onEdit = { navController.navigate("edit_schedule/${schedule.id}") },
                            onPublish = { scheduleViewModel.updateSchedule(schedule.id, mapOf("isPublished" to true), {}, {}) },
                            onMarkAsFinished = { scheduleViewModel.updateSchedule(schedule.id, mapOf("isFinished" to true), {}, {}) },
                            onJoinToggle = {
                                if (userId == null) Toast.makeText(context, "Login dulu", Toast.LENGTH_SHORT).show()
                                else if (isJoined) scheduleViewModel.leaveEvent(schedule.id, userId, true, {}, {})
                                else scheduleViewModel.joinEvent(schedule.id, userId, true, {}, {})
                            },
                            onReminderClick = {
                                val date = schedule.date?.toDate()
                                if (date != null) {
                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                        data = CalendarContract.Events.CONTENT_URI
                                        putExtra(CalendarContract.Events.TITLE, schedule.title)
                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.time)
                                    }
                                    try { context.startActivity(intent) } catch(e:Exception){}
                                }
                            }
                        )
                    }
                    // Spacer bawah agar list tidak tertutup FAB (jika admin)
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // --- DIALOG ---
        if (showDeleteDialog && scheduleToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                confirmButton = {
                    Button(onClick = { scheduleToDelete?.let { scheduleViewModel.deleteSchedule(it.id, {}, {}) }; showDeleteDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = RedError)) { Text("Hapus") }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } },
                title = { Text("Hapus Data?") },
                text = { Text("Yakin ingin menghapus?") }
            )
        }
    }
}

// --- CARD DENGAN LOGIKA PEMISAH ADMIN/USER YANG KETAT ---

@Composable
fun CleanScheduleCard(
    schedule: Schedule,
    isAdmin: Boolean,
    isJoined: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onPublish: () -> Unit,
    onMarkAsFinished: () -> Unit,
    onJoinToggle: () -> Unit,
    onReminderClick: () -> Unit
) {
    val dateObj = schedule.date?.toDate() ?: Date()
    val dayStr = SimpleDateFormat("dd", Locale("id")).format(dateObj)
    val monthStr = SimpleDateFormat("MMM", Locale("id")).format(dateObj).uppercase()
    val timeStr = SimpleDateFormat("HH:mm", Locale("id")).format(dateObj)
    val totalAttendees = (schedule.participantsOnline?.size ?: 0) + (schedule.participantsOffline?.size ?: 0)

    val isFinished = schedule.isFinished
    val isDraft = !schedule.isPublished

    // Logic Live: Waktu acara s/d 2 jam setelahnya
    val now = System.currentTimeMillis()
    val isLive = !isFinished && (now >= dateObj.time && now <= dateObj.time + (7200000))

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = if (isLive) BorderStroke(1.dp, RedError) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // BAGIAN ATAS (Tanggal & Info) - TAMPIL UNTUK SEMUA
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(if (isLive) RedError.copy(0.1f) else Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(dayStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if(isLive) RedError else EmeraldDeep)
                    Text(monthStr, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if(isLive) RedError else TextColorSecondary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (isLive) Text("SEDANG BERLANGSUNG", fontSize = 10.sp, color = RedError, fontWeight = FontWeight.Bold)
                    else if (isDraft && isAdmin) Text("DRAFT (Belum Terbit)", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(schedule.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp)
                    Text("$timeStr WIB • Ust. ${schedule.speaker}", fontSize = 14.sp, color = TextColorSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // --- BAGIAN BAWAH (AKSI) - INI PEMISAHNYA ---

            if (isAdmin) {
                // ==================
                // TAMPILAN KHUSUS ADMIN
                // ==================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End, // Tombol Admin rata kanan
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    Spacer(modifier = Modifier.width(8.dp))

                    if (isDraft) {
                        Button(onClick = onPublish, colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep), modifier = Modifier.height(36.dp)) { Text("Publish") }
                    } else if (!isFinished) {
                        Button(onClick = onMarkAsFinished, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), modifier = Modifier.height(36.dp)) { Text("Selesai") }
                    }

                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = RedError) }
                }

            } else {
                // ==================
                // TAMPILAN KHUSUS USER
                // ==================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Info Peserta
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, null, tint = TextColorSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$totalAttendees", fontSize = 12.sp, color = TextColorSecondary)
                    }

                    // Tombol User (Reminder & Join)
                    Row {
                        // Logic Waktu Lewat (Asumsi durasi 2 jam seperti Logic Live)
                        val isTimePassed = now > (dateObj.time + 7200000)
                        val finalIsFinished = isFinished || isTimePassed

                        // Reminder hanya muncul jika belum selesai
                        if (!finalIsFinished) {
                            IconButton(onClick = onReminderClick) { Icon(Icons.Outlined.CalendarToday, null, tint = EmeraldDeep) }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Button(
                            onClick = onJoinToggle,
                            enabled = !finalIsFinished,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (finalIsFinished) Color.Gray else if (isJoined) Color(0xFFE0F2F1) else EmeraldDeep,
                                contentColor = if (finalIsFinished) White else if (isJoined) EmeraldDeep else White,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = White
                            )
                        ) {
                            if (finalIsFinished) {
                                Text("Selesai")
                            } else {
                                Text(if (isJoined) "Terdaftar" else "Gabung")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper Filter Button
@Composable
fun CleanFilterButton(text: String, selected: String, onClick: (String) -> Unit) {
    val isSel = text == selected
    Surface(
        onClick = { onClick(text) },
        shape = RoundedCornerShape(12.dp),
        color = if (isSel) EmeraldDeep else Color.Transparent,
        border = if (!isSel) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text, color = if (isSel) White else TextColorSecondary, fontSize = 13.sp)
        }
    }
}