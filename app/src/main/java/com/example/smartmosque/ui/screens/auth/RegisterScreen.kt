package com.example.smartmosque.ui.screens.auth

import android.app.Activity
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
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
import androidx.navigation.NavController
import com.example.smartmosque.R
import com.example.smartmosque.ui.theme.Screen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.smartmosque.viewmodel.AuthViewModel
import com.example.smartmosque.viewmodel.AuthState

// IMPORT WARNA DARI THEME (KONSISTEN)
import com.example.smartmosque.ui.theme.EmeraldDeep
import com.example.smartmosque.ui.theme.BgPremium
import com.example.smartmosque.ui.theme.White
import com.example.smartmosque.ui.theme.TextColorPrimary
import com.example.smartmosque.ui.theme.TextColorSecondary
import com.example.smartmosque.ui.theme.GrayInputBackground
import com.example.smartmosque.ui.theme.RedError

@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current

    // State Form Input
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // State Error
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Pantau Status Auth
    val authState by authViewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading

    // Efek Samping (Navigasi saat Sukses)
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Alhamdulillah, Akun Berhasil Dibuat!", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                authViewModel.logout()
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    // Setup Google Sign In
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
                    authViewModel.firebaseSignInWithGoogle(idToken)
                } else {
                    Toast.makeText(context, "ID Token Kosong", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("GoogleLogin", "Error Code: ${e.statusCode}")
            }
        }
    }

    // BACKGROUND GRADIENT (Sama seperti Login)
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
                .padding(top = 40.dp, bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. LOGO HEADER
            Surface(
                shape = CircleShape,
                color = EmeraldDeep,
                shadowElevation = 10.dp,
                modifier = Modifier.size(70.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Mosque,
                        contentDescription = "Logo",
                        tint = White,
                        modifier = Modifier.size(35.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Bergabung Bersama Kami", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextColorPrimary)
            Text("Masjid Agung Manonjaya", fontSize = 14.sp, color = TextColorSecondary)

            Spacer(modifier = Modifier.height(24.dp))

            // 2. CARD UTAMA
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
                            // Tombol Masuk (Pindah Screen)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .clickable { navController.navigate(Screen.Login.route) { popUpTo(Screen.Register.route) { inclusive = true } } }
                            ) {
                                Text("Masuk", fontSize = 14.sp, color = TextColorSecondary, fontWeight = FontWeight.Medium)
                            }

                            // Tombol Daftar (Aktif)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .shadow(2.dp, RoundedCornerShape(50))
                                    .background(White, RoundedCornerShape(50))
                            ) {
                                Text("Daftar", fontSize = 14.sp, color = EmeraldDeep, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // INPUT FIELDS
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PremiumRegisterInput(
                            label = "Nama Lengkap",
                            value = fullName,
                            onValueChange = { fullName = it; nameError = null },
                            icon = Icons.Outlined.Person,
                            errorMessage = nameError,
                            capitalization = KeyboardCapitalization.Words
                        )

                        PremiumRegisterInput(
                            label = "Alamat Email",
                            value = email,
                            onValueChange = { email = it; emailError = null },
                            icon = Icons.Outlined.Email,
                            keyboardType = KeyboardType.Email,
                            errorMessage = emailError
                        )

                        PremiumRegisterInput(
                            label = "Nomor WhatsApp",
                            value = phoneNumber,
                            onValueChange = { if (it.all { c -> c.isDigit() }) { phoneNumber = it; phoneError = null } },
                            icon = Icons.Outlined.Phone,
                            keyboardType = KeyboardType.Phone,
                            errorMessage = phoneError
                        )

                        PremiumRegisterInput(
                            label = "Kata Sandi",
                            value = password,
                            onValueChange = { password = it; passwordError = null },
                            icon = Icons.Outlined.Lock,
                            isPassword = true,
                            isPasswordVisible = isPasswordVisible,
                            onVisibilityChange = { isPasswordVisible = !isPasswordVisible },
                            errorMessage = passwordError
                        )

                        PremiumRegisterInput(
                            label = "Ulangi Kata Sandi",
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; confirmPasswordError = null },
                            icon = Icons.Outlined.Lock,
                            imeAction = ImeAction.Done,
                            isPassword = true,
                            isPasswordVisible = isConfirmPasswordVisible,
                            onVisibilityChange = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                            errorMessage = confirmPasswordError
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // TOMBOL DAFTAR
                    Button(
                        onClick = {
                            // Validasi Logic
                            nameError = null; emailError = null; phoneError = null; passwordError = null; confirmPasswordError = null
                            var isValid = true

                            if (fullName.isBlank()) { nameError = "Nama wajib diisi"; isValid = false }
                            if (email.isBlank()) { emailError = "Email wajib diisi"; isValid = false }
                            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailError = "Format email salah"; isValid = false }

                            if (phoneNumber.isBlank()) { phoneError = "Nomor wajib diisi"; isValid = false }
                            else if (phoneNumber.length < 10) { phoneError = "Nomor tidak valid"; isValid = false }

                            if (password.length < 6) { passwordError = "Min. 6 karakter"; isValid = false }
                            if (confirmPassword != password) { confirmPasswordError = "Password tidak sama"; isValid = false }

                            if (isValid) authViewModel.register(fullName, email, phoneNumber, password)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Buat Akun", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // GOOGLE & DIVIDER
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = GrayInputBackground)
                        Text("atau daftar dengan", fontSize = 12.sp, color = TextColorSecondary, modifier = Modifier.padding(horizontal = 12.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = GrayInputBackground)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                }
                            } catch (e: Exception) {
                                Log.e("Google", "Error: ${e.message}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = White)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_google), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Google", color = TextColorPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "Dengan mendaftar, Anda menyetujui Ketentuan Layanan kami",
                fontSize = 11.sp,
                color = EmeraldDeep,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}

// --- TEXT FIELD PREMIUM (Soft Look) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumRegisterInput(
    label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector?,
    keyboardType: KeyboardType = KeyboardType.Text, imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isPassword: Boolean = false, isPasswordVisible: Boolean = false, onVisibilityChange: () -> Unit = {},
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            leadingIcon = if (icon != null) {
                { Icon(icon, null, tint = EmeraldDeep.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
            } else null,
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = onVisibilityChange) {
                        Icon(if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = TextColorSecondary)
                    }
                } else if (errorMessage != null) {
                    Icon(Icons.Filled.Error, null, tint = RedError)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = errorMessage != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldDeep,
                unfocusedBorderColor = Color.Transparent, // Border hilang saat idle (Soft)
                errorBorderColor = RedError,

                focusedLabelColor = EmeraldDeep,
                cursorColor = EmeraldDeep,

                focusedTextColor = TextColorPrimary,
                unfocusedTextColor = TextColorPrimary,

                focusedContainerColor = White,
                unfocusedContainerColor = GrayInputBackground.copy(alpha = 0.5f), // Abu sangat muda
                errorContainerColor = RedError.copy(alpha = 0.05f)
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, capitalization = capitalization),
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None
        )

        // Error Message Kecil di Bawah
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = RedError,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}
