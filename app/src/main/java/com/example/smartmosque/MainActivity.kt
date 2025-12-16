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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.features.donation.donationGraph
import com.example.smartmosque.features.home.HomeScreen // Panggil dari file baru
import com.example.smartmosque.features.home.HomeViewModel
import com.example.smartmosque.features.home.components.BottomNavBar
import com.example.smartmosque.features.schedule.AddScheduleScreen
import com.example.smartmosque.features.schedule.EditScheduleScreen
import com.example.smartmosque.ui.theme.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) Log.d("FCM", "Izin notifikasi diberikan")
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()

        FirebaseMessaging.getInstance().subscribeToTopic("general")
        FirebaseMessaging.getInstance().subscribeToTopic("waqf")
        FirebaseMessaging.getInstance().subscribeToTopic("events")

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
    val context = LocalContext.current

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
            // AUTH
            composable(Screen.Onboarding.route) { com.example.smartmosque.features.auth.OnboardingScreen(navController, authViewModel) }
            // Login & Register
            composable(
                route = Screen.Login.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(700)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(700)) }
            ) {
                com.example.smartmosque.features.auth.LoginScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    onLoginClick = { e, p -> authViewModel.login(e, p) },
                    onRegisterClick = { n, e, ph, p -> authViewModel.register(n, e, ph, p) },
                    onGoogleSignInClick = { t -> authViewModel.firebaseSignInWithGoogle(t) }
                )
            }
            composable(Screen.Register.route) { com.example.smartmosque.features.auth.RegisterScreen(navController, authViewModel) }

            // MAIN FEATURES
            composable(Screen.Home.route) {
                // PANGGIL HOMESCREEN DARI FILE BARU
                HomeScreen(navController, authViewModel, homeViewModel)
            }

            composable(Screen.Schedule.route) {
                com.example.smartmosque.features.schedule.ScheduleScreen(navController, authViewModel)
            }

            // SCHEDULE
            composable(
                route = Screen.ScheduleDetail.route,
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
            ) { backStackEntry ->
                com.example.smartmosque.features.schedule.ScheduleDetailScreen(
                    navController,
                    backStackEntry.arguments?.getString("scheduleId"),
                    authViewModel
                )
            }
            composable(
                route= Screen.AddSchedule.route
            ){
                AddScheduleScreen(navController = navController)
            }
            donationGraph(navController, authViewModel)


            composable(
                route = "edit_schedule/{scheduleId}",
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
            ) { backStackEntry ->
                // Ambil ID dari argumen
                val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: ""

                // Panggil Screen Edit
                EditScheduleScreen(
                    navController = navController,
                    scheduleId = scheduleId
                )
            }

            // PROFILE
            composable(Screen.Notification.route) { com.example.smartmosque.features.notification.NotificationScreen(navController, authViewModel) }
            composable(Screen.ProfileDetail.route) { com.example.smartmosque.features.profile.ProfileDetailScreen(navController, authViewModel) }
            composable(Screen.EditProfile.route) { com.example.smartmosque.features.profile.EditProfileScreen(navController, authViewModel) }
            composable(Screen.AboutMosque.route) { com.example.smartmosque.features.profile.AboutMosqueScreen(navController, authViewModel) }
        }
    }
}