package com.example.smartmosque.features.finance

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.model.CashTransaction
import com.example.smartmosque.ui.theme.*
import com.example.smartmosque.features.finance.FinanceViewModel
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    financeViewModel: FinanceViewModel = viewModel()
) {
    val context = LocalContext.current

    // Auth Check
    val userRole by authViewModel.userRole.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAdmin = userRole == "admin" || currentUser?.email == "ramdanidoni244@gmail.com"

    // Data State
    val transactions by financeViewModel.transactions.collectAsState()
    val totalIncome by financeViewModel.totalIncome.collectAsState()
    val totalExpense by financeViewModel.totalExpense.collectAsState()
    val currentBalance by financeViewModel.currentBalance.collectAsState()
    val isLoading by financeViewModel.isLoading.collectAsState()

    // UI Logic
    var selectedTab by remember { mutableStateOf("Semua") } // Filter List: Semua, Masuk, Keluar

    // ANIMATION TRIGGER (Selalu animasi saat masuk)
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Scaffold(
        containerColor = BgPremium,
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddFinance.route) },
                    containerColor = EmeraldDeep,
                    contentColor = White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, "Tambah Transaksi")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- HEADER ---
            FinanceHeader(
                isAdmin = isAdmin,
                onBack = { navController.popBackStack() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- GRAFIK & STATISTICS CARD ---
            // Ini akan muncul dengan animasi
            AnimatedFinanceSummary(
                startAnimation = startAnimation,
                balance = currentBalance,
                income = totalIncome,
                expense = totalExpense
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- DAFTAR TRANSAKSI ---
            Text(
                "Riwayat Transaksi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextColorPrimary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Tabs
            Row(modifier = Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipSmart("Semua", selectedTab == "Semua") { selectedTab = "Semua" }
                FilterChipSmart("Pemasukan", selectedTab == "Pemasukan") { selectedTab = "Pemasukan" }
                FilterChipSmart("Pengeluaran", selectedTab == "Pengeluaran") { selectedTab = "Pengeluaran" }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldDeep)
                }
            } else if (transactions.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                    Text("Belum ada data keuangan.", color = TextColorSecondary)
                }
            } else {
                val filteredList = when(selectedTab) {
                    "Pemasukan" -> transactions.filter { it.type == "INCOME" }
                    "Pengeluaran" -> transactions.filter { it.type == "EXPENSE" }
                    else -> transactions
                }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { item ->
                        TransactionItem(
                            transaction = item,
                            isAdmin = isAdmin,
                            onEdit = {
                                navController.navigate("edit_finance/${item.id}")
                            },
                            onDelete = {
                                // Delete Logic Simpel
                                financeViewModel.deleteTransaction(item.id, {
                                    Toast.makeText(context, "Data Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                                }, {
                                    Toast.makeText(context, "Gagal Menghapus: $it", Toast.LENGTH_SHORT).show()
                                })
                            }
                        )
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET REMOVED IN FAVOR OF SCREENS ---
}

// ---------------- UI COMPONENTS -----------------

@Composable
fun FinanceHeader(isAdmin: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tombol Kembali
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = White,
            shadowElevation = 4.dp,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = EmeraldDeep,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Laporan Kas", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmeraldDeep)
            Text("Transparansi Keuangan", fontSize = 12.sp, color = TextColorSecondary)
        }
    }
}

@Composable
fun AnimatedFinanceSummary(
    startAnimation: Boolean,
    balance: Long,
    income: Long,
    expense: Long
) {
    // FORMAT RUPIAH
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRp.maximumFractionDigits = 0

    // ANIMASI BATANG
    // Hitung persentase max agar bar tidak mentok
    val maxValue = maxOf(income, expense, 1L).toFloat()

    // Animasi Tinggi Bar (0f -> 1f)
    val barProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "barGraph"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // JUDUL SALDO
            Text("Total Saldo Akhir", fontSize = 12.sp, color = TextColorSecondary, fontWeight = FontWeight.Medium)
            Text(
                text = formatRp.format(balance),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldDeep
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(24.dp))

            // VISUALISASI GRAFIK BATANG
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // BAR PEMASUKAN
                val incomeHeightConfig = (income.toFloat() / maxValue)
                // Tinggi minimum 10% agar tetap terlihat jika kecil, tapi 0 tetap 0
                val incomeHeight = if(income > 0) maxOf(incomeHeightConfig, 0.1f) else 0f

                StatBarColumn(
                    label = "Pemasukan",
                    value = formatRp.format(income),
                    progress = barProgress * incomeHeight,
                    color = Color(0xFF4CAF50), // Green 500
                    icon = Icons.Outlined.TrendingUp
                )

                // BAR PENGELUARAN
                val expenseHeightConfig = (expense.toFloat() / maxValue)
                val expenseHeight = if(expense > 0) maxOf(expenseHeightConfig, 0.1f) else 0f

                StatBarColumn(
                    label = "Pengeluaran",
                    value = formatRp.format(expense),
                    progress = barProgress * expenseHeight,
                    color = Color(0xFFF44336), // Red 500
                    icon = Icons.Outlined.TrendingDown
                )
            }
        }
    }
}

@Composable
fun StatBarColumn(
    label: String,
    value: String,
    progress: Float, // 0.0 - 1.0 (Height Percentage)
    color: Color,
    icon:  androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Angka di atas bar
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(4.dp))

        // Bar Grafik
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .width(40.dp)
                .weight(1f) // Isi sisa ruang vertikal
                .background(Color(0xFFF5F5F5), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            // Fill Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress) // ANIMATED HEIGHT
                    .background(color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )
            // Icon di dalam bar (biar cantik)
            if (progress > 0.2f) { // Hanya muncul jika bar cukup tinggi
                Icon(icon, null, tint = White.copy(0.8f), modifier = Modifier.padding(bottom = 8.dp).size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = TextColorSecondary)
    }
}

@Composable
fun TransactionItem(
    transaction: CashTransaction,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val icon = if (isIncome) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown
    val sign = if (isIncome) "+" else "-"

    val dateStr = transaction.date?.toDate()?.let {
        SimpleDateFormat("dd MMM yyyy", Locale("id")).format(it)
    } ?: "-"

    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRp.maximumFractionDigits = 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(White, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColorPrimary)
            Text(dateStr, fontSize = 12.sp, color = TextColorSecondary)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$sign ${formatRp.format(transaction.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
            if (isAdmin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Edit,
                        "Edit",
                        tint = EmeraldDeep,
                        modifier = Modifier.size(20.dp).clickable { onEdit() }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Delete,
                        "Hapus",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp).clickable { onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipSmart(text: String, startSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (startSelected) EmeraldDeep else Color.Transparent,
        shape = RoundedCornerShape(50),
        border = if (!startSelected) BorderStroke(1.dp, Color.LightGray) else null
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (startSelected) White else TextColorSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}




