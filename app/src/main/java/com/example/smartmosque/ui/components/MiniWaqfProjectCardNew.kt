package com.example.smartmosque.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartmosque.data.model.WaqfProject
import com.example.smartmosque.ui.theme.Screen
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

// --- IMPORT WARNA TEMA ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.GrayInputBackground
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.GoldAccent
import com.example.smartmosque.ui.theme.EmeraldLight
import com.example.smartmosque.ui.theme.BgPremium

@Composable
fun MiniWaqfProjectNew(navController: NavController) {
    var waqfList by remember { mutableStateOf<List<WaqfProject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("waqf_programs")
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            WaqfProject(
                                id = doc.id,
                                title = doc.getString("title") ?: "Tanpa Judul",
                                description = doc.getString("description") ?: "",
                                targetAmount = doc.getLong("targetAmount") ?: 0L,
                                collectedAmount = doc.getLong("collectedAmount") ?: 0L,
                                imageUrl = doc.getString("imageUrl") ?: ""
                            )
                        } catch (err: Exception) { null }
                    }
                    waqfList = list.take(5)
                }
            }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Program Wakaf",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextColorPrimary
            )
            TextButton(onClick = { navController.navigate(Screen.Donation.route) }) {
                Text("Lihat Semua", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(30.dp))
            }
        } else if (waqfList.isEmpty()) {
            // Empty State
            Surface(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(16.dp),
                color = White,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Belum ada program wakaf.", color = TextColorSecondary, fontSize = 12.sp)
                }
            }
        } else {
            // List Horizontal
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 16.dp, bottom = 8.dp) // Bottom padding untuk shadow
            ) {
                items(waqfList) { project ->
                    WaqfPremiumCard(
                        project = project,
                        onClick = { navController.navigate(Screen.createRoute(project.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun WaqfPremiumCard(
    project: WaqfProject,
    onClick: () -> Unit
) {
    val progress = if (project.targetAmount > 0)
        project.collectedAmount.toFloat() / project.targetAmount.toFloat()
    else 0f
    val percentage = (progress * 100).toInt()

    // KARTU UTAMA
    Surface(
        modifier = Modifier
            .width(260.dp)
            .height(230.dp) // Sedikit lebih tinggi agar proporsional
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp), // Sudut sangat tumpul (Squircle-ish)
        color = White,
        shadowElevation = 6.dp, // Shadow dalam agar 'pop' dari background krem
        tonalElevation = 1.dp
    ) {
        Column {
            // --- AREA GAMBAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp) // Gambar lebih dominan
                    .background(GrayInputBackground)
            ) {
                if (project.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = project.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder Elegan
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = EmeraldDeep.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // --- BADGE PREMIUM (KIRI ATAS) ---
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    shape = CircleShape, // Kapsul
                    color = White.copy(alpha = 0.95f), // Putih hampir solid agar terbaca
                    shadowElevation = 4.dp // Shadow pada badge
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Titik Emas
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wakaf",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDeep
                        )
                    }
                }
            }

            // --- AREA INFO ---
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Judul
                Text(
                    text = project.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextColorPrimary
                )

                // Progress Info
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Terkumpul", fontSize = 11.sp, color = TextColorSecondary)
                        Text(
                            "$percentage%",
                            fontSize = 14.sp,
                            color = EmeraldDeep, // Gunakan Hijau Emerald
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = GoldAccent, // Progress warna Emas agar mewah
                        trackColor = GrayInputBackground
                    )
                }
            }
        }
    }
}
