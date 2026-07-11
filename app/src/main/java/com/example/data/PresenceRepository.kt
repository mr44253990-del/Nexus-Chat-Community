package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

object PresenceRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun setupPresenceSystem() {
        val uid = auth.currentUser?.uid ?: return
        val userStatusDatabaseRef = database.getReference("status/$uid")
        
        val isOfflineForDatabase = mapOf(
            "online" to false,
            "lastSeen" to ServerValue.TIMESTAMP
        )
        val isOnlineForDatabase = mapOf(
            "online" to true,
            "lastSeen" to ServerValue.TIMESTAMP
        )
        
        database.getReference(".info/connected").addValueEventListener(
            object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        userStatusDatabaseRef.onDisconnect().setValue(isOfflineForDatabase).addOnCompleteListener {
                            userStatusDatabaseRef.setValue(isOnlineForDatabase)
                        }
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
        )
    }

    fun setOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val userStatusDatabaseRef = database.getReference("status/$uid")
        
        if (isOnline) {
            userStatusDatabaseRef.setValue(mapOf(
                "online" to true,
                "lastSeen" to ServerValue.TIMESTAMP
            ))
        } else {
            userStatusDatabaseRef.setValue(mapOf(
                "online" to false,
                "lastSeen" to ServerValue.TIMESTAMP
            ))
        }
    }
    
    fun setTyping(chatId: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val typingRef = database.getReference("chats/$chatId/typing/$uid")
        if (isTyping) {
            typingRef.setValue(true)
        } else {
            typingRef.removeValue()
        }
    }
}
