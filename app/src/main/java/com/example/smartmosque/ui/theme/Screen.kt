package com.example.smartmosque.ui.theme

sealed class Screen(val route: String) {
    // Auth & Intro
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object Welcome : Screen("welcome")

    // Fitur Utama
    object Home : Screen("home")

    // --- Jadwal / Schedule ---
    object Schedule : Screen("schedule")
    object AddSchedule : Screen("add_schedule")
    object ScheduleDetail : Screen("schedule_detail/{scheduleId}")

    // --- Wakaf ---
    object Donation : Screen("donation")
    object AddWaqfProgram : Screen("add_waqf")

    // Profile
    object ProfileDetail : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object AboutMosque : Screen("about")

    // Route dengan Parameter (Detail Wakaf)
    object WaqfDetail : Screen("waqf_detail/{projectId}")

    object Notification : Screen("notification")

    // --- HELPER FUNCTION (Wajib dalam companion object agar bisa dipanggil 'Screen.namaFungsi') ---
    companion object {

        // Helper untuk Wakaf (Yang sudah ada)
        fun createRoute(projectId: String) = "waqf_detail/$projectId"

        // Helper untuk Jadwal
        fun createScheduleRoute(scheduleId: String) = "schedule_detail/$scheduleId"
    }
}
