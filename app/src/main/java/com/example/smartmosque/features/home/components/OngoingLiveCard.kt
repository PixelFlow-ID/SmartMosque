package com.example.smartmosque.features.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmosque.model.Schedule
import com.example.smartmosque.ui.theme.*

@Composable
fun OngoingLiveCard(schedule: Schedule, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFFFF5252).copy(alpha = 0.25f))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFFFEBEE), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red.copy(alpha = alpha)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LIVE SEKARANG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red, letterSpacing = 0.5.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${schedule.time} WIB", fontSize = 12.sp, color = TextColorSecondary, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            Row(verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFEBEE), modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Mic, null, tint = Color.Red, modifier = Modifier.size(24.dp)) }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(schedule.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Ust. ${schedule.speaker}", fontSize = 14.sp, color = TextColorSecondary, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LocationOn, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(schedule.location, fontSize = 12.sp, color = TextColorSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(color = EmeraldDeep, shape = RoundedCornerShape(50), modifier = Modifier.padding(start = 8.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Gabung", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}