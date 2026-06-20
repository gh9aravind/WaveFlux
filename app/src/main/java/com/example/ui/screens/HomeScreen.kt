package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.MusicViewModel
import com.example.ui.components.VibeAlbumArt
import com.example.ui.theme.SpotifyGreen
import java.util.Calendar


@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTracks by viewModel.allTracks.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val activeTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Determine greetings dynamically
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp)
    ) {
        // --- 1. Dynamic Welcome Header ---
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "Ready for some beautiful soundtracks?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        // --- 2. Featured Vibe Stations Grid ---
        item {
            Column {
                Text(
                    text = "Vibe Stations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val vibeStations = listOf(
                    Triple("Synthwave", "synthwave", "Retro grids & neon suns"),
                    Triple("Cozy Lofi", "lofi", "Relaxing late night study beats"),
                    Triple("Acoustic Road", "acoustic", "Wooden sunsets & campfires"),
                    Triple("Classical", "classical", "Misty evening piano whispers")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        vibeStations.take(2).forEach { (title, code, desc) ->
                            VibeStationCard(title, code, desc) {
                                val filtered = allTracks.filter { it.vibeCode == code }
                                if (filtered.isNotEmpty()) {
                                    viewModel.selectAndPlayTrack(filtered.first(), filtered)
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        vibeStations.takeLast(2).forEach { (title, code, desc) ->
                            VibeStationCard(title, code, desc) {
                                val filtered = allTracks.filter { it.vibeCode == code }
                                if (filtered.isNotEmpty()) {
                                    viewModel.selectAndPlayTrack(filtered.first(), filtered)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Recently Played Section ---
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentlyPlayed) { track ->
                            RecentTrackCard(track) {
                                viewModel.selectAndPlayTrack(track, recentlyPlayed)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Main Songs Library List ---
        item {
            Text(
                text = "Explore Track Deck",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(allTracks) { track ->
            val isCurrent = activeTrack?.id == track.id
            TrackListItem(
                track = track,
                isCurrent = isCurrent,
                isPlaying = isCurrent && isPlaying,
                onClick = {
                    viewModel.selectAndPlayTrack(track, allTracks)
                },
                onFavoriteToggle = {
                    viewModel.toggleFavorite(track)
                },
                onDownload = {
                    if (track.isDownloaded) {
                        viewModel.removeTrackDownload(context, track)
                    } else {
                        viewModel.startTrackDownload(context, track)
                    }
                }
            )
        }
    }
}

@Composable
fun VibeStationCard(
    title: String,
    vibeCode: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)), // bg-white/5 transparent glass
        border = BorderStroke(1.dp, Color(0x12FFFFFF)), // border-white/5 thin glass stroke
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() }
            .testTag("station_${vibeCode}")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VibeAlbumArt(
                vibeCode = vibeCode,
                modifier = Modifier
                    .size(72.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecentTrackCard(
    track: Track,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        VibeAlbumArt(
            vibeCode = track.vibeCode,
            thumbnailUrl = track.thumbnailUrl,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = track.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            fontSize = 10.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrackListItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            VibeAlbumArt(
                vibeCode = track.vibeCode,
                thumbnailUrl = track.thumbnailUrl,
                modifier = Modifier.size(52.dp)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isCurrent) SpotifyGreen else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (track.isDownloaded) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF283B2F), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "OFFLINE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpotifyGreen
                            )
                        }
                    }
                    Text(
                        text = track.artist,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Controls indicators
            if (isCurrent && isPlaying) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Playing",
                    tint = SpotifyGreen,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(horizontal = 4.dp)
                )
            }

            // Favorite Icon Button
            IconButton(
                onClick = { onFavoriteToggle() },
                modifier = Modifier.testTag("fav_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite status",
                    tint = if (track.isFavorite) SpotifyGreen else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Download Icon Button
            IconButton(
                onClick = { onDownload() },
                modifier = Modifier.testTag("dl_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download track",
                    tint = if (track.isDownloaded) SpotifyGreen else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
