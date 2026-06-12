package com.example.smartmosque.features.schedule

import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.ui.theme.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- WARNA TEMA ---
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

    val isAdmin = userRole?.trim()?.equals("admin", ignoreCase = true) == true

    // Ambil Data dari ViewModel (Bukan direct Firebase)
    val scheduleList by scheduleViewModel.schedules.collectAsState()
    val isLoading by scheduleViewModel.isLoading.collectAsState()

    var selectedFilter by remember { mutableStateOf("Akan Datang") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }

    val todayDate = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date()) }

    // --- LOGIKA FILTER TERBARU (PRESISI MEMBACA JAM DAN TANGGAL) ---
    val filteredList = remember(scheduleList, selectedFilter, isAdmin) {
        val currentTime = System.currentTimeMillis()

        val timeFiltered = scheduleList.filter { schedule ->
            val dateObj = schedule.date?.toDate()
            val finalEventTime = if (dateObj != null) {
                // Gabungkan tanggal dari Firebase dengan string jam ("04:50")
                val calendar = Calendar.getInstance().apply { time = dateObj }
                try {
                    // Ambil jam awal sebelum tanda strip "-" (misal "04:50 - 06:00" -> "04:50")
                    val hourStart = schedule.time.split("-")[0].trim()
                    val timeParts = hourStart.split(":")
                    calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    calendar.set(Calendar.MINUTE, timeParts[1].toInt())
                } catch (e: Exception) {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                }
                calendar.timeInMillis
            } else {
                0L
            }

            // Batas toleransi: Event dianggap berakhir 2 jam setelah waktu mulai kajian
            val isTimeOver = currentTime > (finalEventTime + (2 * 60 * 60 * 1000))

            when (selectedFilter) {
                "Akan Datang" -> !schedule.isFinished && !isTimeOver
                "Selesai" -> schedule.isFinished || isTimeOver
                else -> true // Pilihan filter "Semua" untuk Admin
            }
        }.run {
            // Urutan sorting: "Akan Datang" dari paling dekat, "Selesai" dari paling baru berlalu
            if (selectedFilter == "Selesai") sortedByDescending { it.date } else sortedBy { it.date }
        }

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
                ) { Icon(Icons.Default.Add, contentDescription = "Tambah Jadwal") }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = EmeraldDeep.copy(alpha = 0.05f),
                        modifier = Modifier.align(Alignment.CenterEnd).size(100.dp).offset(x = 20.dp, y = 0.dp).rotate(-15f)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(todayDate, fontSize = 12.sp, color = TextColorSecondary, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Jadwal Kajian", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep, letterSpacing = (-0.5).sp)
                        Text("Temukan majelis ilmu terdekat.", fontSize = 14.sp, color = TextColorSecondary)
                    }
                }

                // Filter Tabs
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CleanFilterButton("Akan Datang", selectedFilter) { selectedFilter = it }
                    CleanFilterButton("Selesai", selectedFilter) { selectedFilter = it }
                    if (isAdmin) CleanFilterButton("Semua", selectedFilter) { selectedFilter = it }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // List Jadwal
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = EmeraldDeep) }
                } else if (filteredList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.6f)) {
                            Icon(Icons.Default.EventNote, null, tint = TextColorSecondary, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Tidak ada jadwal pada filter ini.", color = TextColorSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredList) { schedule ->
                            val isJoined = if (userId != null) schedule.participantsOnline.contains(userId) else false

                            CleanScheduleCard(
                                schedule = schedule,
                                isAdmin = isAdmin,
                                isJoined = isJoined,
                                onClick = { navController.navigate("schedule_detail/${schedule.id}") },
                                onDelete = { scheduleToDelete = schedule; showDeleteDialog = true },
                                onEdit = { navController.navigate("edit_schedule/${schedule.id}") },
                                onPublish = {
                                    scheduleViewModel.publishSchedule(schedule.id) {
                                        Toast.makeText(context, "Jadwal Diterbitkan", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onMarkAsFinished = {
                                    scheduleViewModel.markAsFinished(schedule.id) {
                                        Toast.makeText(context, "Ditandai Selesai", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onJoinToggle = {
                                    if (userId == null) {
                                        Toast.makeText(context, "Silakan login dulu", Toast.LENGTH_SHORT).show()
                                    } else {
                                        scheduleViewModel.toggleJoin(
                                            scheduleId = schedule.id,
                                            userId = userId,
                                            isJoined = isJoined,
                                            onSuccess = { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                onReminderClick = { activeStatus ->
                                    scheduleViewModel.toggleReminder(
                                        context = context,
                                        schedule = schedule,
                                        isReminderActive = activeStatus,
                                        onResult = { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- DIALOG HAPUS ---
        if (showDeleteDialog && scheduleToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false; scheduleToDelete = null },
                containerColor = White,
                title = { Text("Hapus Jadwal?", fontWeight = FontWeight.Bold, color = TextColorPrimary) },
                text = { Text("Jadwal \"${scheduleToDelete?.title}\" akan dihapus permanen.") },
                confirmButton = {
                    Button(
                        onClick = {
                            scheduleToDelete?.let {
                                scheduleViewModel.deleteSchedule(it.id) {
                                    Toast.makeText(context, "Jadwal berhasil dihapus", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showDeleteDialog = false; scheduleToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedError)
                    ) { Text("Hapus", color = White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false; scheduleToDelete = null }) {
                        Text("Batal", color = TextColorSecondary)
                    }
                }
            )
        }
    }
}

// ==========================================
// COMPONENT: CLEAN FILTER BUTTON
// ==========================================
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

// ==========================================
// COMPONENT: CLEAN SCHEDULE CARD
// ==========================================
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
    onReminderClick: (Boolean) -> Unit
) {
    var isReminderActive by remember { mutableStateOf(false) }

    val dateObj = try {
        schedule.date?.toDate() ?: Date()
    } catch (e: Exception) {
        Date()
    }

    // Rekonstruksi waktu penuh (Tanggal + Jam mulai kajian)
    val finalEventTime = remember(dateObj, schedule.time) {
        val calendar = Calendar.getInstance().apply { time = dateObj }
        try {
            val hourStart = schedule.time.split("-")[0].trim()
            val timeParts = hourStart.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].toInt())
        } catch (e: Exception) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
        }
        calendar.timeInMillis
    }

    val dayStr = SimpleDateFormat("dd", Locale("id", "ID")).format(dateObj)
    val monthStr = SimpleDateFormat("MMM", Locale("id", "ID")).format(dateObj).uppercase()
    val totalAttendees = schedule.participantsOnline.size + schedule.participantsOffline.size
    val isPublished = schedule.isPublished
    val isFinished = schedule.isFinished

    val currentTime = System.currentTimeMillis()
    val twoHoursMs = 2 * 60 * 60 * 1000

    // Evaluasi status dinamis berdasarkan gabungan jam aslinya
    val isEventEnded = isFinished || (currentTime > (finalEventTime + twoHoursMs))
    val isLive = isPublished && !isEventEnded && (currentTime >= finalEventTime)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by if (isLive) infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
    ) else remember { mutableFloatStateOf(0f) }

    val cardAlpha = if (isEventEnded) 0.7f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .shadow(
                elevation = if (isLive) 8.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isLive) Color.Red.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = if (isLive) BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                val (dateColor, dateBgColor) = when {
                    isLive -> Pair(Color.Red, Color.Red.copy(0.05f))
                    isEventEnded -> Pair(TextColorSecondary, Color(0xFFF5F5F5))
                    else -> Pair(EmeraldDeep, EmeraldDeep.copy(0.1f))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(dateBgColor, RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Text(dayStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = dateColor)
                    Text(monthStr, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = dateColor)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLive) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red.copy(alpha = pulseAlpha)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SEDANG BERLANGSUNG", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        } else if (isEventEnded) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SELESAI", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                schedule.category.uppercase(),
                                fontSize = 10.sp,
                                color = EmeraldDeep,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(schedule.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ust. ${schedule.speaker}", fontSize = 14.sp, color = TextColorSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(schedule.time, fontSize = 12.sp, color = TextColorSecondary)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.LocationOn, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(schedule.location, fontSize = 12.sp, color = TextColorSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) { Text("Edit", color = EmeraldDeep) }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) { Text("Hapus", color = RedError) }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (!isPublished) {
                            Button(onClick = onPublish, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep)) { Text("Post", fontSize = 12.sp) }
                        } else if (!isEventEnded) {
                            Button(onClick = onMarkAsFinished, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B))) { Text("Tandai Selesai", fontSize = 12.sp, color = White) }
                        }
                    }
                } else {
                    Text("$totalAttendees akan hadir", fontSize = 12.sp, color = TextColorSecondary)
                    Row {
                        FilledIconButton(
                            onClick = {
                                isReminderActive = !isReminderActive
                                onReminderClick(isReminderActive)
                            },
                            enabled = !isEventEnded,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isReminderActive) Color(0xFFFFF3E0) else Color(0xFFF5F5F5),
                                disabledContainerColor = Color(0xFFEEEEEE)
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = if (isEventEnded) Color.Gray else if (isReminderActive) Color(0xFFFF9800) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onJoinToggle,
                            enabled = !isEventEnded,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isJoined) Color(0xFFE0F2F1) else EmeraldDeep,
                                contentColor = if (isJoined) EmeraldDeep else White,
                                disabledContainerColor = Color(0xFFEEEEEE),
                                disabledContentColor = Color.Gray
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(36.dp),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(text = when { isEventEnded -> "Selesai"; isJoined -> "Terdaftar"; else -> "Hadir" }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}