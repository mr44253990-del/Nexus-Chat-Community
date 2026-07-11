package com.example.data

import com.example.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    val currentUser get() = auth.currentUser
    
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            PresenceRepository.setOnlineStatus(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, name: String, dob: String, gender: String, profilePicUri: android.net.Uri?): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                var profilePicUrl = ""
                if (profilePicUri != null) {
                    val uploadResult = StorageRepository.uploadProfilePicture(user.uid, profilePicUri)
                    profilePicUrl = uploadResult.getOrDefault("")
                }

                val username = email.substringBefore("@") + (1000..9999).random()
                val newUser = User(
                    uid = user.uid,
                    username = username,
                    name = name,
                    dob = dob,
                    gender = gender,
                    profilePicture = profilePicUrl
                )
                db.collection("users").document(user.uid).set(newUser).await()
                PresenceRepository.setOnlineStatus(true)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        PresenceRepository.setOnlineStatus(false)
        auth.signOut()
    }
}

