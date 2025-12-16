package com.example.smartmosque.features.donation

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.Timestamp

// --- IMPORT WARNA DARI THEME ---
import com.example.smartmosque.ui.theme.GreenPrimary
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.GrayInputBackground
import com.example.smartmosque.ui.theme.GrayInactive

@Composable
fun AddWaqfProgramScreen(navController: NavController) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") } // Tambahan input gambar
    var targetAmount by remember { mutableStateOf("") }
    var programType by remember { mutableStateOf("Waqf") }

    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = BgPremium, // Background Soft
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 1. CUSTOM HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            ) {
                Surface(
                    onClick = { navController.popBackStack() },
                    shape = CircleShape,
                    color = White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextColorPrimary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Buat Program", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Text("Galang dana untuk umat", fontSize = 12.sp, color = TextColorSecondary)
                }
            }

            // 2. PILIH TIPE PROGRAM
            Column {
                Text("Jenis Program", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColorPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProgramTypeCard(
                        title = "Program Waqf",
                        isSelected = programType == "Waqf",
                        onClick = { programType = "Waqf" },
                        modifier = Modifier.weight(1f)
                    )
                    ProgramTypeCard(
                        title = "Donasi Umum",
                        isSelected = programType == "Donasi",
                        onClick = { programType = "Donasi" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. FORM DETAIL UTAMA (CARD)
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PremiumWaqfInput(
                        value = title,
                        onValueChange = { title = it },
                        label = "Judul Program",
                        placeholder = "Contoh: Renovasi Kubah",
                        icon = Icons.Default.Title
                    )

                    PremiumWaqfInput(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = "Link Gambar (URL)",
                        placeholder = "https://...",
                        icon = Icons.Default.Image
                    )

                    PremiumWaqfInput(
                        value = description,
                        onValueChange = { description = it },
                        label = "Deskripsi Lengkap",
                        placeholder = "Jelaskan tujuan program...",
                        icon = Icons.Default.Description,
                        isMultiLine = true
                    )
                }
            }

            // 4. FORM KEUANGAN (CARD)
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Target Pendanaan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) targetAmount = it },
                        label = { Text("Nominal (Rp)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = EmeraldDeep) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldDeep,
                            unfocusedBorderColor = GrayInactive,
                            focusedLabelColor = EmeraldDeep,
                            cursorColor = EmeraldDeep,
                            focusedContainerColor = White,
                            unfocusedContainerColor = BgPremium.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. TOMBOL SIMPAN
            Button(
                onClick = {
                    // Validasi
                    when {
                        title.isBlank() -> Toast.makeText(context, "Judul wajib diisi", Toast.LENGTH_SHORT).show()
                        description.isBlank() -> Toast.makeText(context, "Deskripsi wajib diisi", Toast.LENGTH_SHORT).show()
                        targetAmount.isBlank() -> Toast.makeText(context, "Target dana wajib diisi", Toast.LENGTH_SHORT).show()
                        else -> {
                            isLoading = true

                            val programData = hashMapOf(
                                "title" to title,
                                "description" to description,
                                "imageUrl" to imageUrl, // Simpan Gambar
                                "targetAmount" to (targetAmount.toLongOrNull() ?: 0L),
                                "collectedAmount" to 0L,
                                "type" to programType,
                                "status" to "active",

                                // PENTING: Timestamp ini yang memicu notifikasi merah di Home
                                "createdAt" to Timestamp.now()
                            )

                            Firebase.firestore.collection("waqf_programs")
                                .add(programData)
                                .addOnSuccessListener {
                                    isLoading = false
                                    Toast.makeText(context, "Program berhasil diterbitkan!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = EmeraldDeep.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Terbitkan Program", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- KOMPONEN UI PREMIUM ---

@Composable
fun ProgramTypeCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) EmeraldDeep else White,
        border = if (!isSelected) BorderStroke(1.dp, GrayInputBackground) else null,
        shadowElevation = if (isSelected) 4.dp else 0.dp,
        modifier = modifier.height(45.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, null, tint = White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) White else TextColorSecondary
            )
        }
    }
}

@Composable
fun PremiumWaqfInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isMultiLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp, color = GrayInactive) },
        leadingIcon = { Icon(icon, null, tint = EmeraldDeep) },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isMultiLine) 120.dp else 60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldDeep,
            unfocusedBorderColor = GrayInactive,
            focusedLabelColor = EmeraldDeep,
            cursorColor = EmeraldDeep,
            focusedContainerColor = White,
            unfocusedContainerColor = BgPremium.copy(alpha = 0.3f)
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        maxLines = if (isMultiLine) 5 else 1
    )
}