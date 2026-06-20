package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.MusicViewModel
import com.example.ui.theme.SpotifyGreen

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xE6120E32)) // Deep space dynamic blue-tint glassmorphism
            .clickable { if (track != null) onExpand() }
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
                        viewModel.seekTo(newValue.toInt())
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
