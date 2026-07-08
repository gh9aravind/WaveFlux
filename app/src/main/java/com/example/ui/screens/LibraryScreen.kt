package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaylistWithCount
import com.example.data.model.Track
import com.example.ui.MusicViewModel
import com.example.ui.theme.SpotifyGreen

@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val activeTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsState()

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Favorites, 1: Downloads, 2: Playlists
    var showCreateDialog by remember { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }

    val activeDisplayList = if (selectedSubTab == 0) favoriteTracks else downloadedTracks

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // --- 1. Library Title / Playlist Detail Header ---
        if (selectedPlaylistId != null) {
            val playlist = allPlaylists.find { it.id == selectedPlaylistId }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
            ) {
                IconButton(onClick = { viewModel.closePlaylist() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = playlist?.name ?: "Playlist",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (playlist != null) {
                    IconButton(onClick = { renamingPlaylist = playlist }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename playlist", tint = Color.LightGray)
                    }
                    IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete playlist", tint = Color(0xFFE57373))
                    }
                }
            }

            if (selectedPlaylistTracks.isEmpty()) {
                EmptyState(
                    title = "This playlist is empty",
                    subtitle = "Add songs to it using the + icon on any track.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(selectedPlaylistTracks) { track ->
                        val isCurrent = activeTrack?.id == track.id
                        TrackListItem(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onClick = { viewModel.selectAndPlayTrack(track, selectedPlaylistTracks) },
                            onFavoriteToggle = { viewModel.toggleFavorite(track) },
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
        } else {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
            )

            // --- Tab Selectors ---
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                contentColor = SpotifyGreen,
                divider = {},
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Liked Songs (${favoriteTracks.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    selectedContentColor = SpotifyGreen,
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_favorites")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Downloads (${downloadedTracks.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    selectedContentColor = SpotifyGreen,
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_downloads")
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text("Playlists (${allPlaylists.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    selectedContentColor = SpotifyGreen,
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_playlists")
                )
            }

            // --- Tab Content ---
            if (selectedSubTab == 2) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("create_playlist_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Playlist", fontWeight = FontWeight.Bold)
                }

                if (allPlaylists.isEmpty()) {
                    EmptyState(
                        title = "No playlists yet!",
                        subtitle = "Create one and start adding your favorite tracks.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(allPlaylists) { playlist ->
                            PlaylistRow(
                                playlist = playlist,
                                onClick = { viewModel.openPlaylist(playlist.id) },
                                onRename = { renamingPlaylist = playlist },
                                onDelete = { viewModel.deletePlaylist(playlist.id) }
                            )
                        }
                    }
                }
            } else {
                if (activeDisplayList.isEmpty()) {
                    EmptyState(
                        title = if (selectedSubTab == 0) "Your liked songs list is empty!" else "No downloaded songs!",
                        subtitle = "Enjoy and save tracks from Home or Search."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(activeDisplayList) { track ->
                            val isCurrent = activeTrack?.id == track.id
                            TrackListItemWithPlaylistAction(
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && isPlaying,
                                onClick = { viewModel.selectAndPlayTrack(track, activeDisplayList) },
                                onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                onDownload = {
                                    if (track.isDownloaded) {
                                        viewModel.removeTrackDownload(context, track)
                                    } else {
                                        viewModel.startTrackDownload(context, track)
                                    }
                                },
                                onAddToPlaylist = { addToPlaylistTrack = track }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Create Playlist Dialog ---
    if (showCreateDialog) {
        var nameText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.testTag("new_playlist_name_input")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createPlaylist(nameText)
                    showCreateDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Rename Playlist Dialog ---
    renamingPlaylist?.let { playlist ->
        var nameText by remember(playlist.id) { mutableStateOf(playlist.name) }
        AlertDialog(
            onDismissRequest = { renamingPlaylist = null },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renamePlaylist(playlist.id, nameText)
                    renamingPlaylist = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renamingPlaylist = null }) { Text("Cancel") }
            }
        )
    }

    // --- Add to Playlist Dialog ---
    addToPlaylistTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { addToPlaylistTrack = null },
            title = { Text("Add to Playlist") },
            text = {
                if (allPlaylists.isEmpty()) {
                    Text("No playlists yet. Create one first from the Playlists tab.", color = Color.Gray)
                } else {
                    Column {
                        allPlaylists.forEach { playlist ->
                            Text(
                                text = playlist.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTrackToPlaylist(playlist.id, track)
                                        addToPlaylistTrack = null
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { addToPlaylistTrack = null }) { Text("Close") }
            }
        )
    }
}
@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Empty state icon",
                tint = Color.DarkGray,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun PlaylistRow(
    playlist: PlaylistWithCount,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
        border = BorderStroke(1.dp, Color(0x12FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(playlist.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text("${playlist.trackCount} songs", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun TrackListItemWithPlaylistAction(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            TrackListItem(
                track = track,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                onClick = onClick,
                onFavoriteToggle = onFavoriteToggle,
                onDownload = onDownload
            )
        }
        IconButton(onClick = onAddToPlaylist) {
            Icon(Icons.Default.Add, contentDescription = "Add to playlist", tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}
