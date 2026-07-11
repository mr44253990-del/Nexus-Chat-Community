package com.example.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object StorageRepository {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadProfilePicture(uid: String, uri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child("profile_pictures/$uid.jpg")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadVoiceMessage(chatId: String, uri: Uri): Result<String> {
        return try {
            val fileName = UUID.randomUUID().toString() + ".m4a"
            val ref = storage.reference.child("voice_messages/$chatId/$fileName")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
