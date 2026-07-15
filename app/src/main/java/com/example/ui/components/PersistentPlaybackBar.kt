package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.MusicViewModel
import com.example.ui.theme.SpotifyGreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PersistentPlaybackBar(
    viewModel: MusicViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.trackDuration.collectAsState()
    
    val track = currentTrack

    // Swiping the bar upward expands the player (like tapping it). Swiping
    // it downward drags it off screen with a volume fade, and releasing past
    // the threshold stops playback entirely - releasing short of it snaps
    // the bar (and volume) back to normal.
    val density = LocalDensity.current
    val expandDragThresholdPx = with(density) { 24.dp.toPx() }
    val dismissDistancePx = with(density) { 90.dp.toPx() }
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    var dragAccumulator by remember { mutableStateOf(0f) }

    val dragState = rememberDraggableState { delta ->
        dragAccumulator += delta
        if (delta > 0f) {
            // Only downward movement visually pulls the bar down + fades
            // volume - an upward drag just accumulates toward the expand
            // threshold, since PlayerScreen already slides up on its own.
            scope.launch {
                val newOffset = (offsetY.value + delta).coerceIn(0f, dismissDistancePx * 1.4f)
                offsetY.snapTo(newOffset)
                if (track != null) {
                    viewModel.setPlaybackVolume(1f - (newOffset / dismissDistancePx).coerceIn(0f, 1f))
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .graphicsLayer { alpha = 1f - (offsetY.value / dismissDistancePx).coerceIn(0f, 1f) }
            .background(Color(0xE6120E32)) // Deep space dynamic blue-tint glassmorphism
            .clickable { if (track != null) onExpand() }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    when {
                        track != null && (offsetY.value > dismissDistancePx * 0.4f || velocity > 800f) -> {
                            // Dragged (or flung) down past the threshold: finish
                            // sliding it away, stop the song, then reset so the
                            // bar is back in place for the next track.
                            scope.launch {
                                offsetY.animateTo(dismissDistancePx * 1.4f, animationSpec = tween(150))
                                viewModel.stopAndClearPlayback()
                                offsetY.snapTo(0f)
                            }
                        }
                        dragAccumulator < -expandDragThresholdPx || velocity < -600f -> {
                            scope.launch {
                                offsetY.animateTo(0f, animationSpec = tween(150))
                                viewModel.setPlaybackVolume(1f)
                            }
                            if (track != null) onExpand()
                        }
                        else -> {
                            scope.launch {
                                offsetY.animateTo(0f, animationSpec = tween(150))
                                viewModel.setPlaybackVolume(1f)
                            }
                        }
                    }
                    dragAccumulator = 0f
                }
            )
            .testTag("persistent_playback_bar")
    ) {
        // Sleek separation line at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x1BFFFFFF))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (track != null) {
                // Left Part: Track Info (Album Art & Text Descriptions)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    VibeAlbumArt(
                        vibeCode = track.vibeCode,
                        thumbnailUrl = track.thumbnailUrl,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = track.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Part: Playback Control Decks
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Like Toggle
                    IconButton(
                        onClick = { viewModel.toggleFavorite(track) },
                        modifier = Modifier.size(38.dp).testTag("persistent_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save track to library",
                            tint = if (track.isFavorite) SpotifyGreen else Color.LightGray,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Previous Track Button
                    IconButton(
                        onClick = { viewModel.playPrevious() },
                        modifier = Modifier.size(38.dp).testTag("persistent_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Skip to preceeding audio catalog entry",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play & Pause State Toggle
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(42.dp).testTag("persistent_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Play pause toggle control",
                            tint = SpotifyGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next Track Button
                    IconButton(
                        onClick = { viewModel.playNext() },
                        modifier = Modifier.size(38.dp).testTag("persistent_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip to succeeding audio catalog entry",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Persistent Idle visual container when no tracks are loaded from the Room DB Cache yet
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x16FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Active Track Placeholder",
                                tint = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ready to Play",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Tap a song to experience AI DJ",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Quick start play trigger running default cache loaded list
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(42.dp).testTag("persistent_play_pause_button_idle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Play first playlist catalog entry",
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Persistent Scrubber / Seek Bar (Only rendered when there's an active track selected)
        if (track != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentSec = position / 1000
                val totalSec = duration / 1000
                
                Text(
                    text = String.format("%d:%02d", currentSec / 60, currentSec % 60),
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier.width(32.dp)
                )

                Slider(
                    value = if (duration > 0) position.toFloat() else 0f,
                    onValueChange = { newValue ->
                        // Never let a stray tap seek into the last 1.5s of a
                        // track - that was causing accidental taps near the
                        // end of this bar to immediately trigger "song ended"
                        // and skip to the next track.
                        val safeMax = (duration - 1500).coerceAtLeast(0)
                        viewModel.seekTo(newValue.toInt().coerceAtMost(safeMax))
                    },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                    colors = SliderDefaults.colors(
                        activeTrackColor = SpotifyGreen,
                        inactiveTrackColor = Color(0x28FFFFFF),
                        thumbColor = SpotifyGreen
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .testTag("persistent_seek_slider")
                )

                Text(
                    text = String.format("%d:%02d", totalSec / 60, totalSec % 60),
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
