package com.example.smartmosque.utils

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary

@Composable
fun UpcommingEventBox(navController: NavController, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val userId = currentUser?.uid
    var eventList by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("schedules").orderBy("date", Query.Direction.ASCENDING).addSnapshotListener { snapshot, _ ->
            isLoading = false
            if (snapshot != null) {
                val now = Date()
                val validEvents = mutableListOf<Schedule>()
                snapshot.documents.forEach { doc ->
                    try {
                        val firestoreTimestamp = doc.getTimestamp("date")
                        val eventDate = firestoreTimestamp?.toDate()
                        @Suppress("UNCHECKED_CAST") val onlineList = (doc.get("participantsOnline") as? List<String>) ?: emptyList()
                        @Suppress("UNCHECKED_CAST") val offlineList = (doc.get("participantsOffline") as? List<String>) ?: emptyList()
                        val schedule = Schedule(id = doc.id, title = doc.getString("title") ?: "", speaker = doc.getString("speaker") ?: "", time = doc.getString("time") ?: "", location = doc.getString("location") ?: "", category = doc.getString("category") ?: "Pengajian", date = firestoreTimestamp, participantsOnline = onlineList, participantsOffline = offlineList, streamingUrl = doc.getString("streamingUrl") ?: "")
                        if (eventDate != null && eventDate.after(now)) validEvents.add(schedule)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                eventList = validEvents.take(3)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Agenda Mendatang", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
            TextButton(onClick = { navController.navigate(Screen.Schedule.route) }) {
                Text("Lihat Semua", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(30.dp)) }
        } else if (eventList.isEmpty()) {
            // EMPTY STATE YANG LEBIH CANTIK
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp) // Lebih tinggi
                    .background(White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventBusy, // Ikon Kalender Silang
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tidak ada jadwal mendatang.", color = TextColorSecondary, fontSize = 12.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                eventList.forEach { schedule ->
                    val isJoined = if (userId != null) schedule.participantsOnline.contains(userId) else false
                    PremiumScheduleCard(
                        schedule = schedule,
                        isJoined = isJoined,
                        onCardClick = { navController.navigate("schedule_detail/${schedule.id}") },
                        onJoinClick = {
                            if (userId == null) { Toast.makeText(context, "Silakan login dulu", Toast.LENGTH_SHORT).show() }
                            else {
                                val docRef = Firebase.firestore.collection("schedules").document(schedule.id)
                                if (isJoined) docRef.update("participantsOnline", FieldValue.arrayRemove(userId))
                                else docRef.update("participantsOnline", FieldValue.arrayUnion(userId))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumScheduleCard(schedule: Schedule, isJoined: Boolean, onCardClick: () -> Unit, onJoinClick: () -> Unit) {
    val date = schedule.date?.toDate()
    val dateStr = if (date != null) SimpleDateFormat("dd MMM", Locale("id", "ID")).format(date) else "-"
    val parts = dateStr.split(" ")
    val day = parts.getOrElse(0) { "" }; val month = parts.getOrElse(1) { "" }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        color = White,
        shadowElevation = 3.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(50.dp), shape = RoundedCornerShape(14.dp), color = GreenPrimary.copy(alpha = 0.1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(day, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    Text(month, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GreenPrimary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = schedule.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextColorPrimary, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextColorSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = schedule.speaker, fontSize = 12.sp, color = TextColorSecondary, maxLines = 1)
                }
            }
            Surface(onClick = onJoinClick, shape = RoundedCornerShape(50), color = if (isJoined) Color(0xFFF5F5F5) else GreenPrimary, modifier = Modifier.height(32.dp)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isJoined) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = TextColorSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hadir", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextColorSecondary)
                    } else {
                        Text("Gabung", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = White)
                    }
                }
            }
        }
    }
}