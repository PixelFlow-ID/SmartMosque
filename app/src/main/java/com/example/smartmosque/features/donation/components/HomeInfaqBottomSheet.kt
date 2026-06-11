package com.example.smartmosque.features.donation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmosque.features.home.InfaqCategoryHome
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.TextBlack
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary

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
                    SuggestionChip(
                        onClick = { amountText = amt.toString() },
                        label = { Text("${amt / 1000}rb") },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if (amountText == amt.toString()) EmeraldDeep.copy(alpha = 0.1f) else Color.Transparent),
                        border = BorderStroke(1.dp, if (amountText == amt.toString()) EmeraldDeep else Color.LightGray)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { if (amountText.isNotEmpty()) onNext(amountText.toLong()) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                enabled = amountText.isNotEmpty()
            ) {
                Text("Lanjut Pembayaran", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}