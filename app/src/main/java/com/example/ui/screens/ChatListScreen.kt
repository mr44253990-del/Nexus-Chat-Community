package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ChatRepository
import com.example.models.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.graphics.Color
import com.example.ui.components.GlassBackground
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val chatRepository = remember { ChatRepository() }
    val chats by chatRepository.getChats().collectAsState(initial = emptyList())
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var isCreating by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create New Group") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            isCreating = true
                            coroutineScope.launch {
                                val groupId = chatRepository.createGroup(groupName, emptyList())
                                showCreateGroupDialog = false
                                onNavigateToChat(groupId, groupName)
                            }
                        }
                    },
                    enabled = !isCreating && groupName.isNotBlank()
                ) {
                    if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    else Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("EBChat", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search Users")
                        }
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { showCreateGroupDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "New Group")
                    }
                    FloatingActionButton(
                        onClick = onNavigateToSearch,
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat")
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (chats.isEmpty()) {
                    Text(
                        text = "No chats yet. Search for users to start chatting!",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chats) { chat ->
                            ChatListItem(
                                chat = chat,
                                onClick = { name -> onNavigateToChat(chat.id, name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(chat: Chat, onClick: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var chatName by remember { mutableStateOf(chat.groupName ?: "Loading...") }
    var profilePic by remember { mutableStateOf("") }
    val timeString = remember(chat.lastMessageTime) {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        if (chat.lastMessageTime > 0) format.format(Date(chat.lastMessageTime)) else ""
    }

    LaunchedEffect(chat.id) {
        if (!chat.isGroup) {
            val otherUserId = chat.participantIds.firstOrNull { it != auth.currentUser?.uid }
            if (otherUserId != null) {
                db.collection("users").document(otherUserId)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null && snapshot.exists()) {
                            chatName = snapshot.getString("name") ?: "Unknown User"
                            profilePic = snapshot.getString("profilePicture") ?: ""
                        }
                    }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(chatName) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (chat.isGroup) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            if (profilePic.isNotBlank() && !chat.isGroup) {
                AsyncImage(
                    model = profilePic,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = rememberVectorPainter(image = if (chat.isGroup) Icons.Default.GroupAdd else Icons.Default.Person),
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = if (chat.isGroup) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = chatName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage.takeIf { it.isNotEmpty() } ?: "No messages yet", 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        
        Text(
            text = timeString,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


