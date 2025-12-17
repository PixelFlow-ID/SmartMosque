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
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
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

    // Dialog Hapus
    var showDeleteDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }

    // Format Tanggal Hari Ini
    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date())
    }

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
                                participantsOffline = offlineList,
                                streamingUrl = doc.getString("streamingUrl") ?: "",
                                isPublished = doc.getBoolean("isPublished") ?: true,
                                isFinished = doc.getBoolean("isFinished") ?: false
                            )
                        } catch (err: Exception) { null }
                    }
                    scheduleList = list
                }
            }
    }

    // --- LOGIKA FILTER ---
    val filteredList = remember(scheduleList, selectedFilter, isAdmin) {
        val threeHoursInMillis = 3 * 60 * 60 * 1000
        val cutoffTime = Date(System.currentTimeMillis() - threeHoursInMillis)

        val timeFiltered = when (selectedFilter) {
            "Akan Datang" -> scheduleList.filter {
                // Tampil jika: Belum ditandai selesai DAN (Waktunya belum lewat jauh ATAU Masih live)
                !it.isFinished && it.date?.toDate()?.after(cutoffTime) == true
            }
            "Selesai" -> scheduleList.filter {
                // Tampil jika: Sudah ditandai selesai ATAU Waktunya sudah lewat jauh
                it.isFinished || it.date?.toDate()?.before(cutoffTime) == true
            }.sortedByDescending { it.date } // Urutkan dari yang baru selesai
            else -> scheduleList
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

            // --- FILTER TABS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CleanFilterButton("Akan Datang", selectedFilter) { selectedFilter = it }
                CleanFilterButton("Selesai", selectedFilter) { selectedFilter = it }
                if (isAdmin) CleanFilterButton("Semua", selectedFilter) { selectedFilter = it }
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

                            // Aksi Publish
                            onPublish = {
                                Firebase.firestore.collection("schedules").document(schedule.id)
                                    .update("isPublished", true)
                                    .addOnSuccessListener { Toast.makeText(context, "Diterbitkan!", Toast.LENGTH_SHORT).show() }
                            },

                            // Aksi Mark as Finished
                            onMarkAsFinished = {
                                Firebase.firestore.collection("schedules").document(schedule.id)
                                    .update("isFinished", true)
                                    .addOnSuccessListener { Toast.makeText(context, "Kajian ditandai selesai.", Toast.LENGTH_SHORT).show() }
                            },

                            onJoinToggle = {
                                if (userId == null) {
                                    Toast.makeText(context, "Silakan login dulu", Toast.LENGTH_SHORT).show()
                                } else {
                                    val docRef = Firebase.firestore.collection("schedules").document(schedule.id)
                                    if (isJoined) {
                                        docRef.update("participantsOnline", FieldValue.arrayRemove(userId))
                                    } else {
                                        docRef.update("participantsOnline", FieldValue.arrayUnion(userId))
                                            .addOnSuccessListener { Toast.makeText(context, "Berhasil bergabung!", Toast.LENGTH_SHORT).show() }
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
                                            putExtra(CalendarContract.Events.DESCRIPTION, "Pembicara: ${schedule.speaker}")
                                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.time)
                                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, date.time + (2 * 60 * 60 * 1000))
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) { Toast.makeText(context, "Tidak ada aplikasi kalender", Toast.LENGTH_SHORT).show() }
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
                onDismissRequest = { showDeleteDialog = false; scheduleToDelete = null },
                containerColor = White,
                title = { Text("Hapus Jadwal?", fontWeight = FontWeight.Bold, color = TextColorPrimary) },
                text = { Text("Data \"${scheduleToDelete?.title}\" akan dihapus permanen.") },
                confirmButton = {
                    Button(
                        onClick = {
                            scheduleToDelete?.let { Firebase.firestore.collection("schedules").document(it.id).delete() }
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

// --- KOMPONEN FILTER BUTTON ---
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

// --- KOMPONEN CARD (UPDATED: COLOR LOGIC FIX) ---
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
    val dayStr = SimpleDateFormat("dd", Locale("id", "ID")).format(dateObj)
    val monthStr = SimpleDateFormat("MMM", Locale("id", "ID")).format(dateObj).uppercase()
    val totalAttendees = schedule.participantsOnline.size + schedule.participantsOffline.size
    val isPublished = schedule.isPublished
    val isFinished = schedule.isFinished

    val currentTime = System.currentTimeMillis()
    val eventTime = dateObj.time
    val threeHoursMs = 3 * 60 * 60 * 1000

    // Logic Live: Harus Published, Waktunya masuk, DAN BELUM DITANDAI SELESAI
    val isLive = isPublished && !isFinished && (eventTime <= currentTime && currentTime < (eventTime + threeHoursMs))

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by if (isLive) infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
    ) else remember { mutableFloatStateOf(0f) }

    val cardAlpha = if (isFinished) 0.7f else 1f

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

            // BAGIAN ATAS
            Row(verticalAlignment = Alignment.Top) {

                // --- LOGIC WARNA TANGGAL (FIXED) ---
                // Live -> Merah
                // Selesai -> Abu
                // Default (Akan Datang) -> Hijau Emerald
                val (dateColor, dateBgColor) = when {
                    isLive -> Pair(Color.Red, Color.Red.copy(0.05f))
                    isFinished -> Pair(TextColorSecondary, Color(0xFFF5F5F5))
                    else -> Pair(EmeraldDeep, EmeraldDeep.copy(0.1f))
                }

                // Tanggal Box
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
                    // Badge Logic
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLive) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red.copy(alpha = pulseAlpha)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SEDANG BERLANGSUNG", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        } else if (isFinished) {
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

            // FOOTER BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAdmin) {
                    // TOMBOL ADMIN
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) { Text("Edit") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) { Text("Hapus", color = RedError) }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (!isPublished) {
                            Button(onClick = onPublish, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep)) { Text("Post", fontSize = 12.sp) }
                        } else if (!isFinished) {
                            Button(
                                onClick = onMarkAsFinished,
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B))
                            ) {
                                Text("Selesai", fontSize = 12.sp, color = White)
                            }
                        }
                    }
                } else {
                    // --- TAMPILAN USER ---
                    Text("$totalAttendees akan hadir", fontSize = 12.sp, color = TextColorSecondary)
                    Row {
                        // Tombol Reminder
                        FilledIconButton(
                            onClick = onReminderClick,
                            enabled = !isFinished, // Matikan jika selesai
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFF5F5F5),
                                disabledContainerColor = Color(0xFFEEEEEE)
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.NotificationsActive,
                                null,
                                tint = if (isFinished) Color.Gray else Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tombol Join / Hadir
                        Button(
                            onClick = onJoinToggle,
                            enabled = !isFinished, // Matikan jika selesai
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isJoined) Color(0xFFE0F2F1) else EmeraldDeep,
                                contentColor = if (isJoined) EmeraldDeep else White,
                                disabledContainerColor = Color.LightGray,
                                disabledContentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(36.dp),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            // Text Button
                            Text(
                                text = when {
                                    isFinished -> "Selesai"
                                    isJoined -> "Terdaftar"
                                    else -> "Hadir"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}