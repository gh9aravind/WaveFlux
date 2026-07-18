package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlin.math.sin

@Composable
fun VibeAlbumArt(
    vibeCode: String,
    thumbnailUrl: String? = null,
    modifier: Modifier = Modifier
) {
    // If we have a real YouTube thumbnail, just show it directly.
    if (!thumbnailUrl.isNullOrBlank()) {
        // "hqdefault" (and sddefault) are YouTube's auto-generated, fixed
        // 4:3-canvas thumbnails - for any video that isn't natively 4:3,
        // YouTube pads them with letterbox/pillarbox bars baked right into
        // the image pixels, which ContentScale.Crop can't remove. The
        // uploader's own "maxresdefault" thumbnail is usually the real,
        // un-padded artwork, so prefer that - but it isn't guaranteed to
        // exist for every video, so fall back to hqdefault if it 404s.
        val preferredUrl = remember(thumbnailUrl) {
            if (thumbnailUrl.contains("hqdefault.jpg")) {
                thumbnailUrl.replace("hqdefault.jpg", "maxresdefault.jpg")
            } else {
                thumbnailUrl
            }
        }
        var currentUrl by remember(thumbnailUrl) { mutableStateOf(preferredUrl) }

        AsyncImage(
            model = currentUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onState = { state ->
                if (state is AsyncImagePainter.State.Error && currentUrl != thumbnailUrl) {
                    currentUrl = thumbnailUrl
                }
            },
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        )
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (vibeCode.lowercase()) {
            "youtube" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1F1F1F), Color(0xFF0A0A0A))
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFFF0000).copy(alpha = 0.85f)
                )
            }
            "synthwave" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Synthwave sunset gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                        )
                    )
                    
                    // Grid lines
                    val width = size.width
                    val height = size.height
                    val centerY = height * 0.6f
                    
                    clipRect {
                        // Horizon line
                        drawLine(
                            color = Color(0xFF00FFFF).copy(alpha = 0.5f),
                            start = Offset(0f, centerY),
                            end = Offset(width, centerY),
                            strokeWidth = 2f
                        )
                        
                        // Perspective grid lines
                        val verticalLines = 12
                        for (i in 0..verticalLines) {
                            val xSource = width * (i.toFloat() / verticalLines)
                            drawLine(
                                color = Color(0xFF00FFFF).copy(alpha = 0.3f),
                                start = Offset(xSource, centerY),
                                end = Offset(width * 0.5f + (xSource - width * 0.5f) * 4f, height),
                                strokeWidth = 1.5f
                            )
                        }
                        
                        // Horizontal compressor lines
                        val horizLines = 6
                        for (i in 1..horizLines) {
                            val ratio = i.toFloat() / horizLines
                            val y = centerY + (height - centerY) * (ratio * ratio) // log spacing for depth
                            drawLine(
                                color = Color(0xFF00FFFF).copy(alpha = 0.3f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.5f
                            )
                        }
                    }
                    
                    // Sun circle
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFEE32), Color(0xFFF12711))
                        ),
                        radius = width * 0.22f,
                        center = Offset(width * 0.5f, centerY)
                    )
                }
            }
            "lofi" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Midnight starry gradient
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2C3E50), Color(0xFF0F2027)),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.width * 0.8f
                        )
                    )
                    
                    // Ambient moon or glow
                    drawCircle(
                        color = Color(0xFFF5D6C6).copy(alpha = 0.7f),
                        radius = size.width * 0.25f,
                        center = Offset(size.width * 0.7f, size.height * 0.3f)
                    )
                    
                    // Cozy star sparklings
                    val stars = listOf(
                        Offset(0.2f, 0.2f), Offset(0.4f, 0.15f), Offset(0.15f, 0.45f),
                        Offset(0.85f, 0.15f), Offset(0.55f, 0.35f), Offset(0.3f, 0.7f),
                        Offset(0.75f, 0.65f), Offset(0.9f, 0.45f)
                    )
                    stars.forEach { percent ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = 3f,
                            center = Offset(size.width * percent.x, size.height * percent.y)
                        )
                    }
                    
                    // Minimalistic plant silhouette shape or curve
                    val width = size.width
                    val height = size.height
                    drawCircle(
                        color = Color(0xFF16A085).copy(alpha = 0.4f),
                        radius = width * 0.35f,
                        center = Offset(0f, height)
                    )
                    drawCircle(
                        color = Color(0xFF512DA8).copy(alpha = 0.5f),
                        radius = width * 0.22f,
                        center = Offset(width * 0.2f, height * 0.9f)
                    )
                }
            }
            "acoustic" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Warm cabin bonfire/sunlight vibes
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF39C12), Color(0xFFD35400), Color(0xFF2C3E50))
                        )
                    )
                    // Intersecting warm light beams
                    drawCircle(
                        color = Color(0xFFFFF9E6).copy(alpha = 0.2f),
                        radius = size.width * 0.5f,
                        center = Offset(size.width * 0.2f, size.height * 0.2f)
                    )
                    drawCircle(
                        color = Color(0xFF5C3A21).copy(alpha = 0.6f),
                        radius = size.width * 0.32f,
                        center = Offset(size.width * 0.5f, size.height * 0.8f)
                    )
                    drawCircle(
                        color = Color(0xFF7B3F00).copy(alpha = 0.4f),
                        radius = size.width * 0.18f,
                        center = Offset(size.width * 0.5f, size.height * 0.8f)
                    )
                }
            }
            "classical" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Misty neoclassical royal sapphire/ivory mist
                    drawRect(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color(0xFF1F3C4D), Color(0xFF122C34), Color(0xFF4A5859), Color(0xFF1F3C4D)),
                            center = Offset(size.width * 0.5f, size.height * 0.5f)
                        )
                    )
                    // Structural elegant geometry
                    val width = size.width
                    val height = size.height
                    
                    drawLine(
                        color = Color(0xFFF4F1DE).copy(alpha = 0.3f),
                        start = Offset(width * 0.1f, 0f),
                        end = Offset(width * 0.1f, height),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color(0xFFF4F1DE).copy(alpha = 0.3f),
                        start = Offset(width * 0.2f, 0f),
                        end = Offset(width * 0.2f, height),
                        strokeWidth = 3f
                    )
                    
                    clipRect {
                        for (i in 0..10) {
                            val offset = i * (height * 0.08f)
                            drawCircle(
                                color = Color(0xFFE07A5F).copy(alpha = 0.15f),
                                radius = width * (0.15f + i * 0.04f),
                                center = Offset(width * 0.8f, offset)
                            )
                        }
                    }
                }
            }
            else -> {
                // Procedural hashed cover art for AI tracks based on title string content!
                val hashValue = vibeCode.hashCode()
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r1 = (hashValue and 0xFF0000) shr 16
                    val g1 = (hashValue and 0x00FF00) shr 8
                    val b1 = (hashValue and 0x0000FF)
                    val color1 = Color(r1, g1, b1)

                    val h2 = hashValue * 31
                    val r2 = (h2 and 0xFF0000) shr 16
                    val g2 = (h2 and 0x00FF00) shr 8
                    val b2 = (h2 and 0x0000FF)
                    val color2 = Color(r2, g2, b2)

                    val h3 = hashValue * 67
                    val r3 = (h3 and 0xFF0000) shr 16
                    val g3 = (h3 and 0x00FF00) shr 8
                    val b3 = (h3 and 0x0000FF)
                    val color3 = Color(r3, g3, b3)
                    
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(color1, color2, color3)
                        )
                    )
                    
                    // Render cool geometric waveform overlays
                    val width = size.width
                    val height = size.height
                    val points = 30
                    for (i in 0 until points - 1) {
                        val x1 = width * (i.toFloat() / points)
                        val x2 = width * ((i + 1).toFloat() / points)
                        val y1 = height * 0.5f + sin(i.toFloat() * 0.5f) * height * 0.15f
                        val y2 = height * 0.5f + sin((i + 1).toFloat() * 0.5f) * height * 0.15f
                        
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 4f
                        )
                    }
                }
            }
        }
        
        // Add a nice subtle central note decoration for branding
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
