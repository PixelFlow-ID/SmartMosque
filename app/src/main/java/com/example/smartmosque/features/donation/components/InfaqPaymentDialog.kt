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
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.features.donation.presentation.WaqfViewModel
import com.example.smartmosque.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.smartmosque.model.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfaqPaymentDialog(
    amount: Long,
    categoryName: String,
    authViewModel: AuthViewModel,
    viewModel: WaqfViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val userId = currentUser?.uid ?: ""

    // Mengambil State Terpusat dari Arsitektur MVVM ViewModel
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val isLoadingMethods by viewModel.isLoadingMethods.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }
    val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        containerColor = White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, tint = EmeraldDeep)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Konfirmasi Infaq", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                Text(formatRupiah, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GrayInputBackground)
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingMethods) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldDeep)
                    }
                } else {
                    Text("Transfer ke:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Render Bank List
                    paymentMethods.filter { it.type == "BANK" }.forEach { bank ->
                        InfaqBankCard(bank)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Render QRIS jika tersedia
                    paymentMethods.find { it.type == "QRIS" }?.let { qris ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scan QRIS:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = qris.logoUrl,
                            contentDescription = "QRIS",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GrayInputBackground),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GrayInputBackground)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Upload Bukti", fontWeight = FontWeight.Bold, color = TextColorPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GrayInputBackground)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.UploadFile, null, tint = EmeraldDeep)
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
                        Toast.makeText(context, "Upload bukti dulu", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.submitInfaq(
                            context = context,
                            imageUri = imageUri!!,
                            categoryName = categoryName,
                            amount = amount,
                            userId = userId,
                            onSuccess = onSuccess,
                            onError = { errorMsg -> Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show() }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                enabled = !isUploading
            ) {
                if (isUploading) CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp))
                else Text("Kirim Bukti", color = White)
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
fun InfaqBankCard(bank: PaymentMethod) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(model = bank.logoUrl, contentDescription = null, modifier = Modifier.size(40.dp), contentScale = ContentScale.Fit)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bank.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextColorPrimary)
                Text(bank.accountNumber, fontSize = 14.sp, color = EmeraldDeep, fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(bank.accountNumber))
                    Toast.makeText(context, "Disalin", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, null, tint = EmeraldDeep, modifier = Modifier.size(20.dp))
            }
        }
    }
}