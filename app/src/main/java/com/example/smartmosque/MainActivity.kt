package com.example.smartmosque

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartmosque.features.admin.presentation.finance.AddEditFinanceScreen
import com.example.smartmosque.features.admin.presentation.finance.AdminFinanceViewModel

// --- IMPORT VIEWMODEL ---
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.features.home.HomeViewModel

// --- IMPORT SCREENS (Dari package 'features') ---
// Pastikan tidak ada yang merah di bagian ini.
// Jika merah, hapus barisnya lalu tekan Alt+Enter untuk import ulang.
import com.example.smartmosque.features.auth.OnboardingScreen
import com.example.smartmosque.features.auth.LoginScreen
import com.example.smartmosque.features.auth.RegisterScreen
import com.example.smartmosque.features.home.HomeScreen
import com.example.smartmosque.features.schedule.ScheduleScreen
import com.example.smartmosque.features.schedule.ScheduleDetailScreen
import com.example.smartmosque.features.admin.presentation.schedule.AddScheduleScreen
import com.example.smartmosque.features.admin.presentation.schedule.AdminScheduleViewModel
import com.example.smartmosque.features.admin.presentation.schedule.EditScheduleScreen
import com.example.smartmosque.features.notification.NotificationScreen
import com.example.smartmosque.features.profile.ProfileDetailScreen
import com.example.smartmosque.features.profile.EditProfileScreen
import com.example.smartmosque.features.profile.AboutMosqueScreen
import com.example.smartmosque.features.donation.donationGraph

// --- COMPONENT LAIN ---
import com.example.smartmosque.ui.components.BottomNavBar
import com.example.smartmosque.ui.theme.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Izin Notifikasi (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) Log.d("FCM", "Izin notifikasi diberikan")
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()

        // Subscribe ke Topik FCM (Bungkus try-catch agar aman)
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("system")    // Untuk pemeliharaan/update aplikasi
            FirebaseMessaging.getInstance().subscribeToTopic("wakaf")     // Untuk program wakaf baru
            FirebaseMessaging.getInstance().subscribeToTopic("schedule")  // Untuk agenda kajian masjid
            Log.d("MainActivity", "FCM Berhasil Subscribe ke semua topik utama")
        } catch (e: Exception) {
            Log.e("MainActivity", "FCM Subscribe error: ${e.message}")
        }

        setContent {
            SmartMosqueTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // --- INISIALISASI VIEWMODEL (TANPA HILT) ---

    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val financeViewModel: com.example.smartmosque.features.finance.FinanceViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Schedule.route,
        Screen.Donation.route,
        Screen.AboutMosque.route,
        Screen.ProfileDetail.route
    )

    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Onboarding.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            // --- AUTHENTICATION ---
            composable(Screen.Onboarding.route) {
                OnboardingScreen(navController, authViewModel)
            }
            composable(
                route = Screen.Login.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(700)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(700)) }
            ) {
                LoginScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    onLoginClick = { e, p -> authViewModel.login(e, p) },
                    onRegisterClick = { n, e, ph, p -> authViewModel.register(n, e, ph, p) },
                    onGoogleSignInClick = { t -> authViewModel.firebaseSignInWithGoogle(t) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(navController, authViewModel)
            }

            // --- MAIN FEATURES ---
            composable(Screen.Home.route) {
                HomeScreen(navController, authViewModel, homeViewModel, financeViewModel)
            }

            // FINANCE / LAPORAN KAS
            composable(Screen.Finance.route) {
                com.example.smartmosque.features.finance.FinanceScreen(navController, authViewModel, financeViewModel)
            }

            composable(Screen.AddFinance.route) {
                // Kelola inisialisasi dengan fungsi viewModel() agar aman dari badai recomposition
                val adminFinanceViewModel: AdminFinanceViewModel = viewModel()

                AddEditFinanceScreen(
                    navController = navController,
                    financeViewModel = adminFinanceViewModel,
                    authViewModel = authViewModel
                )
            }

            composable(
                route = Screen.EditFinance.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId")

                // Kelola inisialisasi secara mandiri khusus untuk layar edit ini
                val adminFinanceViewModel: AdminFinanceViewModel = viewModel()

                AddEditFinanceScreen(
                    navController = navController,
                    financeViewModel = adminFinanceViewModel,
                    authViewModel = authViewModel,
                    transactionId = transactionId
                )
            }

            // --- SCHEDULE
            composable(Screen.Schedule.route) {
                ScheduleScreen(navController, authViewModel)
            }

            // --- SCHEDULE DETAILS & EDIT ---
            composable(
                route = Screen.ScheduleDetail.route,
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
            ) { backStackEntry ->
                ScheduleDetailScreen(
                    navController,
                    backStackEntry.arguments?.getString("scheduleId"),
                    authViewModel
                )
            }

            composable(route = Screen.AddSchedule.route) {
                val adminScheduleViewModel: AdminScheduleViewModel = viewModel()
                AddScheduleScreen(
                    navController = navController,
                    viewModel = adminScheduleViewModel
                )
            }

            composable(
                route = "edit_schedule/{scheduleId}",
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: ""
                val adminScheduleViewModel: AdminScheduleViewModel = viewModel() // Menghidupkan scope Lifecycle VM

                EditScheduleScreen(
                    navController = navController,
                    scheduleId = scheduleId,
                    viewModel = adminScheduleViewModel
                )
            }

            // --- DONATION (Nested Graph) ---
            donationGraph(navController, authViewModel)

            // --- PROFILE & NOTIFICATIONS ---
            composable(Screen.Notification.route) {
                NotificationScreen(navController, authViewModel)
            }

            composable(Screen.ProfileDetail.route) {
                ProfileDetailScreen(navController, authViewModel)
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(navController, authViewModel)
            }

            composable(Screen.AboutMosque.route) {
                AboutMosqueScreen(navController, authViewModel)
            }
        }
    }
}