package com.example.smartmosque.features.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.EmeraldLight

private val GoldAccent = Color(0xFFFFD700)

@Composable
fun AnimatedEmeraldCard(
    eventsThisMonth: Int,
    totalParticipants: Int
) {
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    val donutProgress by animateFloatAsState(
        targetValue = if (startAnimation) 0.75f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "donut"
    )

    val bar1 by animateDpAsState(if (startAnimation) 15.dp else 0.dp, tween(1000), label = "b1")
    val bar2 by animateDpAsState(if (startAnimation) 30.dp else 0.dp, tween(1000, 100), label = "b2")
    val bar3 by animateDpAsState(if (startAnimation) 20.dp else 0.dp, tween(1000, 200), label = "b3")
    val bar4 by animateDpAsState(if (startAnimation) 35.dp else 0.dp, tween(1000, 300), label = "b4")

    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldDeep)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.offset(x = 200.dp, y = (-50).dp).size(250.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))

            Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // KIRI: Grafik Donut
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = Color.White.copy(alpha = 0.1f), strokeWidth = 4.dp)
                            CircularProgressIndicator(progress = { donutProgress }, modifier = Modifier.fillMaxSize(), color = EmeraldLight, strokeWidth = 4.dp, strokeCap = StrokeCap.Round)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Kegiatan", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Bulan Ini", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = eventsThisMonth.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Box(modifier = Modifier.width(1.dp).height(60.dp).background(Color.White.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.width(24.dp))

                // KANAN: Grafik Batang
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(24.dp)) {
                            GoldBar(bar1)
                            GoldBar(bar2)
                            GoldBar(bar3)
                            GoldBar(bar4)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Total", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Kehadiran", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = totalParticipants.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun GoldBar(height: Dp) {
    Box(modifier = Modifier.width(5.dp).height(height).clip(RoundedCornerShape(4.dp)).background(GoldAccent))
}