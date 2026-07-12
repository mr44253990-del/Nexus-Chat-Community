package com.example.data

import com.example.models.Story
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getStories(): Flow<List<Story>> = callbackFlow {
        val now = System.currentTimeMillis()
        val listener = db.collection("stories")
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val stories = snapshot.documents.mapNotNull { it.toObject(Story::class.java)?.copy(id = it.id) }
                    trySend(stories)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun uploadStory(imageUrl: String, userName: String, profilePic: String) {
        val uid = auth.currentUser?.uid ?: return
        val id = db.collection("stories").document().id
        val now = System.currentTimeMillis()
        val story = Story(
            id = id,
            userId = uid,
            userName = userName,
            userProfilePic = profilePic,
            imageUrl = imageUrl,
            timestamp = now,
            expiresAt = now + (24 * 60 * 60 * 1000) // 24 hours
        )
        db.collection("stories").document(id).set(story).await()
    }
}
