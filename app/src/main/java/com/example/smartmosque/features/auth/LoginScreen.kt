package com.example.smartmosque.features.auth

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmosque.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

// IMPORT WARNA DARI THEME (Konsisten)
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.EmeraldLight
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.GrayInputBackground
import com.example.smartmosque.ui.theme.Screen
import androidx.navigation.NavController

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onRegisterClick: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onGoogleSignInClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Monitor auth state untuk navigasi otomatis
    val authState by authViewModel.authState.collectAsState()

    // State Tab (True = Masuk, False = Daftar)
    var isLoginTab by remember { mutableStateOf(true) }

    // State Input
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    
    // --- NAVIGASI OTOMATIS SETELAH LOGIN SUKSES ---
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            is AuthState.Error -> {
                val errorMessage = (authState as AuthState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            else -> { /* Idle atau Loading */ }
        }
    }

    // --- SETUP GOOGLE SIGN IN WITH ACCOUNT SELECTION ---
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    onGoogleSignInClick(idToken)
                } else {
                    Toast.makeText(context, "Gagal Login Google", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("GoogleLogin", "Error: ${e.statusCode}")
            }
        }
    }

    // BACKGROUND GRADIENT
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgPremium, White),
                    startY = 0f,
                    endY = 1500f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. LOGO & HEADER
            Surface(
                shape = CircleShape,
                color = EmeraldDeep,
                shadowElevation = 10.dp,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Mosque,
                        contentDescription = "Logo",
                        tint = White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Smart Mosque",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextColorPrimary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Masjid Agung Manonjaya",
                fontSize = 14.sp,
                color = TextColorSecondary
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 2. CARD UTAMA (FORM)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // TAB SWITCHER (PILL STYLE)
                    Surface(
                        color = GrayInputBackground,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(50.dp).fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            TabButton(
                                text = "Masuk",
                                isSelected = isLoginTab,
                                modifier = Modifier.weight(1f),
                                onClick = { isLoginTab = true }
                            )
                            TabButton(
                                text = "Daftar",
                                isSelected = !isLoginTab,
                                modifier = Modifier.weight(1f),
                                onClick = { isLoginTab = false }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // INPUT FIELDS
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Nama (Hanya saat Daftar)
                        AnimatedVisibility(
                            visible = !isLoginTab,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PremiumAuthInput(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = "Nama Lengkap",
                                icon = Icons.Outlined.Person,
                                capitalization = KeyboardCapitalization.Words
                            )
                        }

                        // Email
                        PremiumAuthInput(
                            value = email,
                            onValueChange = { email = it },
                            label = "Alamat Email",
                            icon = Icons.Outlined.Email,
                            keyboardType = KeyboardType.Email
                        )

                        // No HP (Hanya saat Daftar)
                        AnimatedVisibility(
                            visible = !isLoginTab,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PremiumAuthInput(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = "Nomor WhatsApp",
                                icon = Icons.Outlined.Phone,
                                keyboardType = KeyboardType.Phone
                            )
                        }

                        // Password
                        PremiumAuthInput(
                            value = password,
                            onValueChange = { password = it },
                            label = "Kata Sandi",
                            icon = Icons.Outlined.Lock,
                            isPassword = true,
                            isPasswordVisible = isPasswordVisible,
                            onVisibilityChange = { isPasswordVisible = !isPasswordVisible }
                        )

                        // Confirm Password (Hanya saat Daftar)
                        AnimatedVisibility(
                            visible = !isLoginTab,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PremiumAuthInput(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = "Ulangi Kata Sandi",
                                icon = Icons.Outlined.Lock,
                                isPassword = true,
                                isPasswordVisible = isConfirmPasswordVisible,
                                onVisibilityChange = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                                imeAction = ImeAction.Done
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // TOMBOL LOGIN/DAFTAR
                    Button(
                        onClick = {
                            if (isLoginTab) {
                                onLoginClick(email, password)
                            } else {
                                onRegisterClick(fullName, email, phoneNumber, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text(
                            text = if (isLoginTab) "Masuk Sekarang" else "Buat Akun",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // DIVIDER
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = GrayInputBackground)
                        Text(
                            text = "atau lanjutkan dengan",
                            fontSize = 12.sp,
                            color = TextColorSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = GrayInputBackground)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // GOOGLE BUTTON - With Account Selection
                    OutlinedButton(
                        onClick = {
                            try {
                                // Sign out silently to force account picker to show
                                // This allows users to choose between multiple Google accounts
                                googleSignInClient.signOut().addOnCompleteListener {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                }
                            } catch (e: Exception) {
                                Log.e("GoogleLogin", "Error: ${e.message}")
                                Toast.makeText(context, "Gagal membuka Google Sign-In", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = White)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Google",
                            color = TextColorPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = if (isLoginTab) "Belum punya akun? Daftar dulu yuk!" else "Sudah punya akun? Silakan masuk.",
                fontSize = 12.sp,
                color = TextColorSecondary
            )
        }
    }
}

// --- KOMPONEN UI PREMIUM ---

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) White else Color.Transparent)
            .clickable { onClick() }
            .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(50))
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) EmeraldDeep else TextColorSecondary
        )
    }
}

@Composable
fun PremiumAuthInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onVisibilityChange: () -> Unit = {},
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = {
            Icon(icon, null, tint = EmeraldDeep.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onVisibilityChange) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = TextColorSecondary
                    )
                }
            }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldDeep,
            unfocusedBorderColor = Color.Transparent, // Border hilang saat tidak fokus (Soft Look)
            focusedLabelColor = EmeraldDeep,
            cursorColor = EmeraldDeep,
            focusedTextColor = TextColorPrimary,
            unfocusedTextColor = TextColorPrimary,
            focusedContainerColor = White,
            unfocusedContainerColor = GrayInputBackground.copy(alpha = 0.5f) // Abu sangat muda
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = imeAction
        ),
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None
    )
}