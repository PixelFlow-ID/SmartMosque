package com.example.smartmosque.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartmosque.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// --- IMPORT WARNA TEMA (Agar Konsisten) ---
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.GrayInputBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // --- STATE DATA ---
    var name by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var phoneNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") } // "Laki-laki" atau "Perempuan"

    var isLoading by remember { mutableStateOf(false) }
    var isDataLoaded by remember { mutableStateOf(false) }

    // --- LOAD DATA DARI FIRESTORE SAAT PERTAMA BUKA ---
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Jika data ada di database, pakai data database (lebih lengkap)
                        name = document.getString("fullName") ?: name
                        phoneNumber = document.getString("phoneNumber") ?: ""
                        gender = document.getString("gender") ?: ""
                    }
                    isDataLoaded = true
                }
                .addOnFailureListener {
                    isDataLoaded = true // Tetap load meski gagal (pakai data auth)
                }
        }
    }

    Scaffold(
        containerColor = BgPremium, // Background Soft Premium
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profil", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BgPremium,
                    titleContentColor = TextColorPrimary
                )
            )
        }
    ) { padding ->
        if (!isDataLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldDeep)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 1. AVATAR / FOTO PROFIL (Placeholder)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(10.dp, CircleShape, spotColor = EmeraldDeep.copy(alpha = 0.2f))
                        .clip(CircleShape)
                        .background(White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDeep
                    )
                    // Badge Edit Foto (Hiasan)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .background(EmeraldDeep, CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = White, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 2. FORM DATA (Card Putih)
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

                        // Input Nama
                        EditProfileTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Nama Lengkap",
                            icon = Icons.Outlined.Person
                        )

                        // Input Email (Read Only - Disable)
                        EditProfileTextField(
                            value = email,
                            onValueChange = {},
                            label = "Email (Tidak dapat diubah)",
                            icon = Icons.Outlined.Email,
                            enabled = false
                        )

                        // Input No HP
                        EditProfileTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.all { char -> char.isDigit() }) phoneNumber = it },
                            label = "Nomor WhatsApp",
                            icon = Icons.Outlined.Phone,
                            keyboardType = KeyboardType.Phone
                        )

                        // Input Gender (Pilihan)
                        Column {
                            Text("Jenis Kelamin", fontSize = 12.sp, color = TextColorSecondary, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                GenderOption(
                                    label = "Laki-laki",
                                    isSelected = gender == "Laki-laki",
                                    onClick = { gender = "Laki-laki" },
                                    modifier = Modifier.weight(1f)
                                )
                                GenderOption(
                                    label = "Perempuan",
                                    isSelected = gender == "Perempuan",
                                    onClick = { gender = "Perempuan" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 3. TOMBOL SIMPAN
                Button(
                    onClick = {
                        if (currentUser != null && name.isNotBlank()) {
                            isLoading = true

                            // A. Update Auth (Display Name)
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                            currentUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // B. Update Firestore (Simpan data lengkap)
                                    val userData = hashMapOf(
                                        "fullName" to name,
                                        "email" to email,
                                        "phoneNumber" to phoneNumber,
                                        "gender" to gender,
                                        "updatedAt" to com.google.firebase.Timestamp.now()
                                    )

                                    firestore.collection("users").document(currentUser.uid)
                                        .set(userData, SetOptions.merge()) // Merge agar data lain tidak hilang
                                        .addOnSuccessListener {
                                            isLoading = false
                                            Toast.makeText(context, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            Toast.makeText(context, "Gagal simpan database: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Gagal update auth: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = EmeraldDeep.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                    enabled = !isLoading && name.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Simpan Perubahan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = White)
                    }
                }
            }
        }
    }
}

// --- KOMPONEN PENDUKUNG ---

@Composable
fun EditProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, tint = if(enabled) EmeraldDeep else Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldDeep,
            unfocusedBorderColor = GrayInputBackground, // Border halus saat tidak fokus
            focusedLabelColor = EmeraldDeep,
            cursorColor = EmeraldDeep,
            focusedContainerColor = White,
            unfocusedContainerColor = if(enabled) BgPremium.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.2f),
            disabledTextColor = Color.Gray,
            disabledBorderColor = Color.LightGray.copy(alpha = 0.5f)
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = keyboardType
        )
    )
}

@Composable
fun GenderOption(
    label: String,
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
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) White else TextColorSecondary
            )
        }
    }
}
