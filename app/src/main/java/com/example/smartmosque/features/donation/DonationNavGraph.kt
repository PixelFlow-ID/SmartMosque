package com.example.smartmosque.features.donation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.ui.theme.Screen


// Extension Function ini harus ada agar bisa dipanggil di MainActivity
fun NavGraphBuilder.donationGraph(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // 1. Halaman Utama Donasi
    composable(route = Screen.Donation.route) {
        DonationScreen(
            navController = navController,
            authViewModel = authViewModel
        )
    }

    // 2. Halaman Tambah Program Wakaf (Khusus Admin)
    composable(route = Screen.AddWaqfProgram.route) {
        AddWaqfProgramScreen(navController = navController)
    }

    // 3. Halaman Detail Wakaf
    composable(
        route = Screen.WaqfDetail.route,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId")
        WaqfDetailScreenNew(
            navController = navController,
            projectId = projectId,
            authViewModel = authViewModel
        )
    }

    // 4. Halaman Validasi Admin
    composable(route = "admin_validation") {
        com.example.smartmosque.features.admin.AdminDonationValidationScreen(navController)
    }
}