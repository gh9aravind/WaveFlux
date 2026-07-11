package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.playback.SoundStudioController
import com.example.ui.MusicViewModel
import com.example.ui.components.VibeAlbumArt
import com.example.ui.theme.SpotifyGreen

@OptIn(ExperimentalMaterial3Api::class)
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
    val playQueue by viewModel.playQueue.collectAsState()

    var showOptionsSheet by remember { mutableStateOf(false) }
    var showSoundStudioSheet by remember { mutableStateOf(false) }
    var showSongDetails by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }

    val track = currentTrack ?: return

    val vibeAccent = remember(track.vibeCode) {
        when (track.vibeCode.lowercase()) {
            "synthwave" -> Color(0xFFE0245E)
            "lofi" -> Color(0xFF3B82F6)
            "acoustic" -> Color(0xFFD97706)
            "classical" -> Color(0xFF8B5CF6)
            "youtube" -> Color(0xFFEF4444)
            else -> SpotifyGreen
        }
    }

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

    val upNextTracks = remember(playQueue, track.id) {
        val idx = playQueue.indexOfFirst { it.id == track.id }
        if (idx == -1) playQueue else playQueue.drop(idx + 1) + playQueue.take(idx)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(vibeGradient)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Top Bar ---
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
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Minimize player",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
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

        Spacer(modifier = Modifier.weight(0.15f))

        // --- 2. Glowing Album Artwork ---
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.96f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(vibeAccent.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )
            VibeAlbumArt(
                vibeCode = track.vibeCode,
                thumbnailUrl = track.thumbnailUrl,
                modifier = Modifier.fillMaxSize(0.92f)
            )
        }

        Spacer(modifier = Modifier.weight(0.12f))

        // --- 3. Track Headings + Options Menu ---
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

            IconButton(
                onClick = { showOptionsSheet = true },
                modifier = Modifier.testTag("player_options_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. Seekbar ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (duration > 0) position.toFloat() else 0f,
                onValueChange = { newValue -> viewModel.seekTo(newValue.toInt()) },
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

        Spacer(modifier = Modifier.weight(0.1f))

        // --- 5. Playback Controls ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }, modifier = Modifier.testTag("shuffle_button")) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Toggle Shuffle",
                    tint = if (shuffleEnabled) SpotifyGreen else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { viewModel.playPrevious() }, modifier = Modifier.testTag("prev_button")) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Song",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(SpotifyGreen, CircleShape)
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
            IconButton(onClick = { viewModel.playNext() }, modifier = Modifier.testTag("next_button")) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Song",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = { viewModel.toggleRepeat() }, modifier = Modifier.testTag("repeat_button")) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Toggle Repeat",
                    tint = if (repeatEnabled) SpotifyGreen else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // --- 6. Up Next Preview ---
        if (upNextTracks.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Text(
                    text = "UP NEXT",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(upNextTracks.take(10)) { upNext ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x0FFFFFFF))
                                .clickable { viewModel.selectAndPlayTrack(upNext, playQueue) }
                                .padding(8.dp)
                        ) {
                            VibeAlbumArt(
                                vibeCode = upNext.vibeCode,
                                thumbnailUrl = upNext.thumbnailUrl,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = upNext.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 130.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // --- Options Bottom Sheet ---
    if (showOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            containerColor = Color(0xFF181818)
        ) {
            TrackOptionRow(Icons.Default.Download, "Download") {
                if (track.isDownloaded) {
                    viewModel.removeTrackDownload(context, track)
                } else {
                    viewModel.startTrackDownload(context, track)
                }
                showOptionsSheet = false
            }
            TrackOptionRow(Icons.Default.PlaylistAdd, "Add to Playlist") {
                showOptionsSheet = false
                showAddToPlaylist = true
            }
            TrackOptionRow(Icons.Default.Info, "Song details") {
                showOptionsSheet = false
                showSongDetails = true
            }
            TrackOptionRow(Icons.Default.Share, "Share song") {
                showOptionsSheet = false
                val shareText = if (track.youtubeVideoId != null) {
                    "${track.title} - ${track.artist}\nhttps://www.youtube.com/watch?v=${track.youtubeVideoId}"
                } else {
                    "${track.title} - ${track.artist}"
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share song"))
            }
            TrackOptionRow(
                icon = Icons.Default.GraphicEq,
                label = "Sound Studio",
                highlighted = true
            ) {
                showOptionsSheet = false
                showSoundStudioSheet = true
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // --- Sound Studio Bottom Sheet ---
    if (showSoundStudioSheet) {
        SoundStudioSheet(onDismiss = { showSoundStudioSheet = false })
    }

    // --- Song Details Dialog ---
    if (showSongDetails) {
        AlertDialog(
            onDismissRequest = { showSongDetails = false },
            title = { Text("Song details") },
            text = {
                Column {
                    DetailRow("Title", track.title)
                    DetailRow("Artist", track.artist)
                    DetailRow("Genre", track.genre)
                    DetailRow("Duration", formatTime(track.durationMs.toInt()))
                }
            },
            confirmButton = {
                TextButton(onClick = { showSongDetails = false }) { Text("Close") }
            }
        )
    }

    // --- Add to Playlist Dialog ---
    if (showAddToPlaylist) {
        val allPlaylists by viewModel.allPlaylists.collectAsState()
        AlertDialog(
            onDismissRequest = { showAddToPlaylist = false },
            title = { Text("Add to Playlist") },
            text = {
                if (allPlaylists.isEmpty()) {
                    Text("No playlists yet. Create one from the Library tab.", color = Color.Gray)
                } else {
                    Column {
                        allPlaylists.forEach { playlist ->
                            Text(
                                text = playlist.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTrackToPlaylist(playlist.id, track)
                                        showAddToPlaylist = false
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylist = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun TrackOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) Color(0x2210B981) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SpotifyGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundStudioSheet(onDismiss: () -> Unit) {
    var bass by remember { mutableStateOf(SoundStudioController.getBass().toFloat()) }
    var treble by remember { mutableStateOf(SoundStudioController.getTreble().toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound Studio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                TextButton(onClick = {
                    SoundStudioController.reset()
                    bass = 0f
                    treble = 50f
                }) {
                    Text("Reset", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Bass", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = bass,
                onValueChange = {
                    bass = it
                    SoundStudioController.setBass(it.toInt())
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = Color.DarkGray,
                    thumbColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("bass_slider")
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Treble", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = treble,
                onValueChange = {
                    treble = it
                    SoundStudioController.setTreble(it.toInt())
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = Color.DarkGray,
                    thumbColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("treble_slider")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

          
