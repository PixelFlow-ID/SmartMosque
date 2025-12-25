package com.example.smartmosque.features.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// --- IMPORT WARNA TEMA ---
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.GrayInputBackground
import com.example.smartmosque.ui.theme.GrayInactive

@Composable
fun AddScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // State Input
    var title by remember { mutableStateOf("") }
    var speaker by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var streamingUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Pengajian") }

    // State Waktu
    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var startTime by remember { mutableStateOf("18:00") }
    var endTime by remember { mutableStateOf("20:00") }

    var isLoading by remember { mutableStateOf(false) }

    // Format Tanggal
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    val combinedCalendar = Calendar.getInstance()

    // --- FUNGSI SIMPAN DATA (REUSABLE) ---
    fun saveData(isPublished: Boolean) {
        if (title.isBlank() || speaker.isBlank() || location.isBlank()) {
            Toast.makeText(context, "Mohon lengkapi data utama", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        // Logic Timestamp
        combinedCalendar.time = selectedDate
        val startParts = startTime.split(":")
        if (startParts.size == 2) {
            combinedCalendar.set(Calendar.HOUR_OF_DAY, startParts[0].toInt())
            combinedCalendar.set(Calendar.MINUTE, startParts[1].toInt())
            combinedCalendar.set(Calendar.SECOND, 0)
            combinedCalendar.set(Calendar.MILLISECOND, 0)
        }

        val newEvent = hashMapOf(
            "title" to title,
            "speaker" to speaker,
            "location" to location,
            "category" to category,
            "streamingUrl" to streamingUrl,
            "time" to "$startTime - $endTime",
            "date" to Timestamp(combinedCalendar.time),
            "participantsOnline" to emptyList<String>(),
            "participantsOffline" to emptyList<String>(),
            "createdAt" to Timestamp.now(),
            "isPublished" to isPublished,
            "isFinished" to false
        )

        firestore.collection("schedules")
            .add(newEvent)
            .addOnSuccessListener {
                isLoading = false
                val message = if (isPublished) "Jadwal berhasil ditayangkan!" else "Disimpan sebagai Draft"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        containerColor = BgPremium,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            ) {
                Surface(
                    onClick = { navController.popBackStack() },
                    shape = CircleShape,
                    color = White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextColorPrimary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Buat Jadwal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Text("Isi detail kegiatan masjid", fontSize = 12.sp, color = TextColorSecondary)
                }
            }

            // 2. FORM UTAMA
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PremiumTextField(title, { title = it }, "Nama Kegiatan", Icons.Default.Title)
                    PremiumTextField(speaker, { speaker = it }, "Nama Pemateri / Ustadz", Icons.Default.Mic)
                    PremiumTextField(location, { location = it }, "Lokasi", Icons.Default.LocationOn)
                    PremiumTextField(streamingUrl, { streamingUrl = it }, "Link YouTube (Opsional)", Icons.Default.Link, isLast = true)
                }
            }

            // 3. KATEGORI
            Column {
                Text("Kategori", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColorPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Pengajian", "Kajian Subuh", "Jumat", "PHBI", "Remaja").forEach { cat ->
                        val isSelected = category == cat
                        Surface(
                            onClick = { category = cat },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldDeep else White,
                            border = if (!isSelected) BorderStroke(1.dp, GrayInputBackground) else null,
                            shadowElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Outlined.Check, null, tint = White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    color = if (isSelected) White else TextColorSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 4. WAKTU & TANGGAL
            Column {
                Text("Waktu Pelaksanaan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColorPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            val newCal = Calendar.getInstance()
                            newCal.set(year, month, day)
                            selectedDate = newCal.time
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(BgPremium, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = EmeraldDeep)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Tanggal Acara", fontSize = 10.sp, color = TextColorSecondary)
                            Text(dateFormat.format(selectedDate), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimePickerCard("Mulai", startTime, Modifier.weight(1f)) {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(context, { _, h, m -> startTime = String.format("%02d:%02d", h, m) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                    }
                    TimePickerCard("Selesai", endTime, Modifier.weight(1f)) {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(context, { _, h, m -> endTime = String.format("%02d:%02d", h, m) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. ACTION BUTTONS (DUA TOMBOL)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldDeep)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- TOMBOL 1: SIMPAN DRAFT ---
                    OutlinedButton(
                        onClick = { saveData(isPublished = false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, EmeraldDeep),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldDeep)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draft", fontWeight = FontWeight.Bold)
                    }

                    // --- TOMBOL 2: PUBLIKASIKAN SEKARANG ---
                    Button(
                        onClick = { saveData(isPublished = true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = EmeraldDeep.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publikasi", fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- KOMPONEN PENDUKUNG TETAP SAMA ---
// (PremiumTextField dan TimePickerCard yang sudah Anda buat sebelumnya)
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isLast: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, tint = EmeraldDeep) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldDeep,
            unfocusedBorderColor = GrayInactive,
            focusedLabelColor = EmeraldDeep,
            cursorColor = EmeraldDeep,
            unfocusedContainerColor = BgPremium.copy(alpha = 0.3f),
            focusedContainerColor = White
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next
        ),
        singleLine = true
    )
}

@Composable
fun TimePickerCard(
    label: String,
    time: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = White,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BgPremium, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccessTime, null, tint = EmeraldDeep)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 10.sp, color = TextColorSecondary)
                Text(time, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
            }
        }
    }
}