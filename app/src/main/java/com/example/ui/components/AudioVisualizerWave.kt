package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.SpotifyGreen
import kotlin.math.sin

@Composable
fun AudioVisualizerWave(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Elegant infinite animation parameter to drive fluctuating bar heights
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_infinite")
    val animationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = 20
        val width = size.width
        val height = size.height
        val spacing = 6f
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (width - totalSpacing) / barCount

        for (i in 0 until barCount) {
            // Compute animated ratio
            val phaseFactor = i * 0.35f
            val baseSin = sin(animationPhase + phaseFactor)
            val normalHeight = if (isPlaying) {
                // Fluctuates between 20% and 90% of total height
                (0.2f + 0.7f * (0.5f + 0.5f * baseSin)) * height
            } else {
                // Tiny elegant idle flat line
                height * 0.08f
            }

            val x = i * (barWidth + spacing)
            val y = height - normalHeight

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(SpotifyGreen, Color(0xFF2BED80))
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, normalHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
