package com.example.smartmosque.features.donation.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.model.PaymentMethod // Sesuaikan package model kamu
import com.example.smartmosque.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import com.example.smartmosque.utils.ImageUtils

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
                paymentMethods = listOf(PaymentMethod("BSI", "Masjid Agung Manonjaya", "7289983273", "", "BANK"))
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
                Icon(Icons.Default.Verified, null, tint = EmeraldDeep)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Selesaikan Wakaf", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Total Donasi:", fontSize = 12.sp, color = TextColorSecondary)
                Text(formatRupiah(amount), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)

                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                if (isLoadingMethods) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldDeep, modifier = Modifier.size(30.dp))
                    }
                } else {
                    val banks = paymentMethods.filter { it.type == "BANK" }
                    if (banks.isNotEmpty()) {
                        Text("Transfer Bank:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                        banks.forEach { bank ->
                            BankItemCardNew(bank)
                        }
                    }

                    val qris = paymentMethods.find { it.type == "QRIS" }
                    if (qris != null) {
                        Text("Scan QRIS:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (qris.name.isNotEmpty()) Text(qris.name, fontWeight = FontWeight.Bold, color = TextColorPrimary)
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
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                Text("Upload Bukti Transfer", fontWeight = FontWeight.Bold, color = TextColorPrimary, fontSize = 14.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (imageUri == null) {
                            Toast.makeText(context, "Upload bukti dulu ya", Toast.LENGTH_SHORT).show()
                        } else if (userId.isEmpty()) {
                            Toast.makeText(context, "Silakan login ulang", Toast.LENGTH_SHORT).show()
                        } else {
                            isUploading = true
                            scope.launch(Dispatchers.IO) {
                                // Mengasumsikan ImageUtils sudah dibuat terpisah
                                val compressedFile = ImageUtils.compressImage(context, imageUri!!)
                                if (compressedFile != null) {
                                    MediaManager.get().upload(compressedFile.absolutePath)
                                        .unsigned("masjid_upload")
                                        .callback(object : UploadCallback {
                                            override fun onStart(requestId: String) {}
                                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                                val downloadUrl = resultData["secure_url"].toString()
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
                                                        scope.launch(Dispatchers.Main) { onSuccess() }
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
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mengirim...", color = White)
                    } else {
                        Text("Kirim Bukti", color = White, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isUploading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Batal", color = TextColorSecondary, fontWeight = FontWeight.Medium)
                    }
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

// 🎯 FUNGSI HELPER SEKARANG TERISOLASI DI SINI DAN TIDAK MENGOTORI SCREEN UTAMA
private fun formatRupiah(amount: Long): String {
    return try {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    } catch (e: Exception) {
        "Rp $amount"
    }
}