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

    // State Auth
    private val _authState = MutableStateFlow<AuthState>(
        if (auth.currentUser != null) AuthState.Success(auth.currentUser) else AuthState.Idle
    )
    val authState = _authState.asStateFlow()

    private val _userRole = MutableStateFlow("user")
    val userRole = _userRole.asStateFlow()

    // State Notifikasi
    private val _notifSettings = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val notifSettings: StateFlow<Map<String, Boolean>> = _notifSettings.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = _authState
        .map { state -> (state as? AuthState.Success)?.user ?: auth.currentUser }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), auth.currentUser)

    init {
        // 1. Listener Login/Logout (HANYA SATU KALI)
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

        // 2. Load Notifikasi
        loadNotificationPreferences()
    }

    // --- LOGIC AUTH ---
    private fun fetchUserRole(uid: String) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(uid).get().await()
                _userRole.value = document.getString("role") ?: "user"
            } catch (e: Exception) {
                _userRole.value = "user"
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (email.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Email dan Password wajib diisi")
                return@launch
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _authState.value = AuthState.Error("Format email salah")
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

                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name).build()
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
                    if (!docRef.get().await().exists()) {
                        val userData = hashMapOf(
                            "uid" to user.uid,
                            "fullName" to (user.displayName ?: "Jemaah"),
                            "email" to (user.email ?: ""),
                            "phoneNumber" to (user.phoneNumber ?: ""),
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

    // --- LOGIC NOTIFIKASI ---
    private fun loadNotificationPreferences() {
        val events = prefs.getBoolean("notif_events", true)
        val donations = prefs.getBoolean("notif_donations", true)
        _notifSettings.value = mapOf("events" to events, "donations" to donations)
    }

    fun toggleNotification(topic: String, isEnabled: Boolean) {
        prefs.edit().putBoolean("notif_$topic", isEnabled).apply()
        if (isEnabled) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
        }
        val currentMap = _notifSettings.value.toMutableMap()
        currentMap[topic] = isEnabled
        _notifSettings.value = currentMap
    }

    fun isTopicEnabled(topic: String) = prefs.getBoolean("notif_$topic", true)

    private fun formatPhoneNumber(phone: String): String {
        var cleanPhone = phone.trim().replace("-", "").replace(" ", "")
        if (cleanPhone.startsWith("0")) cleanPhone = cleanPhone.substring(1)
        if (!cleanPhone.startsWith("+62")) cleanPhone = "+62$cleanPhone"
        return cleanPhone
    }

    fun logout() {
        auth.signOut()
    }
}