package com.example.smartmosque.features.schedule

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage

// --- IMPORT PERLU DITAMBAHKAN ---
import com.example.smartmosque.utils.ImageUtils // Pastikan File ImageUtils ada di package utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// --------------------------------

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.smartmosque.features.auth.AuthViewModel
import com.example.smartmosque.model.Schedule
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.EmeraldLight
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary

@Composable
fun ScheduleDetailScreen(
    navController: NavController,
    scheduleId: String?,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val userId = currentUser?.uid
    val scope = rememberCoroutineScope()

    // State Data
    var schedule by remember { mutableStateOf<Schedule?>(null) }
    var selectedTab by remember { mutableStateOf(1) } // 1 = Online, 0 = Offline

    // State Foto & Upload
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoUri = uri
    }

    // --- FETCH DATA ---
    LaunchedEffect(scheduleId) {
        if (scheduleId != null) {
            FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val data = snapshot.toObject(Schedule::class.java)?.copy(id = snapshot.id)
                            schedule = data
                            if (data?.streamingUrl.isNullOrEmpty()) {
                                selectedTab = 0
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
        }
    }

    Scaffold(containerColor = BgPremium) { paddingValues ->
        if (schedule == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldDeep)
            }
        } else {
            val s = schedule!!
            // Cek apakah user sudah ada di list offline
            val isOfflinePresent = if (userId != null) s.participantsOffline.contains(userId) else false

            // --- LOGIKA WAKTU (BARU) ---
            val scheduleDate = s.date?.toDate() ?: Date()
            val now = Date()
            // Acara dianggap mulai jika waktu sekarang >= waktu jadwal
            val isEventStarted = now.after(scheduleDate)

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ... (HEADER & INFO UTAMA SAMA SEPERTI SEBELUMNYA) ...
                // 1. HEADER
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 20.dp, end = 20.dp)
                ) {
                    Surface(
                        onClick = { navController.popBackStack() },
                        shape = CircleShape, color = White, shadowElevation = 4.dp,
                        modifier = Modifier.size(40.dp).align(Alignment.CenterStart)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextColorPrimary)
                        }
                    }
                    Text(
                        text = "Detail Kegiatan", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = TextColorPrimary, modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. INFO UTAMA
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Surface(color = EmeraldLight.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = s.category, color = EmeraldDeep, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(s.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, color = TextColorPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, tint = TextColorSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bersama Ust. ${s.speaker}", color = TextColorSecondary, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 3. TAB NAVIGASI
                Row(
                    modifier = Modifier
                        .fillMaxWidth().padding(horizontal = 24.dp).height(50.dp)
                        .clip(RoundedCornerShape(50)).background(Color.White),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PremiumTabButton("Streaming", Icons.Default.PlayCircle, selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
                    PremiumTabButton("Hadir Fisik", Icons.Default.LocationOn, selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. KONTEN
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (selectedTab == 1) {
                        PremiumOnlineSection(s.streamingUrl, userId, s.id, s.participantsOnline)
                    } else {
                        // --- SECTION OFFLINE (UPDATED) ---
                        PremiumOfflineSection(
                            hasAttendance = isOfflinePresent,
                            isEventStarted = isEventStarted, // Parameter Baru
                            photoUri = photoUri,
                            isLoading = isUploading,
                            onUploadClick = {
                                // Hanya bisa klik jika acara sudah mulai & tidak sedang loading
                                if (!isUploading && isEventStarted) launcher.launch("image/*")
                            },
                            onSubmit = {
                                if (userId != null && photoUri != null) {
                                    isUploading = true
                                    scope.launch(Dispatchers.IO) {
                                        // Proses Kompresi & Upload (Sama seperti sebelumnya)
                                        val compressedFile = ImageUtils.compressImage(context, photoUri!!)
                                        if (compressedFile != null) {
                                            CloudinaryHelper.uploadFile(context, compressedFile.absolutePath) { imageUrl ->
                                                if (imageUrl != null) {
                                                    val db = FirebaseFirestore.getInstance()
                                                    val attendanceData = hashMapOf(
                                                        "userId" to userId,
                                                        "scheduleId" to s.id,
                                                        "date" to Date(),
                                                        "type" to "OFFLINE_PHOTO",
                                                        "photoUrl" to imageUrl
                                                    )
                                                    db.collection("attendance").add(attendanceData).addOnSuccessListener {
                                                        db.collection("schedules").document(s.id)
                                                            .update("participantsOffline", FieldValue.arrayUnion(userId))
                                                            .addOnSuccessListener {
                                                                try { compressedFile.delete() } catch (e: Exception){}
                                                                scope.launch(Dispatchers.Main) {
                                                                    isUploading = false
                                                                    photoUri = null
                                                                    Toast.makeText(context, "Absensi Berhasil!", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                    }.addOnFailureListener {
                                                        scope.launch(Dispatchers.Main) { isUploading = false }
                                                    }
                                                } else {
                                                    scope.launch(Dispatchers.Main) { isUploading = false }
                                                }
                                            }
                                        } else {
                                            scope.launch(Dispatchers.Main) { isUploading = false }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

// ==========================================
// OBJECT HELPER CLOUDINARY (UPDATED)
// ==========================================
object CloudinaryHelper {
    private const val CLOUD_NAME = "dhzn4vwic"
    private const val UPLOAD_PRESET = "masjid_upload"

    // Init function (Lazy)
    private fun init(context: Context) {
        try {
            MediaManager.get()
        } catch (e: Exception) {
            val config = HashMap<String, String>()
            config["cloud_name"] = CLOUD_NAME
            MediaManager.init(context.applicationContext, config)
        }
    }

    // Fungsi Lama (Upload pakai Uri)
    fun uploadImage(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        init(context)
        MediaManager.get().upload(uri)
            .unsigned(UPLOAD_PRESET)
            .option("folder", "absensi_masjid")
            .callback(createCallback(onResult))
            .dispatch()
    }

    // Fungsi Baru (Upload pakai File Path String untuk hasil kompresi)
    fun uploadFile(context: Context, filePath: String, onResult: (String?) -> Unit) {
        init(context)
        MediaManager.get().upload(filePath)
            .unsigned(UPLOAD_PRESET)
            .option("folder", "absensi_masjid")
            .callback(createCallback(onResult))
            .dispatch()
    }

    // Callback Helper agar tidak duplikasi kode
    private fun createCallback(onResult: (String?) -> Unit) = object : UploadCallback {
        override fun onStart(requestId: String) {}
        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
            val url = resultData["secure_url"] as? String
            onResult(url)
        }
        override fun onError(requestId: String, error: ErrorInfo) {
            Log.e("Cloudinary", "Error: ${error.description}")
            onResult(null)
        }
        override fun onReschedule(requestId: String, error: ErrorInfo) {}
    }
}

// ==========================================
// UI COMPONENTS
// ==========================================

@Composable
fun PremiumTabButton(text: String, icon: ImageVector, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(50))
            .background(if (isSelected) EmeraldDeep else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) White else TextColorSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) White else TextColorSecondary)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PremiumOnlineSection(streamingUrl: String, userId: String?, scheduleId: String, currentParticipants: List<String>) {
    // 1. Ekstrak ID
    val videoIdRaw = remember(streamingUrl) { extractYouTubeId(streamingUrl) }

    // 2. Logic Absensi
    val isAlreadyJoined = remember(userId, currentParticipants) { currentParticipants.contains(userId) }
    LaunchedEffect(videoIdRaw, userId) {
        if (videoIdRaw != null && userId != null && !isAlreadyJoined) {
            FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                .update("participantsOnline", FieldValue.arrayUnion(userId))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column {
            // CONTAINER VIDEO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                // TRIK: Cek null di sini, lalu buat variabel baru yang NON-NULL (safeId)
                // Ini mengatasi error "Type mismatch" atau "Smart cast impossible" di dalam update block
                if (videoIdRaw != null) {
                    val safeId = videoIdRaw

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                }
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()
                                // Load URL
                                loadUrl("https://www.youtube.com/embed/$safeId?playsinline=1&rel=0")
                            }
                        },
                        update = { webView ->
                            val currentUrl = webView.url
                            // Di sini kita pakai 'safeId' yang sudah pasti String (bukan String?)
                            if (currentUrl == null || !currentUrl.contains(safeId)) {
                                webView.loadUrl("https://www.youtube.com/embed/$safeId?playsinline=1&rel=0")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Tampilan jika Link Error / Offline
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VideocamOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Siaran Offline",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // INFO SECTION
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (videoIdRaw != null) Color.Red else Color.Gray)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (videoIdRaw != null) "Live Streaming" else "Offline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (videoIdRaw != null) Color.Red else Color.Gray
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Kehadiran online Anda dicatat otomatis saat menonton tayangan ini.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun PremiumOfflineSection(
    hasAttendance: Boolean,
    isEventStarted: Boolean, // Parameter baru untuk cek waktu
    photoUri: Uri?,
    isLoading: Boolean,
    onUploadClick: () -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column {
            // --- 1. VISUAL HEADER (Area Kamera/Foto) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    // Jika belum mulai, background abu gelap. Jika sudah absen, hijau tipis.
                    .background(
                        when {
                            hasAttendance -> EmeraldLight.copy(alpha = 0.2f)
                            !isEventStarted -> Color(0xFFF5F5F5) // Abu-abu "Disabled"
                            else -> BgPremium
                        }
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    // Klik hanya aktif jika acara SUDAH MULAI, BELUM ABSEN, dan TIDAK LOADING
                    .clickable(enabled = isEventStarted && !hasAttendance && !isLoading) { onUploadClick() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    // KONDISI A: SUDAH ABSEN (Sukses)
                    hasAttendance -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Verified, null, tint = EmeraldDeep, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Terverifikasi", color = EmeraldDeep, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    // KONDISI B: FOTO SUDAH DIAMBIL (Preview)
                    photoUri != null -> {
                        AsyncImage(
                            model = photoUri, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                        )
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Text("Ketuk untuk ubah foto", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // KONDISI C: BELUM MULAI (Tampilan Kamera Mati/Disabled)
                    !isEventStarted -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum Dimulai", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // KONDISI D: ACARA BERLANGSUNG (Tampilan Kamera Aktif)
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, null, tint = EmeraldDeep, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ambil Foto Kehadiran", color = TextColorSecondary, fontSize = 14.sp)
                        }
                    }
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldDeep)
                    }
                }
            }

            // --- 2. INFO & ACTION SECTION ---
            Column(modifier = Modifier.padding(20.dp)) {

                // --- LOGIKA TEXT JUDUL & DESKRIPSI ---
                val titleText = when {
                    hasAttendance -> "Jazakallah Khair"
                    !isEventStarted -> "Mohon Bersabar"
                    else -> "Konfirmasi Kehadiran" // Acara sedang berlangsung
                }

                val descText = when {
                    hasAttendance -> "Bukti foto telah terkirim. Kehadiran fisik Anda di masjid telah tercatat sistem."
                    !isEventStarted -> "Silahkan bergabung saat kajian sudah dimulai untuk melakukan absensi."
                    else -> "Jangan lupa lakukan absen. Ambil foto selfie di area masjid sebagai bukti kehadiran."
                }

                Text(text = titleText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = descText, fontSize = 14.sp, color = TextColorSecondary, lineHeight = 20.sp)

                // Tombol Kirim (Hanya muncul jika belum absen)
                if (!hasAttendance) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldDeep,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
                        ),
                        // Tombol aktif HANYA JIKA foto ada, tidak loading, dan acara SUDAH MULAI
                        enabled = photoUri != null && !isLoading && isEventStarted
                    ) {
                        Text(
                            text = if (isLoading) "Mengirim..." else "Kirim Absensi",
                            fontWeight = FontWeight.Bold,
                            color = if (photoUri != null && isEventStarted) Color.White else TextColorSecondary
                        )
                    }
                }
            }
        }
    }
}


fun extractYouTubeId(url: String): String? {
    if (url.isBlank()) return null

    return try {
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: return null

        when {
            // Case 1: youtube.com/watch?v=ID
            host.contains("youtube.com") && uri.getQueryParameter("v") != null -> {
                uri.getQueryParameter("v")
            }
            // Case 2: youtube.com/live/ID
            host.contains("youtube.com") && uri.pathSegments.contains("live") -> {
                val pathSegments = uri.pathSegments
                val liveIndex = pathSegments.indexOf("live")
                if (liveIndex + 1 < pathSegments.size) pathSegments[liveIndex + 1] else null
            }
            // Case 3: youtube.com/embed/ID
            host.contains("youtube.com") && uri.pathSegments.contains("embed") -> {
                uri.lastPathSegment
            }
            // Case 4: youtu.be/ID
            host.contains("youtu.be") -> {
                uri.lastPathSegment
            }
            else -> null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}