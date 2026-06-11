package com.example.smartmosque.features.notification.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmosque.features.notification.JamaahNotificationItem
import com.example.smartmosque.model.Donation
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.model.WaqfProject
import com.example.smartmosque.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

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
            Box(modifier = Modifier.size(56.dp).background(statusColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = statusColor, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Infaq: ${donation.category}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary)
                Text(statusText, fontSize = 14.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(amount, fontSize = 15.sp, color = TextColorSecondary)
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
            Box(modifier = Modifier.size(56.dp).background(EmeraldDeep.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CalendarToday, null, tint = EmeraldDeep, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(color = EmeraldDeep, shape = RoundedCornerShape(4.dp)) {
                    Text("JADWAL BARU", fontSize = 11.sp, color = White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(schedule.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary)
                Text("Bersama: ${schedule.speaker}", fontSize = 14.sp, color = TextColorSecondary)
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
            Box(modifier = Modifier.size(56.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.FavoriteBorder, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(color = Color(0xFF8B5CF6), shape = RoundedCornerShape(4.dp)) {
                    Text("PROGRAM WAKAF", fontSize = 11.sp, color = White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(waqf.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary)
                Text("Mari bantu pembangunan umat", fontSize = 14.sp, color = TextColorSecondary)
            }
        }
    }
}

@Composable
fun JamaahGeneralCard(item: JamaahNotificationItem.GeneralNotification) {
    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).background(Color(0xFF0284C7).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Info, null, tint = Color(0xFF0284C7), modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(4.dp)) {
                    Text("INFORMASI", fontSize = 11.sp, color = White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary)
                Text(item.body, fontSize = 14.sp, color = TextColorSecondary)
            }
        }
    }
}