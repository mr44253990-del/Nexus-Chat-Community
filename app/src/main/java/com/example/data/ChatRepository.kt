package com.example.data

import com.example.models.Chat
import com.example.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val database = com.google.firebase.database.FirebaseDatabase.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getChats(): Flow<List<Chat>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val listener = db.collection("chats")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { it.toObject(Chat::class.java)?.copy(id = it.id) }
                        .sortedByDescending { it.lastMessageTime }
                    trySend(chats)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java)?.copy(id = it.id) }
                    trySend(messages)
                }
            }
            
        awaitClose { listener.remove() }
    }
    
    suspend fun getOrCreateChat(otherUserId: String): String {
        val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val participants = listOf(uid, otherUserId).sorted()
        
        val existingChats = db.collection("chats")
            .whereEqualTo("isGroup", false)
            .whereEqualTo("participantIds", participants)
            .get().await()
            
        if (!existingChats.isEmpty) {
            return existingChats.documents[0].id
        }
        
        val newChatRef = db.collection("chats").document()
        val chat = Chat(
            id = newChatRef.id,
            participantIds = participants,
            isGroup = false,
            lastMessageTime = System.currentTimeMillis()
        )
        newChatRef.set(chat).await()
        return newChatRef.id
    }
    
    suspend fun createGroup(name: String, memberIds: List<String>): String {
        val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val allMembers = (memberIds + uid).distinct()
        
        val newChatRef = db.collection("chats").document()
        val chat = Chat(
            id = newChatRef.id,
            participantIds = allMembers,
            isGroup = true,
            groupName = name,
            adminIds = listOf(uid),
            lastMessageTime = System.currentTimeMillis()
        )
        newChatRef.set(chat).await()
        return newChatRef.id
    }
    
    fun sendMessage(chatId: String, text: String, type: String = "text", mediaUrl: String? = null, replyToId: String? = null, replyToText: String? = null) {
        val uid = auth.currentUser?.uid ?: return
        val messageRef = db.collection("chats").document(chatId).collection("messages").document()
        val message = Message(
            id = messageRef.id,
            senderId = uid,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = type,
            mediaUrl = mediaUrl,
            replyToId = replyToId,
            replyToText = replyToText
        )
        
        db.runBatch { batch ->
            batch.set(messageRef, message)
            val lastMsgText = when (type) {
                "voice" -> "🎙️ Voice message"
                "image" -> "📷 Image"
                else -> text
            }
            batch.update(db.collection("chats").document(chatId), mapOf(
                "lastMessage" to lastMsgText,
                "lastMessageTime" to message.timestamp
            ))
        }

        // Trigger notifications (This typically requires a backend Cloud Function for security)
        // For demonstration, we attempt to find the recipient's token and log it.
        coroutineScope.launch {
            try {
                val chatDoc = db.collection("chats").document(chatId).get().await()
                val participantIds = chatDoc.get("participantIds") as? List<String> ?: emptyList()
                val recipientIds = participantIds.filter { it != uid }
                
                for (recipientId in recipientIds) {
                    val userDoc = db.collection("users").document(recipientId).get().await()
                    val token = userDoc.getString("fcmToken")
                    if (!token.isNullOrBlank()) {
                        // In a real app, you'd call your server or a Cloud Function here to send the FCM message.
                        // sendingNotification(token, "New Message", text)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        try {
            db.collection("chats").document(chatId).collection("messages").document(messageId)
                .update(mapOf(
                    "text" to newText,
                    "isEdited" to true
                )).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun reactToMessage(chatId: String, messageId: String, emoji: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val docRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentReactions = snapshot.get("reactions") as? Map<String, String> ?: emptyMap()
                val updatedReactions = currentReactions.toMutableMap()
                if (updatedReactions[uid] == emoji) {
                    updatedReactions.remove(uid)
                } else {
                    updatedReactions[uid] = emoji
                }
                transaction.update(docRef, "reactions", updatedReactions)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        try {
            db.collection("chats").document(chatId).collection("messages").document(messageId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getTypingStatus(chatId: String): Flow<Map<String, Boolean>> = callbackFlow {
        val ref = database.getReference("chats/$chatId/typing")
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val typingMap = mutableMapOf<String, Boolean>()
                snapshot.children.forEach {
                    val uid = it.key
                    val isTyping = it.getValue(Boolean::class.java)
                    if (uid != null && isTyping != null) {
                        typingMap[uid] = isTyping
                    }
                }
                trySend(typingMap)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
