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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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
import com.example.smartmosque.features.schedule.AddScheduleScreen
import com.example.smartmosque.features.schedule.EditScheduleScreen
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
            FirebaseMessaging.getInstance().subscribeToTopic("general")
            FirebaseMessaging.getInstance().subscribeToTopic("waqf")
            FirebaseMessaging.getInstance().subscribeToTopic("events")
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
    // val context = LocalContext.current // (Unused variable removed)

    // --- INISIALISASI VIEWMODEL (TANPA HILT) ---
    // PENTING: Pastikan AuthViewModel & HomeViewModel constructor-nya KOSONG
    // atau Repository-nya diinisialisasi manual di dalam ViewModel tersebut.
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()

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
                HomeScreen(navController, authViewModel, homeViewModel)
            }

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

            composable(route = Screen.AddSchedule.route){
                AddScheduleScreen(navController = navController)
            }

            composable(
                route = "edit_schedule/{scheduleId}",
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: ""
                EditScheduleScreen(
                    navController = navController,
                    scheduleId = scheduleId
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