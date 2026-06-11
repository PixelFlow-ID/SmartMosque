package com.example.smartmosque.features.admin.presentation.finance

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartmosque.ui.theme.*
import com.example.smartmosque.features.auth.AuthViewModel
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFinanceScreen(
    navController: NavController,
    financeViewModel: AdminFinanceViewModel,
    authViewModel: AuthViewModel,
    transactionId: String? = null
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val transactions by financeViewModel.transactions.collectAsState()

    val transaction = remember(transactionId, transactions) {
        transactions.find { it.id == transactionId }
    }

    var title by remember(transaction) { mutableStateOf(transaction?.title ?: "") }
    var amountStr by remember(transaction) { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var type by remember(transaction) { mutableStateOf(transaction?.type ?: "INCOME") }
    var category by remember(transaction) { mutableStateOf(transaction?.category ?: "Umum") }
    var description by remember(transaction) { mutableStateOf(transaction?.description ?: "") }

    val categories = listOf("Umum", "Operasional", "Pembangunan", "Sosial", "Zakat/Infaq")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (transactionId == null) "Tambah Transaksi" else "Edit Transaksi",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDeep
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = EmeraldDeep)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        containerColor = BgPremium
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // --- TYPE SELECTOR ---
            Text("Jenis Transaksi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                val types = listOf("Pemasukan" to "INCOME", "Pengeluaran" to "EXPENSE")
                types.forEach { (label, value) ->
                    val isSelected = type == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) White else Color.Transparent)
                            .clickable { type = value }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) {
                                if (value == "INCOME") Color(0xFF4CAF50) else Color(0xFFF44336)
                            } else TextColorSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CATEGORY SELECTOR ---
            Text("Kategori", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            var showCategoryMenu by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = !showCategoryMenu }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldDeep,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                ExposedDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- TITLE / KETERANGAN ---
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Keterangan (Cth: Infaq Jumat)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldDeep,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- AMOUNT ---
            OutlinedTextField(
                value = amountStr,
                onValueChange = { if (it.all { c -> c.isDigit() }) amountStr = it },
                label = { Text("Nominal (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldDeep,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- DESCRIPTION ---
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Catatan Tambahan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldDeep,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- SAVE BUTTON ---
            Button(
                onClick = {
                    if (title.isBlank() || amountStr.isBlank()) {
                        Toast.makeText(context, "Judul dan Nominal harus diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val amount = amountStr.toLongOrNull() ?: 0L

                    if (transactionId == null) {
                        financeViewModel.addTransaction(
                            title = title,
                            description = description,
                            amount = amount,
                            type = type,
                            category = category,
                            date = Timestamp.now(),
                            createdBy = currentUser?.uid ?: "",
                            onSuccess = {
                                Toast.makeText(context, "Data Berhasil Ditambahkan", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = {
                                Toast.makeText(context, "Gagal: $it", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        financeViewModel.updateTransaction(
                            id = transactionId,
                            title = title,
                            description = description,
                            amount = amount,
                            type = type,
                            category = category,
                            onSuccess = {
                                Toast.makeText(context, "Data Berhasil Diperbarui", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = {
                                Toast.makeText(context, "Gagal: $it", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep)
            ) {
                Text(
                    if (transactionId == null) "Simpan Transaksi" else "Perbarui Transaksi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
        }
    }
}
