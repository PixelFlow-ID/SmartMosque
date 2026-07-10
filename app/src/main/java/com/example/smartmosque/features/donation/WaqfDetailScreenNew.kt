package com.example.smartmosque.features.donation


import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.features.donation.components.ManualTransferDialogNew
import com.example.smartmosque.model.WaqfProject
import com.example.smartmosque.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaqfDetailScreenNew(
    navController: NavController,
    projectId: String?,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    var project by remember { mutableStateOf<WaqfProject?>(null) }
    var amountText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    // STATE UNTUK TRIGGER ANIMASI MASUK
    var isVisible by remember { mutableStateOf(false) }

    // Fetch Data Project
    LaunchedEffect(projectId) {
        if (projectId != null) {
            FirebaseFirestore.getInstance().collection("waqf_programs").document(projectId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.toObject(WaqfProject::class.java)
                        project = data?.copy(id = snapshot.id)
                        isVisible = true
                    }
                }
        }
    }

    Scaffold(
        containerColor = BgPremium,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    Surface(
                        onClick = { navController.popBackStack() },
                        shape = CircleShape,
                        color = White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(start = 16.dp, top = 8.dp)
                            .size(40.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.Black
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        if (project == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldDeep)
            }
        } else {
            val p = project!!

            // --- Inisiasi Format
            val formatRupiah = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply { maximumFractionDigits = 0 }
            val collectedStr = formatRupiah.format(p.collectedAmount)
            val targetStr = formatRupiah.format(p.targetAmount)

            // --- ANIMASI PROGRESS BAR (LENGTH) ---
            val targetProgress = if (p.targetAmount > 0) p.collectedAmount.toFloat() / p.targetAmount.toFloat() else 0f
            val animatedProgress by animateFloatAsState(
                targetValue = if (isVisible) targetProgress else 0f,
                animationSpec = tween(durationMillis = 1500, delayMillis = 500, easing = FastOutSlowInEasing),
                label = "ProgressLength"
            )

            // --- ANIMASI KILAUAN (SHIMMER) ---
            val infiniteTransition = rememberInfiniteTransition(label = "shimmer_tx")
            val shimmerTranslateAnim by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f, // Jarak tempuh cahaya (lebar layar++)
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmer_anim"
            )

            // Brush Kilauan: Transparan -> Putih -> Transparan
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.5f),
                    Color.Transparent
                ),
                start = Offset(shimmerTranslateAnim - 300f, shimmerTranslateAnim - 300f), // Miring diagonal
                end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // 1. HERO IMAGE
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 800))
                ) {
                    Box(modifier = Modifier.height(350.dp).fillMaxWidth()) {
                        AsyncImage(
                            model = p.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                        startY = 300f
                                    )
                                )
                        )
                    }
                }

                // KONTEN BAWAH
                Column(
                    modifier = Modifier
                        .offset(y = (-50).dp)
                        .padding(horizontal = 20.dp)
                ) {
                    // 2. KARTU UTAMA
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(800, delayMillis = 100)) +
                                slideInVertically(tween(800, delayMillis = 100)) { 50 }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = White)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Surface(
                                    color = EmeraldDeep.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Program Wakaf",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDeep,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = p.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextColorPrimary,
                                    lineHeight = 28.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                // --- PROGRESS BAR DENGAN KILAUAN ---
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(GrayInputBackground)
                                ) {
                                    // Layer 1: Warna Hijau Dasar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(Brush.horizontalGradient(listOf(EmeraldDeep, GreenPrimary)))
                                    )

                                    // Layer 2: Efek Kilauan Cahaya (Overlay)
                                    // Hanya muncul di atas area yang hijau
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(shimmerBrush) // <--- INI ANIMASINYA
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Terkumpul", fontSize = 11.sp, color = TextColorSecondary)
                                        Text(collectedStr, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Target", fontSize = 11.sp, color = TextColorSecondary)
                                        Text(targetStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. DESKRIPSI
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(800, delayMillis = 200)) +
                                slideInVertically(tween(800, delayMillis = 200)) { 50 }
                    ) {
                        Column {
                            Text("Tentang Program", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = p.description, color = TextColorSecondary, fontSize = 14.sp, lineHeight = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 4. INPUT DONASI
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(800, delayMillis = 300)) +
                                slideInVertically(tween(800, delayMillis = 300)) { 50 }
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Mau Wakaf Berapa?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                                    placeholder = { Text("Rp 0") },
                                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldDeep,
                                        unfocusedBorderColor = GrayInactive,
                                        cursorColor = EmeraldDeep
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        if (amountText.isEmpty() || amountText.toLong() < 10000) {
                                            Toast.makeText(context, "Minimal wakaf Rp 10.000", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                                    elevation = ButtonDefaults.buttonElevation(8.dp)
                                ) {
                                    Text("Lanjut Pembayaran", fontWeight = FontWeight.Bold, color = White)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }

    if (showDialog && project != null) {
        ManualTransferDialogNew(
            amount = amountText.toLongOrNull() ?: 0L,
            navController = navController,
            projectId = projectId,
            authViewModel = authViewModel,
            onDismiss = { showDialog = false },
            onSuccess = {
                showDialog = false
                Toast.makeText(context, "Alhamdulillah! Menunggu verifikasi admin.", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        )
    }
}
