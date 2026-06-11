package com.example.smartmosque.features.notification.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.smartmosque.model.Donation
import com.example.smartmosque.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminVerificationCard(
    donation: Donation,
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var donorName by remember { mutableStateOf("Memuat...") }
    LaunchedEffect(donation.userId) {
        if (donation.userId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(donation.userId).get()
                .addOnSuccessListener { donorName = it.getString("fullName") ?: "Hamba Allah" }
        }
    }

    val formattedAmount = try { NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(donation.amount) } catch (e: Exception) { "Rp ${donation.amount}" }

    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(BgPremium, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ReceiptLong, null, tint = EmeraldDeep)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(donation.category, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary)
                    Text("Oleh: $donorName", fontSize = 14.sp, color = TextColorSecondary)
                }
                Text(formattedAmount, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldDeep)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = GrayInputBackground, contentColor = TextColorSecondary), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Text("Detail", fontSize = 14.sp) }
                Button(onClick = onReject, colors = ButtonDefaults.buttonColors(containerColor = RedError.copy(alpha = 0.1f), contentColor = RedError), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Text("Tolak", fontSize = 14.sp) }
                Button(onClick = onApprove, colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Text("Terima", fontSize = 14.sp) }
            }
        }
    }
}