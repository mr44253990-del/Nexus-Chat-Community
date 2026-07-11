package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.core.content.ContextCompat
import com.example.data.ChatRepository
import com.example.data.PresenceRepository
import com.example.data.StorageRepository
import com.example.models.Message
import com.example.utils.AudioRecorder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatName: String,
    onNavigateBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val chatRepository = remember { ChatRepository() }
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val messages by chatRepository.getMessages(chatId).collectAsState(initial = emptyList())
    val typingStatus by chatRepository.getTypingStatus(chatId).collectAsState(initial = emptyMap())
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val audioRecorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
    }

    val isSomeoneTyping = typingStatus.any { it.key != currentUserId && it.value }

    LaunchedEffect(messageText) {
        PresenceRepository.setTyping(chatId, messageText.isNotEmpty())
    }

    DisposableEffect(Unit) {
        onDispose {
            PresenceRepository.setTyping(chatId, false)
            if (isRecording) audioRecorder.stopRecording()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(chatName, fontWeight = FontWeight.Bold)
                        if (isSomeoneTyping) {
                            Text("typing...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(message, currentUserId, chatId)
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (isRecording) "Recording..." else "Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    readOnly = isRecording
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isRecording) Color.Red else MaterialTheme.colorScheme.primary, CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    if (messageText.isBlank()) {
                                        if (hasRecordPermission) {
                                            isRecording = true
                                            audioRecorder.startRecording(chatId)
                                            tryAwaitRelease()
                                            isRecording = false
                                            audioRecorder.stopRecording()
                                            val file = audioRecorder.getAudioFile()
                                            if (file != null && file.exists()) {
                                                coroutineScope.launch {
                                                    val uploadResult = StorageRepository.uploadVoiceMessage(chatId, file.toUri())
                                                    if (uploadResult.isSuccess) {
                                                        chatRepository.sendMessage(chatId, "", type = "voice", mediaUrl = uploadResult.getOrNull())
                                                    }
                                                }
                                            }
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    } else {
                                        // Tap to send text
                                        chatRepository.sendMessage(chatId, messageText)
                                        messageText = ""
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (messageText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(message: Message, currentUserId: String, chatId: String) {
    val isFromMe = message.senderId == currentUserId
    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isFromMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val timeString = remember(message.timestamp) {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        if (message.timestamp > 0) format.format(Date(message.timestamp)) else ""
    }
    
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    val chatRepository = remember { ChatRepository() }
    val coroutineScope = rememberCoroutineScope()

    if (showOptionsDialog && isFromMe) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Message Options") },
            text = { Text("Do you want to delete this message?") },
            confirmButton = {
                TextButton(onClick = {
                    showOptionsDialog = false
                    coroutineScope.launch {
                        chatRepository.deleteMessage(chatId, message.id)
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptionsDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isFromMe) 16.dp else 4.dp,
                        bottomEnd = if (isFromMe) 4.dp else 16.dp
                    ))
                    .background(backgroundColor)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (isFromMe) showOptionsDialog = true
                        }
                    )
                    .padding(12.dp)
            ) {
                if (message.type == "voice" && message.mediaUrl != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (!isPlaying) {
                                    isPlaying = true
                                    val mediaPlayer = MediaPlayer().apply {
                                        setDataSource(message.mediaUrl)
                                        prepareAsync()
                                        setOnPreparedListener { start() }
                                        setOnCompletionListener { 
                                            isPlaying = false
                                            release()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voice Message", color = textColor)
                    }
                } else {
                    Text(
                        text = message.text,
                        color = textColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

