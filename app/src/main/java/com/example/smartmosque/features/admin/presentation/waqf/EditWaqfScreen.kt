package com.example.smartmosque.features.admin.presentation.waqf

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWaqfScreen(
    navController: NavController,
    programId: String,
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var programType by remember { mutableStateOf("Masjid") }

    // Memuat data lama
    LaunchedEffect(programId) {
        viewModel.fetchWaqfDetail(
            id = programId,
            onSuccess = { doc ->
                if (doc.exists()) {
                    title = doc.getString("title") ?: ""
                    description = doc.getString("description") ?: ""
                    targetAmount = doc.getLong("targetAmount")?.toString() ?: ""
                    imageUrl = doc.getString("imageUrl") ?: ""
                    programType = doc.getString("type") ?: "Masjid"
                }
            },
            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Program Wakaf", fontWeight = FontWeight.Bold) },
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
                // --- PREVIEW GAMBAR ---
                Text("Foto Program", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada gambar", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                // --- FORM INPUT ---
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Program") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("Target Dana (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL Gambar") },
                    placeholder = { Text("https://link-gambar.com/foto.jpg") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.updateProgram(
                            id = programId,
                            title = title,
                            description = description,
                            imageUrl = imageUrl,
                            targetAmountStr = targetAmount,
                            programType = programType,
                            onSuccess = {
                                Toast.makeText(context, "Program diperbarui", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}