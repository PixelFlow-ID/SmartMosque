package com.example.smartmosque.ui.screens.donation

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.smartmosque.viewmodel.AuthViewModel
import com.example.smartmosque.data.model.WaqfProject
import com.example.smartmosque.ui.theme.*
import com.example.smartmosque.utils.ImageUtils
import com.example.smartmosque.data.model.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

// --- MODEL DATA LOKAL ---


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

            val collectedStr = formatRupiah(p.collectedAmount)
            val targetStr = formatRupiah(p.targetAmount)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTransferDialogNew(
    amount: Long,
    navController: NavController,
    projectId: String?,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val userId = currentUser?.uid ?: ""

    // COROUTINE SCOPE UNTUK BACKGROUND PROCESS
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var isLoadingMethods by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("payment_methods")
            .get()
            .addOnSuccessListener { result ->
                paymentMethods = result.toObjects(PaymentMethod::class.java)
                isLoadingMethods = false
            }
            .addOnFailureListener {
                isLoadingMethods = false
                paymentMethods = listOf(PaymentMethod("BSI", "Masjid Agung", "7289983273", "", "BANK"))
            }
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        containerColor = White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, tint = EmeraldDeep)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Selesaikan Wakaf", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 550.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Total Donasi:", fontSize = 12.sp, color = TextColorSecondary)
                Text(formatRupiah(amount), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GrayInputBackground)
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingMethods) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldDeep, modifier = Modifier.size(30.dp))
                    }
                } else {
                    val banks = paymentMethods.filter { it.type == "BANK" }
                    if (banks.isNotEmpty()) {
                        Text("Transfer Bank:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        banks.forEach { bank ->
                            BankItemCardNew(bank)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val qris = paymentMethods.find { it.type == "QRIS" }
                    if (qris != null) {
                        Text("Scan QRIS:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GrayInactive, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (qris.name.isNotEmpty()) Text(qris.name, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                AsyncImage(
                                    model = qris.logoUrl,
                                    contentDescription = "Scan QRIS",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Scan pakai e-Wallet apa saja", fontSize = 10.sp, color = TextColorSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                HorizontalDivider(color = GrayInputBackground)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Upload Bukti Transfer", fontWeight = FontWeight.Bold, color = TextColorPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GrayInputBackground)
                        .border(1.dp, GrayInactive, RoundedCornerShape(12.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.UploadFile, null, tint = EmeraldDeep, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Ketuk untuk upload", fontSize = 12.sp, color = TextColorSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (imageUri == null) {
                        Toast.makeText(context, "Upload bukti dulu ya", Toast.LENGTH_SHORT).show()
                    } else if (userId.isEmpty()) {
                        Toast.makeText(context, "Silakan login ulang", Toast.LENGTH_SHORT).show()
                    } else {
                        isUploading = true

                        // --- PROSES KOMPRESI DAN UPLOAD ---
                        scope.launch(Dispatchers.IO) {
                            // 1. Kompres Gambar
                            val compressedFile = ImageUtils.compressImage(context, imageUri!!)

                            if (compressedFile != null) {
                                // 2. Upload ke Cloudinary
                                MediaManager.get().upload(compressedFile.absolutePath)
                                    .unsigned("masjid_upload")
                                    .callback(object : UploadCallback {
                                        override fun onStart(requestId: String) {}
                                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                            val downloadUrl = resultData["secure_url"].toString()

                                            // 3. Simpan ke Firestore
                                            val db = FirebaseFirestore.getInstance()
                                            val donationData = hashMapOf(
                                                "projectId" to projectId,
                                                "amount" to amount,
                                                "category" to "Wakaf",
                                                "status" to "PENDING",
                                                "date" to Date(),
                                                "userId" to userId,
                                                "method" to "MANUAL/QRIS",
                                                "proofUrl" to downloadUrl,
                                                "type" to "WAKAF"
                                            )

                                            db.collection("donations").add(donationData)
                                                .addOnSuccessListener {
                                                    isUploading = false
                                                    try { compressedFile.delete() } catch (e: Exception) {}

                                                    scope.launch(Dispatchers.Main) {
                                                        onSuccess()
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    isUploading = false
                                                    scope.launch(Dispatchers.Main) {
                                                        Toast.makeText(context, "Gagal simpan database", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                        }

                                        override fun onError(requestId: String, error: ErrorInfo) {
                                            isUploading = false
                                            scope.launch(Dispatchers.Main) {
                                                Toast.makeText(context, "Gagal upload: ${error.description}", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        override fun onReschedule(requestId: String, error: ErrorInfo) {}
                                    })
                                    .dispatch()
                            } else {
                                isUploading = false
                                scope.launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                enabled = !isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mengirim...", color = White)
                } else {
                    Text("Kirim Bukti", color = White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isUploading) {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Batal", color = TextColorSecondary)
                }
            }
        }
    )
}

@Composable
fun BankItemCardNew(bank: PaymentMethod) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GrayInactive.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(GrayInputBackground, RoundedCornerShape(8.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bank.logoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = bank.logoUrl,
                        contentDescription = bank.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.AccountBalance, null, tint = TextColorSecondary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bank.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextColorPrimary)
                Text(bank.accountName, fontSize = 10.sp, color = TextColorSecondary, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    bank.accountNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                    color = EmeraldDeep
                )
            }
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(bank.accountNumber))
                Toast.makeText(context, "Disalin", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, "Salin", tint = EmeraldDeep, modifier = Modifier.size(20.dp))
            }
        }
    }
}

fun formatRupiah(amount: Long): String {
    return try {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    } catch (e: Exception) {
        "Rp $amount"
    }
}
