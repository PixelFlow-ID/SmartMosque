package com.example.smartmosque.features.schedule

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
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

    // --- SCOPE UNTUK BACKGROUND PROCESS ---
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

    // --- FETCH DATA (REALTIME) ---
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
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
            val isOfflinePresent = if (userId != null) s.participantsOffline.contains(userId) else false

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
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
                        // --- SECTION ONLINE ---
                        PremiumOnlineSection(s.streamingUrl, userId, s.id, s.participantsOnline)
                    } else {
                        // --- SECTION OFFLINE DENGAN KOMPRESI & CLOUDINARY ---
                        PremiumOfflineSection(
                            hasAttendance = isOfflinePresent,
                            photoUri = photoUri,
                            isLoading = isUploading,
                            onUploadClick = { if (!isUploading) launcher.launch("image/*") },
                            onSubmit = {
                                if (userId != null && photoUri != null) {
                                    isUploading = true

                                    // --- LOGIKA KOMPRESI DAN UPLOAD ---
                                    scope.launch(Dispatchers.IO) {
                                        // 1. Kompres Gambar
                                        val compressedFile = ImageUtils.compressImage(context, photoUri!!)

                                        if (compressedFile != null) {
                                            // 2. Upload File Hasil Kompresi (Path String)
                                            CloudinaryHelper.uploadFile(context, compressedFile.absolutePath) { imageUrl ->
                                                if (imageUrl != null) {
                                                    // 3. Simpan ke Firestore (Masih di Background Thread, perlu hati-hati)
                                                    val db = FirebaseFirestore.getInstance()

                                                    val attendanceData = hashMapOf(
                                                        "userId" to userId,
                                                        "scheduleId" to s.id,
                                                        "date" to Date(),
                                                        "type" to "OFFLINE_PHOTO",
                                                        "photoUrl" to imageUrl
                                                    )

                                                    db.collection("attendance").add(attendanceData)
                                                        .addOnSuccessListener {
                                                            // Update Absensi di Jadwal
                                                            db.collection("schedules").document(s.id)
                                                                .update("participantsOffline", FieldValue.arrayUnion(userId))
                                                                .addOnSuccessListener {
                                                                    // Bersihkan file cache
                                                                    try { compressedFile.delete() } catch (e: Exception){}

                                                                    // Update UI di Main Thread
                                                                    scope.launch(Dispatchers.Main) {
                                                                        isUploading = false
                                                                        photoUri = null
                                                                        Toast.makeText(context, "Absensi Berhasil!", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            scope.launch(Dispatchers.Main) {
                                                                isUploading = false
                                                                Toast.makeText(context, "Gagal simpan DB: ${e.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                } else {
                                                    scope.launch(Dispatchers.Main) {
                                                        isUploading = false
                                                        Toast.makeText(context, "Gagal upload gambar", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            scope.launch(Dispatchers.Main) {
                                                isUploading = false
                                                Toast.makeText(context, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Mohon ambil foto bukti kehadiran", Toast.LENGTH_SHORT).show()
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
    val videoId = remember(streamingUrl) { extractYouTubeId(streamingUrl) }
    val isAlreadyJoined = remember(userId, currentParticipants) { currentParticipants.contains(userId) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(8.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                if (videoId != null) {
                    AndroidView(
                        factory = {
                            WebView(it).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()
                            }
                        },
                        update = { it.loadUrl("https://www.youtube.com/embed/$videoId?playsinline=1") },
                        modifier = Modifier.fillMaxSize()
                    )
                    LaunchedEffect(Unit) {
                        if (userId != null && !isAlreadyJoined) {
                            FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                                .update("participantsOnline", FieldValue.arrayUnion(userId))
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.VideocamOff, null, tint = White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Text("Siaran belum dimulai", color = White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (videoId != null) Color.Red else Color.Gray))
                    Spacer(Modifier.width(8.dp))
                    Text(if (videoId != null) "Sedang Berlangsung" else "Offline", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (videoId != null) Color.Red else Color.Gray)
                }
                Spacer(Modifier.height(12.dp))
                Text("Kehadiran online Anda dicatat otomatis saat menonton tayangan ini.", fontSize = 14.sp, color = TextColorSecondary)
            }
        }
    }
}

@Composable
fun PremiumOfflineSection(
    hasAttendance: Boolean,
    photoUri: Uri?,
    isLoading: Boolean,
    onUploadClick: () -> Unit,
    onSubmit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(8.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (hasAttendance) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(EmeraldLight.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Verified, null, tint = EmeraldDeep, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Jazakallah Khair", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                Text("Kehadiran fisik Anda sudah tercatat.", color = TextColorSecondary, fontSize = 14.sp)
            } else {
                Text("Konfirmasi Kehadiran", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                Text("Ambil foto selfie di lokasi masjid", fontSize = 14.sp, color = TextColorSecondary)

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(160.dp).clip(RoundedCornerShape(20.dp))
                        .background(BgPremium)
                        .border(BorderStroke(2.dp, Color.Gray), RoundedCornerShape(20.dp))
                        .clickable(enabled = !isLoading) { onUploadClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(model = photoUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, null, tint = EmeraldDeep, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Ketuk Kamera", fontSize = 12.sp, color = EmeraldDeep, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                    enabled = photoUri != null && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mengirim...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Kirim Absensi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun extractYouTubeId(url: String): String? {
    val pattern = "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?|live)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/\\s]{11})"
    val compiledPattern = java.util.regex.Pattern.compile(pattern)
    val matcher = compiledPattern.matcher(url)
    return if (matcher.find()) matcher.group(1) else null
}