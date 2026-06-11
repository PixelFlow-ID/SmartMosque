package com.example.smartmosque.features.admin.presentation.schedule

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    navController: NavController,
    scheduleId: String,
    viewModel: AdminScheduleViewModel
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // State lokal untuk form
    var title by remember { mutableStateOf("") }
    var speaker by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var streamingUrl by remember { mutableStateOf("") } // Ditambahkan agar klop dengan ViewModel
    var category by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    // Pecahan jam untuk mempermudah form sinkron dengan penambahan data
    var startTime by remember { mutableStateOf("18:00") }
    var endTime by remember { mutableStateOf("20:00") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // --- FETCH DATA LAMA VIA VIEWMODEL ---
    LaunchedEffect(scheduleId) {
        viewModel.fetchScheduleDetail(
            id = scheduleId,
            onSuccess = { doc ->
                if (doc.exists()) {
                    title = doc.getString("title") ?: ""
                    speaker = doc.getString("speaker") ?: ""
                    location = doc.getString("location") ?: ""
                    streamingUrl = doc.getString("streamingUrl") ?: ""
                    category = doc.getString("category") ?: ""
                    selectedDate = doc.getTimestamp("date")?.toDate()

                    // Memecah kembali string "18:00 - 20:00" dari Firebase menjadi startTime & endTime
                    val rawTime = doc.getString("time") ?: "18:00 - 20:00"
                    val timeParts = rawTime.split(" - ")
                    if (timeParts.size == 2) {
                        startTime = timeParts[0]
                        endTime = timeParts[1]
                    }
                } else {
                    Toast.makeText(context, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            },
            onError = { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Jadwal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        containerColor = BgPremium
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldDeep)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Kajian") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = speaker,
                    onValueChange = { speaker = it },
                    label = { Text("Nama Pemateri") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // PERBAIKAN: Cara bersih membuat TextField DatePicker bisa diklik tanpa trik Box gaib melayang
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                ) {
                    OutlinedTextField(
                        value = if (selectedDate != null) SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(selectedDate!!) else "",
                        onValueChange = {},
                        label = { Text("Tanggal") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                        modifier = Modifier.fillMaxWidth(),
                        // Properti di bawah ini dikunci agar event klik ditangani sepenuhnya oleh Box pembungkusnya
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray,
                            disabledTrailingIconColor = EmeraldDeep
                        )
                    )
                }

                // Input jam mulai
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Jam Mulai (Contoh: 18:00)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Input jam selesai
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Jam Selesai (Contoh: 20:00)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = streamingUrl,
                    onValueChange = { streamingUrl = it },
                    label = { Text("Link YouTube (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori (Kajian Rutin/Tabligh Akbar)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- TOMBOL SIMPAN VIA VIEWMODEL ---
                Button(
                    onClick = {
                        if (title.isNotEmpty() && selectedDate != null) {
                            // PERBAIKAN: Parameter disamakan persis dengan updateSchedule di ViewModel baru kita
                            viewModel.updateSchedule(
                                id = scheduleId,
                                title = title,
                                speaker = speaker,
                                location = location,
                                category = category,
                                streamingUrl = streamingUrl,
                                startTime = startTime,
                                endTime = endTime,
                                selectedDate = selectedDate!!,
                                onSuccess = {
                                    Toast.makeText(context, "Jadwal berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(context, "Gagal: $errorMessage", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Mohon lengkapi judul dan tanggal", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Perubahan")
                    }
                }
            }
        }
    }
}