package com.example.smartmosque.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
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
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.viewmodel.NotificationViewModel
import com.example.smartmosque.data.model.Donation
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDonationValidationScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val donationList by notificationViewModel.pendingDonations.collectAsState()
    
    // State untuk Dialog Bukti Transfer (Zoom Image)
    var selectedProofUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Validasi Donasi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color(0xFFF7F7F7))) {
            if (donationList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada antrian validasi.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(donationList) { donation ->
                        ValidationItemCard(
                            donation = donation,
                            onViewProof = { url -> selectedProofUrl = url },
                            onAction = { action, don ->
                                if (action == "APPROVE") {
                                    notificationViewModel.approveDonation(don,
                                        onSuccess = { Toast.makeText(context, "Donasi disetujui", Toast.LENGTH_SHORT).show() },
                                        onFailure = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                    )
                                } else {
                                     notificationViewModel.rejectDonation(don.id,
                                        onSuccess = { Toast.makeText(context, "Donasi ditolak", Toast.LENGTH_SHORT).show() },
                                        onFailure = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                     )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // DIALOG ZOOM BUKTI TRANSFER
    if (selectedProofUrl != null) {
        AlertDialog(
            onDismissRequest = { selectedProofUrl = null },
            confirmButton = {
                TextButton(onClick = { selectedProofUrl = null }) { Text("Tutup") }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    AsyncImage(
                        model = selectedProofUrl,
                        contentDescription = "Bukti Transfer",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        )
    }
}

@Composable
fun ValidationItemCard(
    donation: Donation,
    onViewProof: (String) -> Unit,
    onAction: (String, Donation) -> Unit
) {
    val amount = donation.amount
    val category = donation.category
    val proofUrl = donation.proofUrl
    val date = donation.date?.toDate() ?: Date()

    val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    val formatDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(date)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Info
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(formatDate, fontSize = 11.sp, color = Color.Gray)
                }
                Text(formatRupiah, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GreenPrimary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Area Bukti Transfer
            if (proofUrl.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { onViewProof(proofUrl) }
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Image, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lihat Bukti Transfer", fontSize = 12.sp, color = Color.Blue)
                }
            } else {
                Text("Tidak ada bukti gambar (Saweria/Manual)", fontSize = 12.sp, color = Color.Red)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Tombol Aksi (Tolak / Terima)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tombol Tolak
                Button(
                    onClick = { onAction("REJECT", donation) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tolak")
                }

                // Tombol Terima
                Button(
                    onClick = { onAction("APPROVE", donation) },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Validasi")
                }
            }
        }
    }
}
