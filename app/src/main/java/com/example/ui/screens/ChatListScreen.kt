package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.data.UserRepository
import com.example.data.StoryRepository
import com.example.data.StorageRepository
import com.example.models.User
import com.example.models.Chat
import com.example.R
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.LazyColumn
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
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
    val storyRepository = remember { StoryRepository() }
    val chats by chatRepository.getChats().collectAsState(initial = emptyList())
    val stories by storyRepository.getStories().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chats", "Groups")
    
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val filteredChats = when (selectedTab) {
        0 -> chats.filter { !it.isGroup }
        1 -> chats.filter { it.isGroup }
        else -> chats
    }
    
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val userDoc = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                val name = userDoc.getString("name") ?: ""
                val pic = userDoc.getString("profilePicture") ?: ""
                val path = "stories/${auth.currentUser?.uid}_${System.currentTimeMillis()}.jpg"
                val uploadResult = StorageRepository.uploadFile(path, uri)
                if (uploadResult.isSuccess) {
                    storyRepository.uploadStory(uploadResult.getOrDefault(""), name, pic)
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        val selectedMembers = remember { mutableStateListOf<String>() }
        var isCreating by remember { mutableStateOf(false) }
        val userRepository = remember { UserRepository() }
        val allUsers by userRepository.getAllUsers().collectAsState(initial = emptyList())
        val currentUid = auth.currentUser?.uid
        
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create New Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Members", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(allUsers.filter { it.uid != currentUid }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedMembers.contains(user.uid)) selectedMembers.remove(user.uid)
                                        else selectedMembers.add(user.uid)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = user.profilePicture,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(user.name, modifier = Modifier.weight(1f))
                                Checkbox(
                                    checked = selectedMembers.contains(user.uid),
                                    onCheckedChange = null
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            isCreating = true
                            coroutineScope.launch {
                                val groupId = chatRepository.createGroup(groupName, selectedMembers.toList())
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
                Column {
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
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
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
                if (filteredChats.isEmpty()) {
                    Text(
                        text = if (selectedTab == 0) "No chats yet. Search for users to start chatting!" else "No groups yet. Create one to start chatting!",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (selectedTab == 0) {
                            item {
                                LazyRow(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { launcher.launch("image/*") }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                            Text("My Story", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    items(stories) { story ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            AsyncImage(
                                                model = story.userProfilePic.ifBlank { R.drawable.ic_launcher_foreground },
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                                    .padding(2.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(story.userName.substringBefore(" "), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                        items(filteredChats) { chat ->
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
    val userRepository = remember { UserRepository() }
    var chatName by remember { mutableStateOf(chat.groupName ?: "Loading...") }
    var profilePic by remember { mutableStateOf("") }
    var otherUserId by remember { mutableStateOf<String?>(null) }
    
    val timeString = remember(chat.lastMessageTime) {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        if (chat.lastMessageTime > 0) format.format(Date(chat.lastMessageTime)) else ""
    }

    LaunchedEffect(chat.id) {
        if (!chat.isGroup) {
            val uid = chat.participantIds.firstOrNull { it != auth.currentUser?.uid }
            otherUserId = uid
            if (uid != null) {
                db.collection("users").document(uid)
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

    val presence = if (!chat.isGroup && otherUserId != null) {
        userRepository.getUserPresence(otherUserId!!).collectAsState(initial = Pair(false, 0L))
    } else null
    val isOnline = presence?.value?.first ?: false

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(chatName) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
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
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                        .padding(2.dp)
                        .background(Color.Green, shape = CircleShape)
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


