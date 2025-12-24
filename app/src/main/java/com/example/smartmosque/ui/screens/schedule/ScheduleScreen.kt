package com.example.smartmosque.ui.screens.schedule

import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
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

// --- IMPORTS TEMA (Sesuaikan jika path package berbeda) ---
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
    val userRole by authViewModel.userRole.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userId = currentUser?.uid
    val isAdmin = userRole == "admin"

    val scheduleList by scheduleViewModel.schedules.collectAsState()
    val isLoading by scheduleViewModel.isLoading.collectAsState()

    // Default filter. Jika ingin defaultnya langsung 'Semua' saat testing, ubah string di bawah.
    var selectedFilter by remember { mutableStateOf("Akan Datang") }

    // Dialog Hapus
    var showDeleteDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }

    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    // --- FETCH DATA ---
    LaunchedEffect(Unit) {
        Log.d("ScheduleScreen", "Screen opened, fetching data...")
        scheduleViewModel.fetchSchedules()
    }

    // --- DEBUG LOGGING ---
    LaunchedEffect(scheduleList) {
        Log.d("ScheduleScreen", "Data received: ${scheduleList.size} items")
        scheduleList.forEach {
            Log.d("ScheduleScreen", "Item: ${it.title} | Published: ${it.isPublished} | Finished: ${it.isFinished} | Date: ${it.date?.toDate()}")
        }
    }

    // --- LOGIKA FILTER ---
    val filteredList = remember(scheduleList, selectedFilter, isAdmin) {
        // Logika 3 Jam toleransi
        val threeHoursInMillis = 3 * 60 * 60 * 1000
        val cutoffTime = Date(System.currentTimeMillis() - threeHoursInMillis)

        val timeFiltered = when (selectedFilter) {
            "Akan Datang" -> scheduleList.filter {
                // Tampil jika: Belum finish DAN (Waktunya di masa depan ATAU baru lewat dikit)
                !it.isFinished && (it.date?.toDate()?.after(cutoffTime) == true)
            }
            "Selesai" -> scheduleList.filter {
                // Tampil jika: Sudah finish ATAU Waktunya sudah lampau
                it.isFinished || (it.date?.toDate()?.before(cutoffTime) == true)
            }.sortedByDescending { it.date }
            else -> scheduleList // "Semua" -> menampilkan semuanya tanpa filter waktu
        }

        // Filter User Biasa: Hanya yang isPublished = true
        // Admin bisa melihat draft (unpublished)
        if (isAdmin) timeFiltered else timeFiltered.filter { it.isPublished }
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
            // --- HEADER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Dekorasi Icon Background
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = EmeraldDeep.copy(alpha = 0.05f),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(100.dp)
                        .offset(x = 20.dp, y = 0.dp)
                        .rotate(-15f)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(todayDate, fontSize = 12.sp, color = TextColorSecondary, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Jadwal Kajian",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDeep,
                        letterSpacing = (-0.5).sp
                    )
                    Text("Temukan majelis ilmu terdekat.", fontSize = 14.sp, color = TextColorSecondary)
                }
            }

            // --- FILTER TABS (FIXED) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // FIX: Menghapus pengecekan 'if (isAdmin)' agar tab 'Semua' muncul untuk semua user
                CleanFilterButton("Semua", selectedFilter) { selectedFilter = it }
                CleanFilterButton("Akan Datang", selectedFilter) { selectedFilter = it }
                CleanFilterButton("Selesai", selectedFilter) { selectedFilter = it }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- LIST KONTEN ---
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldDeep)
                }
            } else if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.6f)) {
                        Icon(Icons.Default.EventNote, null, tint = TextColorSecondary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tidak ada jadwal yang sesuai.", color = TextColorSecondary, fontSize = 14.sp)

                        // Pesan Debugging (Bisa dihapus nanti kalau sudah beres)
                        if (scheduleList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Debug: Data asli ada (${scheduleList.size}), tapi terfilter.\nCoba klik tab 'Semua' atau 'Selesai'.",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList) { schedule ->
                        // Safe check participant list
                        val isJoined = if (userId != null) schedule.participantsOnline.contains(userId) else false

                        CleanScheduleCard(
                            schedule = schedule,
                            isAdmin = isAdmin,
                            isJoined = isJoined,
                            onClick = { navController.navigate("schedule_detail/${schedule.id}") },
                            onDelete = { scheduleToDelete = schedule; showDeleteDialog = true },
                            onEdit = { navController.navigate("edit_schedule/${schedule.id}") },
                            onPublish = {
                                scheduleViewModel.updateSchedule(schedule.id, mapOf("isPublished" to true),
                                    onSuccess = { Toast.makeText(context, "Diterbitkan!", Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, "Gagal: $it", Toast.LENGTH_SHORT).show() }
                                )
                            },
                            onMarkAsFinished = {
                                scheduleViewModel.updateSchedule(schedule.id, mapOf("isFinished" to true),
                                    onSuccess = { Toast.makeText(context, "Selesai.", Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, "Gagal: $it", Toast.LENGTH_SHORT).show() }
                                )
                            },
                            onJoinToggle = {
                                if (userId == null) {
                                    Toast.makeText(context, "Login dulu", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (isJoined) {
                                        scheduleViewModel.leaveEvent(schedule.id, userId, true, {}, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() })
                                    } else {
                                        scheduleViewModel.joinEvent(schedule.id, userId, true, { Toast.makeText(context, "Bergabung!", Toast.LENGTH_SHORT).show() }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() })
                                    }
                                }
                            },
                            onReminderClick = {
                                val date = schedule.date?.toDate()
                                if (date != null) {
                                    try {
                                        val intent = Intent(Intent.ACTION_INSERT).apply {
                                            data = CalendarContract.Events.CONTENT_URI
                                            putExtra(CalendarContract.Events.TITLE, schedule.title)
                                            putExtra(CalendarContract.Events.EVENT_LOCATION, schedule.location)
                                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.time)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) { Toast.makeText(context, "Tidak ada kalender", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- DIALOG HAPUS ---
        if (showDeleteDialog && scheduleToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = White,
                title = { Text("Hapus Jadwal?") },
                text = { Text("Yakin ingin menghapus ${scheduleToDelete?.title}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scheduleToDelete?.let {
                                scheduleViewModel.deleteSchedule(it.id,
                                    onSuccess = { showDeleteDialog = false; scheduleToDelete = null },
                                    onError = { Toast.makeText(context, "Gagal hapus", Toast.LENGTH_SHORT).show() }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedError)
                    ) { Text("Hapus") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}

// --- KOMPONEN HELPER ---

@Composable
fun CleanFilterButton(text: String, selectedFilter: String, onClick: (String) -> Unit) {
    val isSelected = text == selectedFilter
    Surface(
        onClick = { onClick(text) },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) EmeraldDeep else Color.Transparent,
        border = if (!isSelected) BorderStroke(1.dp, Color.LightGray) else null,
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (isSelected) White else TextColorSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

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
    // Handling null date untuk keamanan
    val dateObj = schedule.date?.toDate() ?: Date()
    val dayStr = SimpleDateFormat("dd", Locale("id", "ID")).format(dateObj)
    val monthStr = SimpleDateFormat("MMM", Locale("id", "ID")).format(dateObj).uppercase()
    val totalAttendees = (schedule.participantsOnline?.size ?: 0) + (schedule.participantsOffline?.size ?: 0)

    val isFinished = schedule.isFinished
    val isPublished = schedule.isPublished

    // Hitung apakah sedang live
    val currentTime = System.currentTimeMillis()
    val eventTime = dateObj.time
    val threeHoursMs = 3 * 60 * 60 * 1000
    val isLive = isPublished && !isFinished && (eventTime <= currentTime && currentTime < (eventTime + threeHoursMs))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = if (isLive) BorderStroke(1.dp, Color.Red) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // TANGGAL
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(if(isLive) Color.Red.copy(0.1f) else EmeraldDeep.copy(0.1f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(dayStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if(isLive) Color.Red else EmeraldDeep)
                    Text(monthStr, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if(isLive) Color.Red else EmeraldDeep)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // KONTEN UTAMA
                Column(modifier = Modifier.weight(1f)) {
                    if (isLive) {
                        Text("SEDANG BERLANGSUNG", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                    } else if (isFinished) {
                        Text("SELESAI", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    } else {
                        Text(schedule.category.uppercase(), fontSize = 10.sp, color = EmeraldDeep, fontWeight = FontWeight.Bold)
                    }

                    Text(schedule.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary, maxLines = 2)
                    Text("Ust. ${schedule.speaker}", fontSize = 14.sp, color = TextColorSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(12.dp))

            // FOOTER BUTTONS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (isAdmin) {
                    Row {
                        TextButton(onClick = onEdit) { Text("Edit") }
                        TextButton(onClick = onDelete) { Text("Hapus", color = RedError) }
                        if (!isPublished) Button(onClick = onPublish, modifier = Modifier.height(35.dp)) { Text("Publish") }
                        else if (!isFinished) Button(onClick = onMarkAsFinished, modifier = Modifier.height(35.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Selesai") }
                    }
                } else {
                    Text("$totalAttendees hadir", fontSize = 12.sp, color = TextColorSecondary)
                    Button(
                        onClick = onJoinToggle,
                        enabled = !isFinished,
                        modifier = Modifier.height(35.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if(isJoined) Color(0xFFE0F2F1) else EmeraldDeep, contentColor = if(isJoined) EmeraldDeep else White)
                    ) {
                        Text(if(isJoined) "Terdaftar" else "Hadir")
                    }
                }
            }
        }
    }
}