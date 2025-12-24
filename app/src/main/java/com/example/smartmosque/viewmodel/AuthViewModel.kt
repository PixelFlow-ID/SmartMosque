package com.example.smartmosque.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmosque.data.repository.AuthRepository
import com.example.smartmosque.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel untuk mengelola Authentication State
 * Mengikuti MVVM Pattern - ViewModel hanya mengelola UI state dan delegate ke Repository
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // Repositories
    private val authRepository = AuthRepository()
    private val notificationRepository = NotificationRepository(application)
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // ==================== STATE MANAGEMENT ====================
    
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

    // ==================== AUTHENTICATION OPERATIONS ====================
    
    /**
     * Fetch user role dari repository
     */
    private fun fetchUserRole(uid: String) {
        viewModelScope.launch {
            authRepository.fetchUserRole(uid)
                .onSuccess { role -> _userRole.value = role }
                .onFailure { _userRole.value = "user" }
        }
    }

    /**
     * Login dengan email dan password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            // Validasi input
            if (email.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Email dan Password wajib diisi")
                return@launch
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _authState.value = AuthState.Error("Format email salah")
                return@launch
            }
            
            // Call repository
            authRepository.login(email, password)
                .onSuccess { userId ->
                    _authState.value = AuthState.Success(auth.currentUser)
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Login Gagal")
                }
        }
    }

    /**
     * Register user baru
     */
    fun register(name: String, email: String, phoneRaw: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            // Validasi input
            if (name.isBlank() || email.isBlank() || phoneRaw.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Semua data wajib diisi")
                return@launch
            }
            if (password.length < 6) {
                _authState.value = AuthState.Error("Password minimal 6 karakter")
                return@launch
            }

            val formattedPhone = formatPhoneNumber(phoneRaw)

            // Call repository
            authRepository.register(name, email, formattedPhone, password)
                .onSuccess { userId ->
                    // Update display name
                    auth.currentUser?.let { user ->
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build()
                        user.updateProfile(profileUpdates)
                    }
                    _authState.value = AuthState.Success(auth.currentUser)
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Registrasi Gagal")
                }
        }
    }

    /**
     * Sign in dengan Google
     */
    fun firebaseSignInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            // Call repository
            authRepository.signInWithGoogle(idToken)
                .onSuccess { userId ->
                    _authState.value = AuthState.Success(auth.currentUser)
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Google Sign-In Gagal")
                }
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        authRepository.logout()
    }

    // ==================== NOTIFICATION OPERATIONS ====================
    
    /**
     * Load notification preferences
     */
    private fun loadNotificationPreferences() {
        _notifSettings.value = notificationRepository.loadNotificationPreferences()
    }

    /**
     * Toggle notification untuk topic tertentu
     */
    fun toggleNotification(topic: String, isEnabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.toggleNotification(topic, isEnabled)
                .onSuccess {
                    val currentMap = _notifSettings.value.toMutableMap()
                    currentMap[topic] = isEnabled
                    _notifSettings.value = currentMap
                }
                .onFailure { exception ->
                    // Handle error if needed
                }
        }
    }

    /**
     * Check if topic is enabled
     */
    fun isTopicEnabled(topic: String): Boolean {
        return notificationRepository.isTopicEnabled(topic)
    }

    // ==================== UTILITY FUNCTIONS ====================
    
    /**
     * Format nomor telepon ke format internasional
     */
    private fun formatPhoneNumber(phone: String): String {
        var cleanPhone = phone.trim().replace("-", "").replace(" ", "")
        if (cleanPhone.startsWith("0")) cleanPhone = cleanPhone.substring(1)
        if (!cleanPhone.startsWith("+62")) cleanPhone = "+62$cleanPhone"
        return cleanPhone
    }
}
