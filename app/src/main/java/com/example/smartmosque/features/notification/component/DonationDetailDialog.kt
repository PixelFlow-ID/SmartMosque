package com.example.smartmosque.features.notification.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.smartmosque.model.Donation
import com.example.smartmosque.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DonationDetailDialog(
    donation: Donation,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var donorName by remember { mutableStateOf("Memuat...") }
    var donorEmail by remember { mutableStateOf("-") }
    LaunchedEffect(donation.userId) {
        if (donation.userId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(donation.userId).get()
                .addOnSuccessListener {
                    donorName = it.getString("fullName") ?: "Hamba Allah"
                    donorEmail = it.getString("email") ?: "-"
                }
        }
    }
    val formattedAmount = try { NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(donation.amount) } catch (e: Exception) { "Rp ${donation.amount}" }
    val fullDate = try {
        val date = donation.date?.toDate() ?: java.util.Date()
        SimpleDateFormat("EEEE, dd MMMM yyyy - HH:mm", Locale("id", "ID")).format(date)
    } catch (e: Exception) { "-" }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = White), modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.Black)) {
                    if (donation.proofUrl.isNotEmpty()) {
                        AsyncImage(model = donation.proofUrl, contentDescription = "Bukti", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    } else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tidak ada bukti", color = Color.White, fontSize = 16.sp) } }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape).size(48.dp)) { Icon(Icons.Default.Close, null, tint = White) }
                }
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(donation.category, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Text("ID: ${donation.id.take(8)}...", fontSize = 14.sp, color = TextColorSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailRow(label = "Nominal", value = formattedAmount, isBold = true, color = EmeraldDeep)
                    DetailRow(label = "Tanggal", value = fullDate)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = GrayInputBackground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = TextColorSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pengirim", fontSize = 13.sp, color = TextColorSecondary)
                            Text(donorName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary)
                            Text(donorEmail, fontSize = 14.sp, color = TextColorSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onReject, colors = ButtonDefaults.buttonColors(containerColor = RedError.copy(alpha = 0.1f), contentColor = RedError), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(52.dp)) { Text("Tolak", fontSize = 16.sp) }
                        Button(onClick = onApprove, colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(52.dp)) { Text("Konfirmasi", fontSize = 16.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false, color: Color = TextColorPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 15.sp, color = TextColorSecondary)
        Text(value, fontSize = 16.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = color)
    }
}