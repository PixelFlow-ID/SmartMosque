package com.example.smartmosque.features.schedule.components

/**
 * Component untuk EventScheduleItem dengan fitur Reminder
 * Pisahkan file ini untuk clean code architecture
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmosque.ui.theme.GreenLight
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary

@Composable
fun EventScheduleItemWithReminder(
    title: String,
    ustadz: String,
    time: String,
    location: String,
    attendees: String,
    dateTag: String,
    isReminderSet: Boolean,
    onReminderClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- 1. JUDUL & TAG ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateTag,
                    color = GreenPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(GreenLight, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Pemateri
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ustadz,
                fontSize = 13.sp,
                color = TextColorSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- 2. INFO DETAIL ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = TextColorSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = time, fontSize = 12.sp, color = TextColorSecondary)

                Spacer(Modifier.width(16.dp))

                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextColorSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = location,
                    fontSize = 12.sp,
                    color = TextColorSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextColorSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = attendees,
                    fontSize = 12.sp,
                    color = TextColorSecondary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))

            // --- 3. TOMBOL AKSI dengan Reminder Functionality ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Reminder dengan state
                TextButton(
                    onClick = onReminderClick,
                    contentPadding = PaddingValues(horizontal = 8.dp), // Sedikit padding horizontal agar area sentuh luas
                    modifier = Modifier.weight(1f, fill = false) // Biarkan dia ambil ruang tapi jangan paksa penuh
                ) {
                    Icon(
                        if (isReminderSet) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (isReminderSet) GreenPrimary else TextColorSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isReminderSet) "Pengingat Aktif" else "Ingatkan Saya",
                        color = if (isReminderSet) GreenPrimary else TextColorSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isReminderSet) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onJoinClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Gabung", fontSize = 13.sp)
                }
            }
        }
    }
}
