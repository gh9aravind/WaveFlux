package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MusicViewModel
import com.example.ui.components.AudioVisualizerWave
import com.example.ui.components.VibeAlbumArt
import com.example.ui.theme.SpotifyGreen

@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.trackDuration.collectAsState()
    val shuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatEnabled by viewModel.isRepeatEnabled.collectAsState()

    val track = currentTrack ?: return

    // Linear dynamic background gradient matching track vibe
    val vibeGradient = remember(track.vibeCode) {
        val colorStart = when (track.vibeCode.lowercase()) {
            "synthwave" -> Color(0xFF1B031E)
            "lofi" -> Color(0xFF030D1B)
            "acoustic" -> Color(0xFF1F1003)
            "classical" -> Color(0xFF030C12)
            else -> Color(0xFF010A03)
        }
        Brush.verticalGradient(colors = listOf(colorStart, Color(0xFF040605)))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(vibeGradient)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Navigation Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onCollapse() },
                modifier = Modifier.testTag("collapse_player_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize SoundSpot Deck",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = Color.LightGray
            )
            IconButton(
                onClick = { viewModel.toggleFavorite(track) },
                modifier = Modifier.testTag("fav_player_toggle")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like active song",
                    tint = if (track.isFavorite) SpotifyGreen else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // --- 2. Floating Album Artwork Cover ---
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally)
        ) {
            VibeAlbumArt(
                vibeCode = track.vibeCode,
                thumbnailUrl = track.thumbnailUrl,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // --- 3. Track Headings & Offline Controls ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    fontSize = 15.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Download Trigger on the player deck!
            IconButton(
                onClick = {
                    if (track.isDownloaded) {
                        viewModel.removeTrackDownload(context, track)
                    } else {
                        viewModel.startTrackDownload(context, track)
                    }
                },
                modifier = Modifier.testTag("download_player_button")
            ) {
                Icon(
                    imageVector = if (track.isDownloaded) Icons.Default.OfflinePin else Icons.Default.DownloadForOffline,
                    contentDescription = "Save track offline",
                    tint = if (track.isDownloaded) SpotifyGreen else Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 4. Interactive Seekbar/Scrubber ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (duration > 0) position.toFloat() else 0f,
                onValueChange = { newValue ->
                    viewModel.seekTo(newValue.toInt())
                },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                colors = SliderDefaults.colors(
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = Color.DarkGray,
                    thumbColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_scrubber_slider")
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(position), fontSize = 12.sp, color = Color.Gray)
                Text(text = formatTime(duration), fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.weight(0.08f))

        // --- 5. Bouncing Soundwave Canvas ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            AudioVisualizerWave(isPlaying = isPlaying)
        }

        Spacer(modifier = Modifier.weight(0.08f))

        // --- 6. Music Control Array Grid ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = { viewModel.toggleShuffle() },
                modifier = Modifier.testTag("shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Toggle Shuffle",
                    tint = if (shuffleEnabled) SpotifyGreen else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(
                onClick = { viewModel.playPrevious() },
                modifier = Modifier.testTag("prev_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Song",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Central Play/Pause Capsule
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(SpotifyGreen, CircleShape) // Emerald 500 colored dynamic play circle
                    .clickable { viewModel.togglePlayPause() }
                    .testTag("player_center_play_pause"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play or Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Next
            IconButton(
                onClick = { viewModel.playNext() },
                modifier = Modifier.testTag("next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Song",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = { viewModel.toggleRepeat() },
                modifier = Modifier.testTag("repeat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Toggle Repeat",
                    tint = if (repeatEnabled) SpotifyGreen else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.12f))
    }
}

// Helper to convert elapsed milliseconds safely to "MM:SS"
private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
