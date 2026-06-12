package com.example.smartmosque.features.donation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smartmosque.model.WaqfProject
import com.example.smartmosque.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PremiumWaqfCard(
    project: WaqfProject,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val progressRaw = if (project.targetAmount > 0) project.collectedAmount.toDouble() / project.targetAmount.toDouble() else 0.0
    val progressPercent = (progressRaw * 100).toInt().coerceIn(0, 100)

    val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRupiah.maximumFractionDigits = 0
    val collectedStr = formatRupiah.format(project.collectedAmount)
    val targetStr = formatRupiah.format(project.targetAmount)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.06f))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                if (project.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = project.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFFF0FDF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, null, tint = EmeraldDeep.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    }
                }

                Surface(
                    modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd),
                    shape = RoundedCornerShape(12.dp),
                    color = White,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "$progressPercent%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                    }
                }

                // --- TOMBOL EDIT & DELETE KHUSUS ADMIN ---
                if (isAdmin) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tombol Edit
                        Surface(
                            modifier = Modifier.clickable { onEdit() },
                            shape = CircleShape,
                            color = White.copy(alpha = 0.9f),
                            shadowElevation = 4.dp
                        ) {
                            Icon(Icons.Default.Edit, null, tint = EmeraldDeep, modifier = Modifier.padding(8.dp).size(20.dp))
                        }

                        // Tombol Delete
                        Surface(
                            modifier = Modifier.clickable { onDelete() },
                            shape = CircleShape,
                            color = White.copy(alpha = 0.9f),
                            shadowElevation = 4.dp
                        ) {
                            Icon(Icons.Default.Delete, null, tint = RedError, modifier = Modifier.padding(8.dp).size(20.dp))
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "SEDANG BERJALAN", fontSize = 10.sp, color = GreenPrimary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = project.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))

                ShimmerProgressBar(currentAmount = project.collectedAmount.toDouble(), targetAmount = project.targetAmount.toDouble())

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Terkumpul", fontSize = 11.sp, color = TextColorSecondary)
                        Text(text = collectedStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    }
                    Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color.LightGray.copy(alpha = 0.5f)))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target", fontSize = 11.sp, color = TextColorSecondary)
                        Text(text = targetStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerProgressBar(currentAmount: Double, targetAmount: Double, modifier: Modifier = Modifier) {
    val progressRaw = if (targetAmount > 0) currentAmount / targetAmount else 0.0
    val progressClamped = progressRaw.toFloat().coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progressClamped,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "fill"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "shimmer_move"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(EmeraldDeep, Color(0xFF4ADE80), EmeraldDeep),
        start = Offset(shimmerTranslate - 300f, 0f), end = Offset(shimmerTranslate, 0f)
    )

    Box(modifier = modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE2E8F0))) {
        Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(RoundedCornerShape(50)).background(brush = shimmerBrush))
    }
}