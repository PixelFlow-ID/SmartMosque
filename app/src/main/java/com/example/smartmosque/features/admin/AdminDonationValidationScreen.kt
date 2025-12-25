package com.example.smartmosque.features.admin

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
import coil.compose.AsyncImage
import com.example.smartmosque.ui.theme.GreenPrimary
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDonationValidationScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var donationList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // State untuk Dialog Bukti Transfer (Zoom Image)
    var selectedProofUrl by remember { mutableStateOf<String?>(null) }

    // Ambil Data Donasi Status PENDING
    LaunchedEffect(Unit) {
        Firebase.firestore.collection("donations")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        doc.data!!.plus("id" to doc.id)
                    }
                    donationList = list
                }
            }
    }

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
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
            } else if (donationList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada antrian validasi.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(donationList) { data ->
                        ValidationItemCard(
                            data = data,
                            onViewProof = { url -> selectedProofUrl = url },
                            onAction = { action, id, projectId, amount ->
                                processValidation(action, id, projectId, amount,
                                    onSuccess = {
                                        Toast.makeText(context, "Berhasil diproses", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = {
                                        Toast.makeText(context, "Gagal memproses", Toast.LENGTH_SHORT).show()
                                    }
                                )
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
    data: Map<String, Any>,
    onViewProof: (String) -> Unit,
    onAction: (String, String, String?, Long) -> Unit
) {
    val id = data["id"] as String
    val amount = data["amount"] as? Long ?: 0L
    val category = data["category"] as? String ?: "Wakaf"
    val proofUrl = data["proofUrl"] as? String
    val projectId = data["projectId"] as? String // Bisa null jika Infaq Umum
    val timestamp = data["date"] as? com.google.firebase.Timestamp
    val date = timestamp?.toDate() ?: java.util.Date()

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
            if (proofUrl != null) {
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
                    onClick = { onAction("REJECT", id, null, 0) },
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
                    onClick = { onAction("APPROVE", id, projectId, amount) },
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

// LOGIKA TRANSAKSI FIREBASE (CRUCIAL)
fun processValidation(
    action: String,
    donationId: String,
    projectId: String?,
    amount: Long,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val db = Firebase.firestore

    if (action == "REJECT") {
        // Jika ditolak, cukup ubah status jadi FAILED/REJECTED
        db.collection("donations").document(donationId)
            .update("status", "REJECTED")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError() }
        return
    }

    if (action == "APPROVE") {
        // JIKA DISETUJUI: Gunakan Batch/Transaction untuk update 2 tempat
        db.runTransaction { transaction ->
            // 1. Ubah status donasi jadi SUCCESS
            val donationRef = db.collection("donations").document(donationId)
            transaction.update(donationRef, "status", "SUCCESS")

            // 2. Jika ini Wakaf (ada projectId), Tambahkan saldo ke Program Wakaf
            if (projectId != null && projectId.isNotEmpty()) {
                val projectRef = db.collection("waqf_programs").document(projectId)
                val snapshot = transaction.get(projectRef)

                // Ambil saldo lama, tambah nominal baru
                val currentCollected = snapshot.getLong("collectedAmount") ?: 0L
                val newCollected = currentCollected + amount

                transaction.update(projectRef, "collectedAmount", newCollected)
            }
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onError()
        }
    }
}