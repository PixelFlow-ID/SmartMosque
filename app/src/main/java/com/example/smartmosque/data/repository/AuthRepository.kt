package com.example.smartmosque.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk mengelola semua operasi autentikasi dan user data
 * Mengikuti Repository Pattern - Single Source of Truth untuk Auth operations
 */
class AuthRepository {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    /**
     * Login dengan email dan password
     */
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid
            if (userId != null) {
                Result.success(userId)
            } else {
                Result.failure(Exception("User ID is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register user baru dengan email, password, dan data profil
     */
    suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<String> {
        return try {
            // 1. Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid 
                ?: return Result.failure(Exception("Failed to create user"))

            // 2. Save user profile to Firestore
            val userProfile = hashMapOf(
                "name" to name,
                "email" to email,
                "phone" to phone,
                "role" to "user", // Default role
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("users")
                .document(userId)
                .set(userProfile)
                .await()

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in dengan Google menggunakan ID Token
     */
    suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val userId = result.user?.uid
            
            if (userId != null) {
                // Check if user exists in Firestore, if not create profile
                val userDoc = firestore.collection("users").document(userId).get().await()
                
                if (!userDoc.exists()) {
                    val userProfile = hashMapOf(
                        "name" to (result.user?.displayName ?: ""),
                        "email" to (result.user?.email ?: ""),
                        "phone" to "",
                        "role" to "user",
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    firestore.collection("users").document(userId).set(userProfile).await()
                }
                
                Result.success(userId)
            } else {
                Result.failure(Exception("User ID is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch user role dari Firestore
     */
    suspend fun fetchUserRole(uid: String): Result<String> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            val role = document.getString("role") ?: "user"
            Result.success(role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch user profile dari Firestore
     */
    suspend fun fetchUserProfile(uid: String): Result<Map<String, Any?>> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                Result.success(document.data ?: emptyMap())
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        auth.signOut()
    }
}
