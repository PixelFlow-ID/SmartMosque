package com.example.smartmosque.features.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartmosque.features.auth.AuthState
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.features.notification.component.* // Import semua kartu baru
import com.example.smartmosque.model.Donation
import com.example.smartmosque.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    notifViewModel: NotificationViewModel = viewModel()
) {
    val pendingDonations by notifViewModel.pendingDonations.collectAsState()
    val jamaahNotifications by notifViewModel.jamaahNotifications.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user

    val isAdmin = currentUser?.email == "ramdanidoni244@gmail.com"

    LaunchedEffect(currentUser) {
        if (!isAdmin && currentUser != null) {
            notifViewModel.fetchJamaahNotifications(currentUser.uid)
        }
    }

    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedDonation by remember { mutableStateOf<Donation?>(null) }

    Scaffold(
        containerColor = BgPremium,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = if(isAdmin) "Admin Panel" else "Kotak Masuk", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BgPremium)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isAdmin) {
                if (pendingDonations.isNotEmpty()) {
                    Text(
                        text = "Menunggu Verifikasi (${pendingDonations.size})",
                        fontWeight = FontWeight.Bold,
                        color = TextColorPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 8.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(pendingDonations) { donation ->
                            AdminVerificationCard(
                                donation = donation,
                                onClick = { selectedDonation = donation; showDetailDialog = true },
                                onApprove = { notifViewModel.approveDonation(donation) },
                                onReject = { notifViewModel.rejectDonation(donation.id) }
                            )
                        }
                    }
                } else {
                    EmptyStateNotif(message = "Tidak ada donasi pending saat ini.")
                }
            } else {
                if (jamaahNotifications.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(jamaahNotifications) { item ->
                            when (item) {
                                is JamaahNotificationItem.DonationStatus -> JamaahDonationCard(item.donation)
                                is JamaahNotificationItem.NewSchedule -> JamaahScheduleCard(item.schedule)
                                is JamaahNotificationItem.NewWaqf -> JamaahWaqfCard(item.waqf)
                                is JamaahNotificationItem.GeneralNotification -> JamaahGeneralCard(item)
                            }
                        }
                    }
                } else {
                    EmptyStateNotif(message = "Belum ada notifikasi baru dalam 30 hari terakhir.")
                }
            }
        }
    }

    if (showDetailDialog && selectedDonation != null) {
        DonationDetailDialog(
            donation = selectedDonation!!,
            onDismiss = { showDetailDialog = false },
            onApprove = { notifViewModel.approveDonation(selectedDonation!!); showDetailDialog = false },
            onReject = { notifViewModel.rejectDonation(selectedDonation!!.id); showDetailDialog = false }
        )
    }
}

@Composable
fun EmptyStateNotif(message: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.NotificationsNone, null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = TextColorSecondary, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
    }
}