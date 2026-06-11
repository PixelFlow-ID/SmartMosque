package com.example.smartmosque.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartmosque.features.finance.FinanceViewModel
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.features.schedule.components.MiniCardSchedule
import com.example.smartmosque.ui.theme.*
import com.example.smartmosque.features.home.components.*

// DATA MODEL
data class InfaqCategoryHome(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val iconColor: Color
)

@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    financeViewModel: FinanceViewModel
) {
    // --- AUTH STATE ---
    val currentUser by authViewModel.currentUser.collectAsState()
    val userName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Jamaah"
    val userInitial = userName.take(1).uppercase()

    // --- HOME STATE ---
    val hasUnreadNotifications by homeViewModel.hasUnreadNotifications.collectAsState()
    val ongoingEvent by homeViewModel.ongoingEvent.collectAsState()
    val eventsThisMonth by homeViewModel.eventsThisMonth.collectAsState()
    val totalParticipants by homeViewModel.totalParticipants.collectAsState()

    // --- FINANCE STATE ---
    val balance by financeViewModel.currentBalance.collectAsState()
    val income by financeViewModel.totalIncome.collectAsState()
    val expense by financeViewModel.totalExpense.collectAsState()

    // --- LOCAL UI STATE ---
    var showInfaqSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedInfaqCategory by remember { mutableStateOf<InfaqCategoryHome?>(null) }
    var amountToPay by remember { mutableStateOf(0L) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(BgPremium)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 10.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profil User
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { navController.navigate(Screen.ProfileDetail.route) }
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).shadow(4.dp, CircleShape, spotColor = EmeraldDeep.copy(alpha = 0.2f))
                            .clip(CircleShape).background(EmeraldDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userInitial, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Assalamualaikum,", fontSize = 12.sp, color = TextGrey, fontWeight = FontWeight.Medium)
                        Text("$userName 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }
                }

                // Tombol Notifikasi
                Surface(
                    onClick = {
                        homeViewModel.markNotificationsAsRead()
                        navController.navigate("notification")
                    },
                    modifier = Modifier.size(46.dp), shape = CircleShape, color = CardSurface, shadowElevation = 3.dp, tonalElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Notifications, "Notifikasi", tint = TextBlack, modifier = Modifier.size(24.dp))
                        if (hasUnreadNotifications) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        }
                    }
                }
            }

            // --- KONTEN SCROLL ---
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 1. DASHBOARD GRAFIK ANIMASI
                AnimatedEmeraldCard(eventsThisMonth = eventsThisMonth, totalParticipants = totalParticipants)

                Spacer(modifier = Modifier.height(24.dp))

                // 2. ONGOING EVENT (LIVE)
                ongoingEvent?.let { schedule ->
                    OngoingLiveCard(
                        schedule = schedule,
                        onClick = { navController.navigate("schedule_detail/${schedule.id}") }
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                }

                // 3. FINANCE SUMMARY
                FinanceSummaryCard(
                    balance = balance,
                    income = income,
                    expense = expense,
                    onClick = { navController.navigate("finance") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. WAKAF & EVENT
                com.example.smartmosque.features.donation.components.MiniWaqfProjectNew(navController)

                Spacer(modifier = Modifier.height(24.dp))

                // 5. MINI SCHEDULE
                MiniCardSchedule(navController, authViewModel)

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}