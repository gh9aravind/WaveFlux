package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    var showUpNextSheet by remember { mutableStateOf(false) }

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

    val upNextTracks = remember(playQueue, track.id) {
        val idx = playQueue.indexOfFirst { it.id == track.id }
        if (idx == -1) playQueue else playQueue.drop(idx + 1) + playQueue.take(idx)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedBubbleBackground(seedColor = vibeAccent, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
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

            // --- 2. Album Artwork ---
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                VibeAlbumArt(
                    vibeCode = track.vibeCode,
                    thumbnailUrl = track.thumbnailUrl,
                    modifier = Modifier.fillMaxSize()
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
                        color = Color.LightGray,
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
                        inactiveTrackColor = Color(0x40FFFFFF),
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
                    Text(text = formatTime(position), fontSize = 12.sp, color = Color.LightGray)
                    Text(text = formatTime(duration), fontSize = 12.sp, color = Color.LightGray)
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

            // --- 6. Up Next Handle Bar (tap arrow or swipe up to expand) ---
            if (upNextTracks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showUpNextSheet = true }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                if (dragAmount < -12f) {
                                    showUpNextSheet = true
                                }
                                change.consume()
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UP NEXT",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = Color.LightGray
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Show up next queue",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .testTag("up_next_expand_button")
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
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

    // --- Up Next Full Queue Sheet ---
    if (showUpNextSheet) {
        UpNextSheet(
            viewModel = viewModel,
            queue = playQueue,
            currentTrackId = track.id,
            onDismiss = { showUpNextSheet = false }
        )
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

/**
 * Apple Music style animated background: a few large, soft radial-gradient
 * "bubbles" that slowly drift and shift hue over time, layered above a near
 * black base and darkened with a scrim so text stays readable. Deliberately
 * avoids Modifier.blur() (requires API 31+) - the soft gradient falloff
 * gives a comparable blurred look on all supported devices.
 */
@Composable
private fun AnimatedBubbleBackground(seedColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")

    val hueShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(26000, easing = LinearEasing)),
        label = "hueShift"
    )
    val drift1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(15000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "drift1"
    )
    val drift2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(19000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "drift2"
    )
    val drift3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(12000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "drift3"
    )

    val baseHue = remember(seedColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (seedColor.red * 255).toInt(),
            (seedColor.green * 255).toInt(),
            (seedColor.blue * 255).toInt(),
            hsv
        )
        hsv[0]
    }

    fun bubbleColor(phase: Float): Color {
        val hue = (baseHue + hueShift + phase).mod(360f)
        return Color.hsv(hue, 0.55f, 0.75f)
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(Color(0xFF050505))

        val c1 = bubbleColor(0f)
        val c2 = bubbleColor(110f)
        val c3 = bubbleColor(220f)

        drawCircle(
            brush = Brush.radialGradient(colors = listOf(c1.copy(alpha = 0.55f), Color.Transparent)),
            radius = w * 0.6f,
            center = Offset(w * (0.15f + drift1 * 0.5f), h * (0.12f + drift2 * 0.18f))
        )
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(c2.copy(alpha = 0.5f), Color.Transparent)),
            radius = w * 0.65f,
            center = Offset(w * (0.85f - drift2 * 0.5f), h * (0.38f + drift3 * 0.28f))
        )
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(c3.copy(alpha = 0.45f), Color.Transparent)),
            radius = w * 0.55f,
            center = Offset(w * (0.35f + drift3 * 0.4f), h * (0.8f - drift1 * 0.22f))
        )

        drawRect(Color.Black.copy(alpha = 0.4f))
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

/**
 * Full-height queue sheet. Tap-and-hold the drag handle on the right of any
 * row to reorder it within the queue; tap the X to remove a track from the
 * queue entirely. Changes are written back to the ViewModel's queue as they
 * happen (and once more on dismiss, as a safety net).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpNextSheet(
    viewModel: MusicViewModel,
    queue: List<Track>,
    currentTrackId: String?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val items = remember(queue) { mutableStateListOf<Track>().apply { addAll(queue) } }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 68.dp.toPx() }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    fun commitQueue() {
        viewModel.reorderQueue(items.toList())
    }

    ModalBottomSheet(
        onDismissRequest = {
            commitQueue()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color(0xFF141414)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Up Next", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = {
                commitQueue()
                onDismiss()
            }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        HorizontalDivider(color = Color(0x14FFFFFF))

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(bottom = 16.dp)
        ) {
            itemsIndexed(items, key = { _, t -> t.id }) { index, track ->
                val isBeingDragged = draggedIndex == index
                val elevation by animateDpAsState(if (isBeingDragged) 6.dp else 0.dp, label = "queueItemElevation")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = if (isBeingDragged) dragOffset else 0f
                            shadowElevation = elevation.toPx()
                        }
                        .background(if (track.id == currentTrackId) Color(0x1A10B981) else Color.Transparent)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    VibeAlbumArt(
                        vibeCode = track.vibeCode,
                        thumbnailUrl = track.thumbnailUrl,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {
                        items.removeAt(index)
                        commitQueue()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove from queue",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            .pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedIndex = index
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        draggedIndex = null
                                        dragOffset = 0f
                                        commitQueue()
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        val current = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                        val targetIndex = (current + (dragOffset / itemHeightPx).toInt())
                                            .coerceIn(0, items.lastIndex)
                                        if (targetIndex != current) {
                                            val moved = items.removeAt(current)
                                            items.add(targetIndex, moved)
                                            dragOffset -= (targetIndex - current) * itemHeightPx
                                            draggedIndex = targetIndex
                                        }
                                    }
                                )
                            }
                    )
                }
                HorizontalDivider(color = Color(0x0FFFFFFF))
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
