package com.example.smartmosque.ui.screens.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartmosque.viewmodel.HomeViewModel

// --- IMPORTS KHUSUS UNTUK KOMPRESI ---
import com.example.smartmosque.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
// -------------------------------------

import com.example.smartmosque.ui.components.MiniWaqfProjectNew
import com.example.smartmosque.viewmodel.AuthViewModel
import com.example.smartmosque.data.model.PaymentMethod
import com.example.smartmosque.data.model.Schedule // Import Model Schedule
import com.example.smartmosque.ui.theme.*
import com.example.smartmosque.utils.UpcommingEventBox
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

// DATA MODEL
data class InfaqCategoryHome(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val iconColor: Color
)

// Warna Khusus untuk Grafik Emas (Local)
private val GoldAccent = Color(0xFFFFD700)

@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val userName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Jamaah"
    val userInitial = userName.take(1).uppercase()
    val context = LocalContext.current

    // Notifikasi State
    val hasUnreadNotifications by homeViewModel.hasUnreadNotifications.collectAsState()

    // Ongoing Event State (BARU)
    val ongoingEvent by homeViewModel.ongoingEvent.collectAsState()

    // --- STATE UNTUK FLOW INFAQ ---
    var showInfaqSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    var selectedInfaqCategory by remember { mutableStateOf<InfaqCategoryHome?>(null) }
    var amountToPay by remember { mutableStateOf(0L) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPremium)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 10.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profil User
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Screen.ProfileDetail.route) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(4.dp, CircleShape, spotColor = EmeraldDeep.copy(alpha = 0.2f))
                            .clip(CircleShape)
                            .background(EmeraldDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userInitial, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text("Assalamualaikum,", fontSize = 12.sp, color = TextGrey, fontWeight = FontWeight.Medium)
                        Text("$userName 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }
                }

                // Tombol Notifikasi
                Surface(
                    onClick = {
                        homeViewModel.markNotificationsAsRead()
                        navController.navigate(Screen.Notification.route)
                    },
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = CardSurface,
                    shadowElevation = 3.dp,
                    tonalElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Notifications, "Notifikasi", tint = TextBlack, modifier = Modifier.size(24.dp))
                        if (hasUnreadNotifications) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        }
                    }
                }
            }

            // --- KONTEN SCROLL ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 1. DASHBOARD GRAFIK ANIMASI
                AnimatedEmeraldCard(homeViewModel)

                Spacer(modifier = Modifier.height(24.dp))

                // --- 1.5 FITUR BARU: ONGOING EVENT (SEDANG BERLANGSUNG) ---
                if (ongoingEvent != null) {
                    OngoingLiveCard(
                        schedule = ongoingEvent!!,
                        onClick = {
                            // Navigasi ke Detail Jadwal
                            navController.navigate("schedule_detail/${ongoingEvent!!.id}")
                        }
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                }
                // -----------------------------------------------------------

                // 2. MENU INFAQ
                Text("Layanan Infaq", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                Spacer(modifier = Modifier.height(16.dp))

                PremiumDonationGrid(
                    onItemClick = { category ->
                        selectedInfaqCategory = category
                        showInfaqSheet = true
                    }
                )

                Spacer(modifier = Modifier.height(30.dp))

                // 3. WAKAF & EVENT
                MiniWaqfProjectNew(navController)

                Spacer(modifier = Modifier.height(24.dp))
                UpcommingEventBox(navController, authViewModel)
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // --- SHEET & DIALOG LOGIC ---
        if (showInfaqSheet && selectedInfaqCategory != null) {
            HomeInfaqBottomSheet(
                category = selectedInfaqCategory!!,
                onDismiss = { showInfaqSheet = false },
                onNext = { amount ->
                    amountToPay = amount
                    showInfaqSheet = false
                    showPaymentDialog = true
                }
            )
        }

        if (showPaymentDialog && selectedInfaqCategory != null) {
            InfaqPaymentDialog(
                amount = amountToPay,
                categoryName = selectedInfaqCategory!!.title,
                authViewModel = authViewModel,
                onDismiss = { showPaymentDialog = false },
                onSuccess = {
                    showPaymentDialog = false
                    Toast.makeText(context, "Jazakallah! Infaq Anda sedang diverifikasi.", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

// --- ONGOING LIVE CARD ---
// --- GANTI KODE LAMA 'OngoingLiveCard' DENGAN INI ---

@Composable
fun OngoingLiveCard(schedule: Schedule, onClick: () -> Unit) {
    // Animasi Pulse untuk titik merah
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFFFF5252).copy(alpha = 0.25f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEBEE)) // Border merah sangat muda
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // --- HEADER: BADGE LIVE & WAKTU ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge LIVE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(50)) // Background merah muda
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = alpha)) // Titik berkedip
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LIVE SEKARANG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        letterSpacing = 0.5.sp
                    )
                }

                // Jam
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${schedule.time} WIB",
                        fontSize = 12.sp,
                        color = TextColorSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CONTENT: JUDUL & PEMBICARA ---
            Row(verticalAlignment = Alignment.Top) {
                // Icon Kategori Besar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFEBEE), // Background icon merah muda
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Mic, // Icon Mic
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColorPrimary,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ust. ${schedule.speaker}",
                        fontSize = 14.sp,
                        color = TextColorSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(12.dp))

            // --- FOOTER: LOKASI & CTA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LocationOn, null, tint = TextColorSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = schedule.location, // Pastikan di model Schedule ada field 'location'
                        fontSize = 12.sp,
                        color = TextColorSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Tombol "Gabung" Kecil
                Surface(
                    color = EmeraldDeep, // Warna Hijau Tema
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gabung", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}

// --- KOMPONEN GRAFIK ANIMASI ---
@Composable
fun AnimatedEmeraldCard(homeViewModel: HomeViewModel) {
    val eventsThisMonth by homeViewModel.eventsThisMonth.collectAsState()
    val totalParticipants by homeViewModel.totalParticipants.collectAsState()

    // Trigger animasi saat masuk layar
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    // Animasi Donut (Lingkaran)
    val donutProgress by animateFloatAsState(
        targetValue = if (startAnimation) 0.75f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "donut"
    )

    // Animasi Bar (Grafik Batang) - Bergelombang
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
            // Hiasan Latar Belakang
            Box(modifier = Modifier.offset(x = 200.dp, y = (-50).dp).size(250.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))

            Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                // KIRI: Grafik Donut
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                            // Track
                            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = Color.White.copy(alpha = 0.1f), strokeWidth = 4.dp)
                            // Progress Animasi
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

                // GARIS PEMISAH
                Box(modifier = Modifier.width(1.dp).height(60.dp).background(Color.White.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.width(24.dp))

                // KANAN: Grafik Batang
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Animasi Bar
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

// Helper untuk Batang Emas
@Composable
fun GoldBar(height: Dp) {
    Box(modifier = Modifier.width(5.dp).height(height).clip(RoundedCornerShape(4.dp)).background(GoldAccent))
}

// --- SHEET & DIALOG KOMPONEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeInfaqBottomSheet(category: InfaqCategoryHome, onDismiss: () -> Unit, onNext: (Long) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    val quickAmounts = listOf(10000L, 20000L, 50000L, 100000L)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(category.color, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(category.icon, null, tint = category.iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Infaq: ${category.title}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Text("Masukkan nominal terbaik", fontSize = 12.sp, color = TextColorSecondary)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = amountText, onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                label = { Text("Nominal (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldDeep, focusedLabelColor = EmeraldDeep, cursorColor = EmeraldDeep, focusedTextColor = TextBlack, unfocusedTextColor = TextBlack)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickAmounts.forEach { amt ->
                    SuggestionChip(onClick = { amountText = amt.toString() }, label = { Text("${amt / 1000}rb") },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if (amountText == amt.toString()) EmeraldDeep.copy(alpha = 0.1f) else Color.Transparent),
                        border = BorderStroke(1.dp, if (amountText == amt.toString()) EmeraldDeep else Color.LightGray)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { if (amountText.isNotEmpty()) onNext(amountText.toLong()) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep), enabled = amountText.isNotEmpty()) {
                Text("Lanjut Pembayaran", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfaqPaymentDialog(amount: Long, categoryName: String, authViewModel: AuthViewModel, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val userId = currentUser?.uid ?: ""

    // Scope untuk proses background (Kompresi & Upload)
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var isLoadingMethods by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("payment_methods").get()
            .addOnSuccessListener { paymentMethods = it.toObjects(PaymentMethod::class.java); isLoadingMethods = false }
            .addOnFailureListener { isLoadingMethods = false }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }
    val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() }, containerColor = White,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.VerifiedUser, null, tint = EmeraldDeep); Spacer(modifier = Modifier.width(8.dp)); Text("Konfirmasi Infaq", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary) } },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                Text(formatRupiah, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                Spacer(modifier = Modifier.height(16.dp)); HorizontalDivider(color = GrayInputBackground); Spacer(modifier = Modifier.height(16.dp))
                if (isLoadingMethods) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = EmeraldDeep) }
                else {
                    Text("Transfer ke:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    paymentMethods.filter { it.type == "BANK" }.forEach { InfaqBankCard(it); Spacer(modifier = Modifier.height(8.dp)) }
                    paymentMethods.find { it.type == "QRIS" }?.let {
                        Spacer(modifier = Modifier.height(16.dp)); Text("Scan QRIS:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary); Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(model = it.logoUrl, contentDescription = "QRIS", modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(GrayInputBackground), contentScale = ContentScale.Fit)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)); HorizontalDivider(color = GrayInputBackground); Spacer(modifier = Modifier.height(16.dp))
                Text("Upload Bukti", fontWeight = FontWeight.Bold, color = TextColorPrimary); Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(GrayInputBackground).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                    if (imageUri != null) AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.UploadFile, null, tint = EmeraldDeep); Text("Ketuk untuk upload", fontSize = 12.sp, color = TextColorSecondary) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (imageUri == null) {
                    Toast.makeText(context, "Upload bukti dulu", Toast.LENGTH_SHORT).show()
                } else {
                    isUploading = true

                    // --- LOGIKA KOMPRESI DAN UPLOAD ---
                    scope.launch(Dispatchers.IO) {
                        // 1. Kompres Gambar
                        val compressedFile = ImageUtils.compressImage(context, imageUri!!)

                        if (compressedFile != null) {
                            val compressedUri = Uri.fromFile(compressedFile)
                            val ref = FirebaseStorage.getInstance().reference.child("proofs/infaq/${UUID.randomUUID()}.jpg")

                            // 2. Upload File yang sudah dikompres
                            ref.putFile(compressedUri)
                                .addOnSuccessListener { taskSnapshot ->
                                    // 3. Ambil URL Download
                                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                                        val data = hashMapOf(
                                            "type" to "INFAQ",
                                            "category" to categoryName,
                                            "amount" to amount,
                                            "status" to "PENDING",
                                            "date" to Timestamp.now(),
                                            "userId" to userId,
                                            "proofUrl" to uri.toString(),
                                            "method" to "MANUAL"
                                        )

                                        // 4. Simpan Data ke Firestore
                                        Firebase.firestore.collection("donations").add(data)
                                            .addOnSuccessListener {
                                                // 5. Bersihkan cache dan Update UI
                                                try { compressedFile.delete() } catch (e: Exception) {}
                                                scope.launch(Dispatchers.Main) {
                                                    isUploading = false
                                                    onSuccess()
                                                }
                                            }
                                            .addOnFailureListener {
                                                scope.launch(Dispatchers.Main) { isUploading = false }
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    scope.launch(Dispatchers.Main) {
                                        isUploading = false
                                        Toast.makeText(context, "Gagal upload gambar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            scope.launch(Dispatchers.Main) {
                                isUploading = false
                                Toast.makeText(context, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep), enabled = !isUploading) { if(isUploading) CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp)) else Text("Kirim Bukti", color = White) }
        },
        dismissButton = { if(!isUploading) TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Batal", color = TextColorSecondary) } }
    )
}

@Composable
fun InfaqBankCard(bank: PaymentMethod) {
    val clipboard = LocalClipboardManager.current; val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.LightGray)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = bank.logoUrl, contentDescription = null, modifier = Modifier.size(40.dp), contentScale = ContentScale.Fit)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(bank.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextColorPrimary); Text(bank.accountNumber, fontSize = 14.sp, color = EmeraldDeep, fontWeight = FontWeight.Bold) }
            IconButton(onClick = { clipboard.setText(AnnotatedString(bank.accountNumber)); Toast.makeText(context, "Disalin", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, null, tint = EmeraldDeep, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
fun PremiumDonationGrid(onItemClick: (InfaqCategoryHome) -> Unit) {
    val menuList = listOf(
        InfaqCategoryHome("ops", "Operasional", Icons.Default.Mosque, Color(0xFFE0F2F1), Color(0xFF00695C)),
        InfaqCategoryHome("snack", "Jumat Berkah", Icons.Default.Restaurant, Color(0xFFFFF8E1), Color(0xFFFF8F00)),
        InfaqCategoryHome("alat", "Sarana", Icons.Default.Chair, Color(0xFFE3F2FD), Color(0xFF1565C0)),
        InfaqCategoryHome("kitab", "Wakaf Kitab", Icons.Default.Book, Color(0xFFF3E5F5), Color(0xFF7B1FA2))
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        menuList.forEach { item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp).clickable { onItemClick(item) }) {
                Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(22.dp), color = item.color, shadowElevation = 0.dp) { Box(contentAlignment = Alignment.Center) { Icon(item.icon, item.title, tint = item.iconColor, modifier = Modifier.size(28.dp)) } }
                Spacer(modifier = Modifier.height(10.dp)); Text(item.title, fontSize = 12.sp, color = TextBlack, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}
