package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DarkBackground

@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Draw the background glow layers precisely representing the tailwind blur circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val minDim = size.minDimension

            // 1. Top-Left Indigo Mesh Glow: w-70%, h-50%, bg-indigo-600/20, blur
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x384F46E5), // Indigo-600 with 22% alpha
                        Color.Transparent
                    ),
                    center = Offset(width * -0.05f, height * -0.05f),
                    radius = minDim * 0.75f
                ),
                size = this.size
            )

            // 2. Bottom-Right Emerald Mesh Glow: w-60%, h-40%, bg-emerald-500/15, blur
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2B10B981), // Emerald-500 with 17% alpha
                        Color.Transparent
                    ),
                    center = Offset(width * 1.05f, height * 0.85f),
                    radius = minDim * 0.7f
                ),
                size = this.size
            )
        }

        // Render screen contents on top
        content()
    }
}
