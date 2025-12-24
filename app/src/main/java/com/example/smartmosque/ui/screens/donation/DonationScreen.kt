package com.example.smartmosque.ui.screens.donation

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
import com.example.smartmosque.data.model.PaymentMethod
import com.example.smartmosque.data.model.WaqfProject
import com.example.smartmosque.ui.theme.*
import com.example.smartmosque.utils.ImageUtils
import com.example.smartmosque.viewmodel.AuthState
import com.example.smartmosque.viewmodel.AuthViewModel
import com.example.smartmosque.viewmodel.WaqfViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: WaqfViewModel = viewModel()
) {
    val context = LocalContext.current
    val waqfList by viewModel.waqfProjects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user

    // State untuk Infaq Cepat
    var showInfaqSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedInfaqCategory by remember { mutableStateOf<InfaqCategoryHome?>(null) }
    var amountToPay by remember { mutableStateOf(0L) }

    // Logika Admin (Konsisten dengan screen lain)
    val userRole by authViewModel.userRole.collectAsState()
    val isAdmin = userRole == "admin" || currentUser?.email == "ramdanidoni244@gmail.com"

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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // HEADER BARU DENGAN GRID INFAK
                PremiumDonationHeaderWithGrid(
                    isAdmin = isAdmin,
                    onItemClick = { category ->
                        selectedInfaqCategory = category
                        showInfaqSheet = true
                    }
                )

                // LIST WAKAF
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                         Text("Program Wakaf", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                         Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = EmeraldDeep)
                            }
                        }
                    } else if (waqfList.isEmpty()) {
                        item {
                            EmptyStateDonation()
                        }
                    } else {
                        items(waqfList) { project ->
                            PremiumWaqfCard(
                                project = project,
                                isAdmin = isAdmin,
                                onClick = { navController.navigate(Screen.createRoute(project.id)) },
                                onDelete = { viewModel.deleteProject(project.id, {}, {}) }
                            )
                        }
                        // Spacer
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            // --- SHEET & DIALOG INFAK ---
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
}

data class InfaqCategoryHome(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val iconColor: Color
)

@Composable
fun PremiumDonationHeaderWithGrid(isAdmin: Boolean, onItemClick: (InfaqCategoryHome) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 24.dp).background(White, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).shadow(12.dp, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp), spotColor = Color.Black.copy(0.05f))) {
        // Bagian Atas Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Icon(Icons.Outlined.VolunteerActivism, null, tint = EmeraldDeep.copy(alpha = 0.05f), modifier = Modifier.align(Alignment.CenterEnd).size(120.dp).offset(x = 30.dp, y = (-10).dp).rotate(-10f))
            Column {
                Text("LAYANAN INFAK", fontSize = 11.sp, color = EmeraldDeep, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Sedekah Sekarang", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary, letterSpacing = (-0.5).sp)
                Text("Pilih kategori kebaikanmu hari ini.", fontSize = 14.sp, color = TextColorSecondary)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Grid Menu
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val menuList = listOf(
                InfaqCategoryHome("ops", "Operasional", Icons.Default.Mosque, Color(0xFFE0F2F1), Color(0xFF00695C)),
                InfaqCategoryHome("snack", "Jumat Berkah", Icons.Default.Restaurant, Color(0xFFFFF8E1), Color(0xFFFF8F00)),
                InfaqCategoryHome("alat", "Sarana", Icons.Default.Chair, Color(0xFFE3F2FD), Color(0xFF1565C0)),
                InfaqCategoryHome("kitab", "Wakaf Kitab", Icons.Default.Book, Color(0xFFF3E5F5), Color(0xFF7B1FA2))
            )

            menuList.forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).clickable { onItemClick(item) }) {
                    Surface(modifier = Modifier.size(60.dp), shape = RoundedCornerShape(20.dp), color = item.color) { 
                        Box(contentAlignment = Alignment.Center) { Icon(item.icon, null, tint = item.iconColor, modifier = Modifier.size(26.dp)) } 
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(item.title, fontSize = 11.sp, color = TextColorPrimary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 14.sp)
                }
            }
        }
    }
}

// --- SHEET & DIALOG LOGIC ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeInfaqBottomSheet(category: InfaqCategoryHome, onDismiss: () -> Unit, onNext: (Long) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    val quickAmounts = listOf(10000L, 20000L, 50000L, 100000L)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = White) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(category.color, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(category.icon, null, tint = category.iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Infaq: ${category.title}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Text("Nominal terbaikmu", fontSize = 12.sp, color = TextColorSecondary)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = amountText, onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                label = { Text("Nominal (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldDeep, focusedLabelColor = EmeraldDeep)
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
                Text("Lanjut Pembayaran", color = White, fontWeight = FontWeight.Bold)
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
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Verified, null, tint = EmeraldDeep); Spacer(modifier = Modifier.width(8.dp)); Text("Konfirmasi Infaq", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary) } },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                Text(formatRupiah, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                Spacer(modifier = Modifier.height(16.dp)); HorizontalDivider(color = Color(0xFFF5F5F5)); Spacer(modifier = Modifier.height(16.dp))
                if (isLoadingMethods) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = EmeraldDeep) }
                else {
                    Text("Transfer ke:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    paymentMethods.filter { it.type == "BANK" }.forEach { InfaqBankCard(it); Spacer(modifier = Modifier.height(8.dp)) }
                    paymentMethods.find { it.type == "QRIS" }?.let {
                        Spacer(modifier = Modifier.height(16.dp)); Text("Scan QRIS:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary); Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(model = it.logoUrl, contentDescription = "QRIS", modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Fit)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)); HorizontalDivider(color = Color(0xFFF5F5F5)); Spacer(modifier = Modifier.height(16.dp))
                Text("Upload Bukti", fontWeight = FontWeight.Bold, color = TextColorPrimary); Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                    if (imageUri != null) AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Add, null, tint = EmeraldDeep); Text("Ketuk untuk upload", fontSize = 12.sp, color = TextColorSecondary) }
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
                        val compressedFile = ImageUtils.compressImage(context, imageUri!!)
                        if (compressedFile != null) {
                             val compressedUri = Uri.fromFile(compressedFile)
                             val ref = FirebaseStorage.getInstance().reference.child("proofs/infaq/${UUID.randomUUID()}.jpg")
                             ref.putFile(compressedUri).addOnSuccessListener { taskSnapshot ->
                                 taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                                     val data = hashMapOf(
                                         "type" to "INFAQ", "category" to categoryName, "amount" to amount, "status" to "PENDING",
                                         "date" to Timestamp.now(), "userId" to userId, "proofUrl" to uri.toString(), "method" to "MANUAL"
                                     )
                                     FirebaseFirestore.getInstance().collection("donations").add(data)
                                         .addOnSuccessListener {
                                             try { compressedFile.delete() } catch (e: Exception) {}
                                             scope.launch(Dispatchers.Main) { isUploading = false; onSuccess() }
                                         }
                                 }
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
            IconButton(onClick = { clipboard.setText(AnnotatedString(bank.accountNumber)); Toast.makeText(context, "Disalin", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.Menu, null, tint = EmeraldDeep, modifier = Modifier.size(20.dp)) }
        }
    }
}

// --- KOMPONEN CARD UTAMA WAKAF (EXISTING) ---
@Composable
fun PremiumWaqfCard(
    project: WaqfProject,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Hitung Progress Persen
    val progressRaw = if (project.targetAmount > 0) project.collectedAmount.toDouble() / project.targetAmount.toDouble() else 0.0
    val progressPercent = (progressRaw * 100).toInt().coerceIn(0, 100)

    // Format Rupiah
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
            // 1. IMAGE HERO
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

                // Badge Percent (Floating di atas gambar)
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

                // Tombol Delete Admin
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

            // 2. KONTEN INFO
            Column(modifier = Modifier.padding(20.dp)) {
                // Label Status
                Text(
                    text = "SEDANG BERJALAN",
                    fontSize = 10.sp,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Judul
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

                // --- PROGRESS BAR DENGAN ANIMASI SHIMMER ---
                ShimmerProgressBar(
                    currentAmount = project.collectedAmount.toDouble(),
                    targetAmount = project.targetAmount.toDouble()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Angka Donasi
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

                    // Garis Pemisah Kecil
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

// --- KOMPONEN ANIMASI PROGRESS BAR (SHIMMER) --- (EXISTING)
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

    val shimmerColors = listOf(
        EmeraldDeep,
        Color(0xFF4ADE80),
        EmeraldDeep
    )

    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
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

// --- TAMPILAN KOSONG --- (EXISTING)
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
