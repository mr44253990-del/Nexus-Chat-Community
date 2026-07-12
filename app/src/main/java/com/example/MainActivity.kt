package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.PresenceRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme

import com.example.utils.PreferenceManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val preferenceManager = PreferenceManager(this)
    
    setContent {
      val isDarkMode = remember { mutableStateOf(preferenceManager.isDarkMode) }
      val lifecycleOwner = LocalLifecycleOwner.current
      DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
          when (event) {
            Lifecycle.Event.ON_RESUME -> PresenceRepository.setOnlineStatus(true)
            Lifecycle.Event.ON_STOP -> PresenceRepository.setOnlineStatus(false) // Only set offline when stopped
            else -> {}
          }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
          lifecycleOwner.lifecycle.removeObserver(observer)
        }
      }
      
      MyApplicationTheme(darkTheme = isDarkMode.value) {
        AppNavigation(
          onThemeChange = { 
            preferenceManager.isDarkMode = it
            isDarkMode.value = it
          }
        )
      }
    }
  }
}

