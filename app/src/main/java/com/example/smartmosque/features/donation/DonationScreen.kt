package com.example.smartmosque.features.donation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartmosque.features.auth.AuthState
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.model.PaymentMethod
import com.example.smartmosque.model.WaqfProject
import com.example.smartmosque.ui.theme.Screen
import com.example.smartmosque.utils.ImageUtils // Pastikan utility ini ada
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

// --- IMPORT WARNA TEMA ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.RedError
import com.example.smartmosque.ui.theme.TextBlack
import com.example.smartmosque.ui.theme.GrayInputBackground

// Model lokal untuk Menu Infaq (jika belum ada di folder model)
data class InfaqCategoryHome(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val iconColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: WaqfViewModel = viewModel()
) {

    val context = LocalContext.current

    // State Wakaf
    val waqfList by viewModel.waqfProjects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // State User
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user
    val isAdmin = currentUser?.email == "ramdanidoni244@gmail.com"

    // State Infaq Popups
    var showInfaqSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<InfaqCategoryHome?>(null) }
    var inputAmount by remember { mutableLongStateOf(0L) }

    Scaffold(
        containerColor = BgPremium,
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddWaqfProgram.route) },
                    containerColor = EmeraldDeep,
                    contentColor = White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Program")
                }
            }
        }
    ) { paddingValues ->

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp), // Padding bawah agar tidak tertutup FAB
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. HEADER
            item {
                CleanDonationHeader()
            }

            // 2. BAGIAN LAYANAN INFAQ
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Layanan Infaq",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColorPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid Menu Infaq
                    PremiumDonationGrid(
                        onItemClick = { category ->
                            selectedCategory = category
                            showInfaqSheet = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                // Garis pemisah halus
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Color.LightGray.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 3. BAGIAN PROGRAM WAKAF
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Program Wakaf",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColorPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Investasi untuk rumah di surga.",
                        fontSize = 13.sp,
                        color = TextColorSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 4. LIST ITEM WAKAF
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldDeep)
                    }
                }
            } else if (waqfList.isEmpty()) {
                item {
                    EmptyStateDonation()
                }
            } else {
                items(waqfList) { project ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                        PremiumWaqfCard(
                            project = project,
                            isAdmin = isAdmin,
                            onClick = { navController.navigate(Screen.createRoute(project.id)) },
                            onDelete = { viewModel.deleteProject(project.id, {}, {}) }
                        )
                    }
                }
            }
        } // End LazyColumn

        // --- POPUP LOGIC ---

        // 1. Bottom Sheet Input Nominal
        if (showInfaqSheet && selectedCategory != null) {
            HomeInfaqBottomSheet(
                category = selectedCategory!!,
                onDismiss = { showInfaqSheet = false },
                onNext = { amount ->
                    inputAmount = amount
                    showInfaqSheet = false
                    showPaymentDialog = true
                }
            )
        }

        // 2. Dialog Pembayaran & Upload
        if (showPaymentDialog && selectedCategory != null) {
            InfaqPaymentDialog(
                amount = inputAmount,
                categoryName = selectedCategory!!.title,
                authViewModel = authViewModel,
                onDismiss = { showPaymentDialog = false },
                onSuccess = {
                    showPaymentDialog = false
                    Toast.makeText(context, "Alhamdulillah, bukti terkirim!", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

// ==========================================
// COMPONENT: GRID MENU INFAQ
// ==========================================

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

// ==========================================
// COMPONENT: BOTTOM SHEET & DIALOGS
// ==========================================

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
                    scope.launch(Dispatchers.IO) {
                        // Proses Kompresi & Upload
                        val compressedFile = ImageUtils.compressImage(context, imageUri!!)

                        if (compressedFile != null) {
                            val compressedUri = Uri.fromFile(compressedFile)
                            val ref = FirebaseStorage.getInstance().reference.child("proofs/infaq/${UUID.randomUUID()}.jpg")

                            ref.putFile(compressedUri)
                                .addOnSuccessListener { taskSnapshot ->
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
                                        Firebase.firestore.collection("donations").add(data)
                                            .addOnSuccessListener {
                                                try { compressedFile.delete() } catch (e: Exception) {}
                                                scope.launch(Dispatchers.Main) {
                                                    isUploading = false
                                                    onSuccess()
                                                }
                                            }
                                            .addOnFailureListener { scope.launch(Dispatchers.Main) { isUploading = false } }
                                    }
                                }
                                .addOnFailureListener {
                                    scope.launch(Dispatchers.Main) { isUploading = false; Toast.makeText(context, "Gagal upload", Toast.LENGTH_SHORT).show() }
                                }
                        } else {
                            scope.launch(Dispatchers.Main) { isUploading = false; Toast.makeText(context, "Gagal kompresi", Toast.LENGTH_SHORT).show() }
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

// ==========================================
// COMPONENT: EXISTING WAKAF & HEADER UI
// ==========================================

@Composable
fun CleanDonationHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Watermark Icon di Kanan (Sangat Samar & Elegan)
        Icon(
            imageVector = Icons.Outlined.VolunteerActivism,
            contentDescription = null,
            tint = EmeraldDeep.copy(alpha = 0.05f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(110.dp)
                .offset(x = 20.dp, y = 10.dp)
                .rotate(-10f)
        )

        Column {
            Text(
                text = "INVESTASI AKHIRAT",
                fontSize = 11.sp,
                color = EmeraldDeep,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Infaq & Wakaf",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextColorPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Salurkan harta terbaikmu untuk\nkebaikan yang mengalir abadi.",
                fontSize = 14.sp,
                color = TextColorSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun PremiumWaqfCard(
    project: WaqfProject,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
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
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                if (project.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = project.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF0FDF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, null, tint = EmeraldDeep.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    }
                }

                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomEnd),
                    shape = RoundedCornerShape(12.dp),
                    color = White,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$progressPercent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDeep
                        )
                    }
                }

                if (isAdmin) {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                            .clickable { onDelete() },
                        shape = CircleShape,
                        color = White.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = RedError,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SEDANG BERJALAN",
                    fontSize = 10.sp,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = project.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColorPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                ShimmerProgressBar(
                    currentAmount = project.collectedAmount.toDouble(),
                    targetAmount = project.targetAmount.toDouble()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Terkumpul", fontSize = 11.sp, color = TextColorSecondary)
                        Text(
                            text = collectedStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColorPrimary
                        )
                    }

                    Box(modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f)))

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target", fontSize = 11.sp, color = TextColorSecondary)
                        Text(
                            text = targetStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColorSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerProgressBar(
    currentAmount: Double,
    targetAmount: Double,
    modifier: Modifier = Modifier
) {
    val progressRaw = if (targetAmount > 0) currentAmount / targetAmount else 0.0
    val progressClamped = progressRaw.toFloat().coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progressClamped,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "fill"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_move"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(EmeraldDeep, Color(0xFF4ADE80), EmeraldDeep),
        start = Offset(shimmerTranslate - 300f, 0f),
        end = Offset(shimmerTranslate, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(brush = shimmerBrush)
        )
    }
}

@Composable
fun EmptyStateDonation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.VolunteerActivism,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Belum ada program wakaf.",
            fontSize = 16.sp,
            color = TextColorSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}