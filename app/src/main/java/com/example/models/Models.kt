package com.example.models

data class User(
    val uid: String = "",
    val username: String = "",
    val name: String = "",
    val dob: String = "",
    val gender: String = "",
    val profilePicture: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val isReaction: String? = null,
    val type: String = "text",
    val mediaUrl: String? = null
)

data class Chat(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val groupPhoto: String? = null,
    val groupBackground: String? = null,
    val adminIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val typingUsers: Map<String, Boolean> = emptyMap()
)
