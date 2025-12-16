package com.example.smartmosque.features.donation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.model.WaqfProject
import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.storage.FirebaseStorage <-- DIHAPUS

// --- IMPORT CLOUDINARY ---
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- IMPORT WARNA TEMA (Sesuaikan dengan file theme Anda) ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.BackgroundLight
import com.example.smartmosque.ui.theme.GrayInputBackground
import com.example.smartmosque.ui.theme.GrayInactive
import com.example.smartmosque.ui.theme.BgPremium

// --- MODEL DATA LOKAL ---
data class PaymentMethod(
    val name: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val logoUrl: String = "",
    val type: String = "" // "BANK" atau "QRIS"
)

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

    // Fetch Data Project
    LaunchedEffect(projectId) {
        if (projectId != null) {
            FirebaseFirestore.getInstance().collection("waqf_programs").document(projectId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.toObject(WaqfProject::class.java)
                        project = data?.copy(id = snapshot.id)
                    }
                }
        }
    }

    Scaffold(
        containerColor = BgPremium,
        // contentWindowInsets(0.dp) membuat layout "Immersive" (gambar sampai belakang status bar)
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = { /* Kosongkan Judul agar fokus ke gambar */ },
                navigationIcon = {
                    // Tombol Back Custom (Bulat & Semi Transparan)
                    Surface(
                        onClick = { navController.popBackStack() },
                        shape = CircleShape,
                        color = White.copy(alpha = 0.5f), // Efek Glass
                        modifier = Modifier
                            .padding(start = 16.dp, top = 8.dp) // Sesuaikan padding agar pas
                            .size(40.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.Black // Icon hitam agar kontras
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent // Bar transparan
                )
            )
        }
    ) { paddingValues ->
        // LOGIKA KONTEN UTAMA
        if (project == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldDeep)
            }
        } else {
            val p = project!!

            // Hitung Progress
            val progress = if (p.targetAmount > 0) p.collectedAmount.toFloat() / p.targetAmount.toFloat() else 0f
            val collectedStr = formatRupiah(p.collectedAmount)
            val targetStr = formatRupiah(p.targetAmount)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // Kita hanya pakai padding bawah, padding atas diabaikan agar gambar full screen
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // 1. HERO IMAGE (FULL SCREEN WIDTH & HEIGHT)
                Box(modifier = Modifier.height(350.dp).fillMaxWidth()) {
                    AsyncImage(
                        model = p.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient Gelap di Bawah Gambar agar teks terbaca/transisi halus
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

                // 2. KONTEN DETAIL (Overlap ke atas gambar sedikit)
                Column(
                    modifier = Modifier
                        .offset(y = (-50).dp) // Tarik ke atas 50dp
                        .padding(horizontal = 20.dp)
                ) {
                    // --- KARTU UTAMA ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = White)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Badge Kategori
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

                            // Judul Project
                            Text(
                                text = p.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextColorPrimary,
                                lineHeight = 28.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(GrayInputBackground)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.horizontalGradient(listOf(EmeraldDeep, GreenPrimary)))
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Info Nominal
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- DESKRIPSI ---
                    Text("Tentang Program", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = p.description,
                        color = TextColorSecondary,
                        fontSize = 14.sp,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- INPUT CARD ---
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
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                                elevation = ButtonDefaults.buttonElevation(8.dp)
                            ) {
                                Text("Lanjut Pembayaran", fontWeight = FontWeight.Bold, color = White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }

    // --- DIALOG PEMBAYARAN ---
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

// ==========================================
// KOMPONEN PENDUKUNG (DIALOG, CARD, DLL)
// ==========================================

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

    // State Upload
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // State Payment Methods
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var isLoadingMethods by remember { mutableStateOf(true) }

    // Fetch Payment Methods
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("payment_methods")
            .get()
            .addOnSuccessListener { result ->
                paymentMethods = result.toObjects(PaymentMethod::class.java)
                isLoadingMethods = false
            }
            .addOnFailureListener {
                isLoadingMethods = false
                // Fallback Dummy jika gagal fetch
                paymentMethods = listOf(
                    PaymentMethod("BSI", "Masjid Agung", "7289983273", "", "BANK")
                )
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
                // Info Total
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
                    // 1. LIST BANK
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

                    // 2. QRIS
                    val qris = paymentMethods.find { it.type == "QRIS" }
                    if (qris != null) {
                        Text("Scan QRIS:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, GrayInactive, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if(qris.name.isNotEmpty()) Text(qris.name, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                AsyncImage(
                                    model = qris.logoUrl,
                                    contentDescription = "Scan QRIS",
                                    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)),
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

                // 3. UPLOAD BUKTI
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
                        AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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

                        // --- UPLOAD KE CLOUDINARY ---
                        MediaManager.get().upload(imageUri)
                            .unsigned("masjid_upload")
                            .callback(object : UploadCallback {
                                override fun onStart(requestId: String) {}
                                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                    // 1. Ambil URL Gambar dari Cloudinary
                                    val downloadUrl = resultData["secure_url"].toString()

                                    // 2. Simpan Data ke Firestore (Sama seperti sebelumnya)
                                    val db = FirebaseFirestore.getInstance()
                                    val donationData = hashMapOf(
                                        "projectId" to projectId,
                                        "amount" to amount,
                                        "category" to "Wakaf",
                                        "status" to "PENDING",
                                        "date" to Date(),
                                        "userId" to userId,
                                        "method" to "MANUAL/QRIS",
                                        "proofUrl" to downloadUrl, // URL dari Cloudinary
                                        "type" to "WAKAF"
                                    )

                                    db.collection("donations").add(donationData)
                                        .addOnSuccessListener {
                                            isUploading = false
                                            onSuccess()
                                        }
                                        .addOnFailureListener {
                                            isUploading = false
                                            Toast.makeText(context, "Gagal simpan data ke database", Toast.LENGTH_SHORT).show()
                                        }
                                }

                                override fun onError(requestId: String, error: ErrorInfo) {
                                    isUploading = false
                                    Toast.makeText(context, "Gagal upload: ${error.description}", Toast.LENGTH_SHORT).show()
                                }

                                override fun onReschedule(requestId: String, error: ErrorInfo) {}
                            })
                            .dispatch()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
        modifier = Modifier.fillMaxWidth().border(1.dp, GrayInactive.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
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
                if(bank.logoUrl.isNotEmpty()) {
                    AsyncImage(model = bank.logoUrl, contentDescription = bank.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.AccountBalance, null, tint = TextColorSecondary)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(bank.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextColorPrimary)
                Text(bank.accountName, fontSize = 10.sp, color = TextColorSecondary, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(bank.accountNumber, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp, color = EmeraldDeep)
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

// Helper Format Rupiah
fun formatRupiah(amount: Long): String {
    return try {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    } catch (e: Exception) { "Rp $amount" }
}