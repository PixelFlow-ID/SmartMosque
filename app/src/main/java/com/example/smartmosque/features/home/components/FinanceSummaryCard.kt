package com.example.smartmosque.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmosque.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FinanceSummaryCard(
    balance: Long,
    income: Long,
    expense: Long,
    onClick: () -> Unit
) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = EmeraldDeep.copy(0.2f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).background(EmeraldLight.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AccountBalanceWallet, null, tint = EmeraldDeep, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Kas Masjid", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                }

                Surface(onClick = onClick, shape = RoundedCornerShape(50), color = EmeraldDeep.copy(0.1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Detail", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = EmeraldDeep, modifier = Modifier.size(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Balance
            Text(formatRp.format(balance), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextBlack)
            Text("Saldo saat ini", fontSize = 12.sp, color = TextColorSecondary)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = GrayInputBackground)
            Spacer(modifier = Modifier.height(12.dp))

            // Income / Expense
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Pemasukan", fontSize = 10.sp, color = TextColorSecondary)
                        Text(formatRp.format(income), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }
                }
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, null, tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Pengeluaran", fontSize = 10.sp, color = TextColorSecondary)
                        Text(formatRp.format(expense), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }
                }
            }
        }
    }
}