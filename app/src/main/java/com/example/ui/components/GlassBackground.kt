package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFB6C1).copy(alpha = 0.2f),
                        Color(0xFFFF1493).copy(alpha = 0.1f),
                        Color(0xFFE0B0FF).copy(alpha = 0.15f)
                    )
                )
            )
    ) {
        content()
    }
}
