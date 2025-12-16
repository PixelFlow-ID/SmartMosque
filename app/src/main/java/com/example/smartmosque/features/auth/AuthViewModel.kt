package com.example.smartmosque.features.auth

import android.app.Application
import android.content.Context
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// State Auth
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("smart_mosque_prefs", Context.MODE_PRIVATE)

    // --- PERBAIKAN UTAMA: CEK SESI LOGIN SAAT APLIKASI DIBUKA ---
    private val _authState = MutableStateFlow<AuthState>(
        if (auth.currentUser != null) {
            AuthState.Success(auth.currentUser)
        } else {
            AuthState.Idle
        }
    )
    val authState = _authState.asStateFlow()

    private val _userRole = MutableStateFlow("user")
    val userRole = _userRole.asStateFlow()

    // Variabel currentUser (Realtime Flow)
    val currentUser: StateFlow<FirebaseUser?> = _authState
        .map { state -> (state as? AuthState.Success)?.user ?: auth.currentUser }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), auth.currentUser)

    init {
        // --- LISTENER OTOMATIS ---
        // Memantau jika user login/logout secara realtime
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.value = AuthState.Success(user)
                fetchUserRole(user.uid)
            } else {
                _authState.value = AuthState.Idle
                _userRole.value = "user"
            }
        }
    }

    // ==========================================
    // BAGIAN 1: AUTHENTICATION
    // ==========================================

    private fun fetchUserRole(uid: String) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(uid).get().await()
                val role = document.getString("role") ?: "user"
                _userRole.value = role
            } catch (e: Exception) {
                _userRole.value = "user"
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            // Reset ke loading agar UI merespon klik baru
            _authState.value = AuthState.Loading

            if (email.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Email dan Password tidak boleh kosong")
                return@launch
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _authState.value = AuthState.Error("Format email tidak valid")
                return@launch
            }

            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success(result.user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login Gagal")
            }
        }
    }

    fun register(name: String, email: String, phoneRaw: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            if (name.isBlank() || email.isBlank() || phoneRaw.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Semua data wajib diisi")
                return@launch
            }

            // Validasi di ViewModel juga (Safety Net)
            if (password.length < 6) {
                _authState.value = AuthState.Error("Password minimal 6 karakter")
                return@launch
            }

            val formattedPhone = formatPhoneNumber(phoneRaw)

            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null) {
                    val userData = hashMapOf(
                        "uid" to user.uid,
                        "fullName" to name,
                        "email" to email,
                        "phoneNumber" to formattedPhone,
                        "role" to "user",
                        "createdAt" to System.currentTimeMillis()
                    )

                    firestore.collection("users").document(user.uid).set(userData).await()

                    // Update Nama di Auth Firebase agar langsung muncul tanpa reload
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user.updateProfile(profileUpdates).await()

                    _authState.value = AuthState.Success(user)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registrasi Gagal")
            }
        }
    }

    fun firebaseSignInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null) {
                    val docRef = firestore.collection("users").document(user.uid)
                    val docSnapshot = docRef.get().await()

                    if (!docSnapshot.exists()) {
                        val userData = hashMapOf(
                            "uid" to user.uid,
                            "fullName" to (user.displayName ?: "Jemaah"),
                            "email" to (user.email ?: ""),
                            "phoneNumber" to (user.phoneNumber ?: ""),
                            "photoUrl" to (user.photoUrl?.toString() ?: ""),
                            "role" to "user",
                            "createdAt" to System.currentTimeMillis()
                        )
                        docRef.set(userData).await()
                    }
                    _authState.value = AuthState.Success(user)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In Gagal")
            }
        }
    }

    // ==========================================
    // BAGIAN 2: NOTIFICATION & UTILS
    // ==========================================

    fun isTopicEnabled(topic: String): Boolean {
        return prefs.getBoolean("notif_$topic", true)
    }

    fun toggleNotification(topic: String, isEnabled: Boolean) {
        prefs.edit().putBoolean("notif_$topic", isEnabled).apply()
        if (isEnabled) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        var cleanPhone = phone.trim().replace("-", "").replace(" ", "")
        if (cleanPhone.startsWith("0")) cleanPhone = cleanPhone.substring(1)
        if (!cleanPhone.startsWith("+62")) cleanPhone = "+62$cleanPhone"
        return cleanPhone
    }

    fun logout() {
        auth.signOut()
        // AuthStateListener akan otomatis mengubah state jadi Idle
    }
}