package com.example.smartmosque.features.donation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// --- IMPORT DARI AUTH & MVVM DONATION REFACTORED ---
import com.example.smartmosque.features.auth.AuthState

import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.features.donation.presentation.WaqfViewModel
import com.example.smartmosque.features.donation.components.*
import com.example.smartmosque.features.home.InfaqCategoryHome
import com.example.smartmosque.ui.theme.Screen

// --- IMPORT WARNA TEMA ---
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: WaqfViewModel = viewModel()
) {
    val context = LocalContext.current

    // State Wakaf dari ViewModel terpusat
    val waqfList by viewModel.waqfProjects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // State User Authentication
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user
    val isAdmin = currentUser?.email == "ramdanidoni244@gmail.com"

    // State Popups Kontrol Lokal Screen
    var showInfaqSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<InfaqCategoryHome?>(null) }
    var inputAmount by remember { mutableLongStateOf(0L) }

    Scaffold(
        containerColor = BgPremium,
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddWaqfProgram.route) },
                    containerColor = EmeraldDeep,
                    contentColor = White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Program")
                }
            }
        }
    ) { paddingValues ->

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. HEADER HALAMAN
            item { CleanDonationHeader() }

            // 2. SEKSI LAYANAN INFAQ
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Layanan Infaq", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumDonationGrid(
                        onItemClick = { category ->
                            selectedCategory = category
                            showInfaqSheet = true
                        }
                    )
                }
            }

            // Garis Pembatas Estetik
            item {
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 3. KEPALA SEKSI PROGRAM WAKAF
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Program Wakaf", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Investasi untuk rumah di surga.", fontSize = 13.sp, color = TextColorSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 4. DAFTAR PROGRAM WAKAF (KONTROL STATE MVVM)
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldDeep)
                    }
                }
            } else if (waqfList.isEmpty()) {
                item { EmptyStateDonation() }
            } else {
                // Sekarang aman digunakan karena import lazy.items sudah ditambahkan di atas
                items(waqfList) { project ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                        PremiumWaqfCard(
                            project = project,
                            isAdmin = isAdmin,
                            onClick = { navController.navigate(Screen.createRoute(project.id)) },
                            onDelete = {
                                viewModel.deleteProject(
                                    projectId = project.id,
                                    onSuccess = { Toast.makeText(context, "Program berhasil dihapus", Toast.LENGTH_SHORT).show() },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                )
                            }
                        )
                    }
                }
            }
        }

        // --- LAYER POPUP INTERAKSI DIALOG ---

        // Bottom Sheet Input Nominal
        if (showInfaqSheet && selectedCategory != null) {
            HomeInfaqBottomSheet(
                category = selectedCategory!!,
                onDismiss = { showInfaqSheet = false },
                onNext = { amount ->
                    inputAmount = amount
                    showInfaqSheet = false
                    showPaymentDialog = true
                }
            )
        }

        // Dialog Panduan Transfer & Unggah Bukti Transaksi
        if (showPaymentDialog && selectedCategory != null) {
            InfaqPaymentDialog(
                amount = inputAmount,
                categoryName = selectedCategory!!.title,
                authViewModel = authViewModel,
                viewModel = viewModel,
                onDismiss = { showPaymentDialog = false },
                onSuccess = {
                    showPaymentDialog = false
                    Toast.makeText(context, "Alhamdulillah, bukti terkirim!", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}