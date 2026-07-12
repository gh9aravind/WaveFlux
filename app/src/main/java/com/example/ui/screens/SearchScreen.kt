package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlbumResult
import com.example.ui.MusicViewModel
import com.example.ui.components.VibeAlbumArt
import com.example.ui.theme.SpotifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var queryText by remember { mutableStateOf("") }
    val activeTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val results by viewModel.elasticSearchResults.collectAsState()
    val isLoading by viewModel.isElasticSearchLoading.collectAsState()
    val error by viewModel.elasticSearchError.collectAsState()

    val albumResults by viewModel.albumResults.collectAsState()
    val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsState()
    val playlistResults by viewModel.playlistResults.collectAsState()
    val isLoadingPlaylists by viewModel.isLoadingPlaylists.collectAsState()

    val category by viewModel.searchCategory.collectAsState()
    val openAlbum by viewModel.openAlbum.collectAsState()

    LaunchedEffect(queryText) {
        viewModel.performElasticSearch(queryText)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(top = 28.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            placeholder = {
                Text(
                    "What do you want to listen to?",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = Color.LightGray) },
            trailingIcon = {
                if (queryText.isNotEmpty()) {
                    IconButton(onClick = { queryText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search query", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x19FFFFFF),
                unfocusedContainerColor = Color(0x0CFFFFFF),
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = Color(0x1FFFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .testTag("search_field_bar")
        )

        if (queryText.trim().isNotEmpty()) {
            // --- Category Tabs ---
            val categories = listOf("all" to "All", "songs" to "Songs", "albums" to "Albums", "playlists" to "Playlists")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                items(categories) { (key, label) ->
                    val selected = category == key
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (selected) SpotifyGreen else Color(0x0CFFFFFF),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { viewModel.setSearchCategory(key) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("search_tab_$key")
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.Black else Color.White
                        )
                    }
                }
            }

            when (category) {
                "songs" -> SongsResultsList(
                    results = results,
                    isLoading = isLoading,
                    error = error,
                    queryText = queryText,
                    activeTrackId = activeTrack?.id,
                    isPlaying = isPlaying,
                    onTrackClick = { track -> viewModel.selectAndPlayTrack(track, results) },
                    onFavoriteToggle = { track -> viewModel.toggleFavorite(track) },
                    onDownload = { track ->
                        if (track.isDownloaded) viewModel.removeTrackDownload(context, track)
                        else viewModel.startTrackDownload(context, track)
                    }
                )
                "albums" -> AlbumOrPlaylistGrid(
                    items = albumResults,
                    isLoading = isLoadingAlbums,
                    emptyLabel = "No albums found for \"$queryText\"",
                    onClick = { viewModel.openAlbum(it) }
                )
                "playlists" -> AlbumOrPlaylistGrid(
                    items = playlistResults,
                    isLoading = isLoadingPlaylists,
                    emptyLabel = "No playlists found for \"$queryText\"",
                    onClick = { viewModel.openAlbum(it) }
                )
                else -> AllResultsSections(
                    results = results,
                    isLoadingSongs = isLoading,
                    albumResults = albumResults,
                    isLoadingAlbums = isLoadingAlbums,
                    playlistResults = playlistResults,
                    isLoadingPlaylists = isLoadingPlaylists,
                    activeTrackId = activeTrack?.id,
                    isPlaying = isPlaying,
                    onTrackClick = { track -> viewModel.selectAndPlayTrack(track, results) },
                    onFavoriteToggle = { track -> viewModel.toggleFavorite(track) },
                    onDownload = { track ->
                        if (track.isDownloaded) viewModel.removeTrackDownload(context, track)
                        else viewModel.startTrackDownload(context, track)
                    },
                    onAlbumClick = { viewModel.openAlbum(it) },
                    onSeeAllSongs = { viewModel.setSearchCategory("songs") },
                    onSeeAllAlbums = { viewModel.setSearchCategory("albums") },
                    onSeeAllPlaylists = { viewModel.setSearchCategory("playlists") }
                )
            }
        } else {
            Text(
                text = "Browse all",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val browseCategories = listOf(
                Pair("Lofi Chill", Brush.linearGradient(colors = listOf(Color(0xFF8E44AD), Color(0xFF3498DB)))),
                Pair("Synthwave", Brush.linearGradient(colors = listOf(Color(0xFFD35400), Color(0xFFE74C3C)))),
                Pair("Acoustic Folk", Brush.linearGradient(colors = listOf(Color(0xFF16A085), Color(0xFF27AE60)))),
                Pair("Classical Piano", Brush.linearGradient(colors = listOf(Color(0xFF2C3E50), Color(0xFF4A6572)))),
                Pair("New Releases", Brush.linearGradient(colors = listOf(Color(0xFF8A2387), Color(0xFFE94057)))),
                Pair("Podcasts", Brush.linearGradient(colors = listOf(Color(0xFF00B4DB), Color(0xFF0083B0))))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(browseCategories) { (name, brush) ->
                    CategoryBrowseCard(name, brush) {
                        queryText = name.split(" ").first()
                    }
                }
            }
        }
    }

    // --- Album/Playlist Tracklist Sheet (e.g. a full movie's soundtrack) ---
    if (openAlbum != null) {
        AlbumTracklistSheet(
            viewModel = viewModel,
            album = openAlbum!!,
            activeTrackId = activeTrack?.id,
            isPlaying = isPlaying,
            onDismiss = { viewModel.closeAlbum() }
        )
    }
}

@Composable
private fun SongsResultsList(
    results: List<com.example.data.model.Track>,
    isLoading: Boolean,
    error: String?,
    queryText: String,
    activeTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (com.example.data.model.Track) -> Unit,
    onFavoriteToggle: (com.example.data.model.Track) -> Unit,
    onDownload: (com.example.data.model.Track) -> Unit
) {
    when {
        isLoading && results.isEmpty() -> {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SpotifyGreen)
            }
        }
        results.isEmpty() -> {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = error ?: "No results found for \"$queryText\"",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(results) { track ->
                    val isCurrent = activeTrackId == track.id
                    TrackListItem(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        onClick = { onTrackClick(track) },
                        onFavoriteToggle = { onFavoriteToggle(track) },
                        onDownload = { onDownload(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumOrPlaylistGrid(
    items: List<AlbumResult>,
    isLoading: Boolean,
    emptyLabel: String,
    onClick: (AlbumResult) -> Unit
) {
    when {
        isLoading && items.isEmpty() -> {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SpotifyGreen)
            }
        }
        items.isEmpty() -> {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(text = emptyLabel, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(items) { album -> AlbumCard(album) { onClick(album) } }
            }
        }
    }
}

@Composable
private fun AllResultsSections(
    results: List<com.example.data.model.Track>,
    isLoadingSongs: Boolean,
    albumResults: List<AlbumResult>,
    isLoadingAlbums: Boolean,
    playlistResults: List<AlbumResult>,
    isLoadingPlaylists: Boolean,
    activeTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (com.example.data.model.Track) -> Unit,
    onFavoriteToggle: (com.example.data.model.Track) -> Unit,
    onDownload: (com.example.data.model.Track) -> Unit,
    onAlbumClick: (AlbumResult) -> Unit,
    onSeeAllSongs: () -> Unit,
    onSeeAllAlbums: () -> Unit,
    onSeeAllPlaylists: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            SectionHeader(title = "Songs", onSeeAll = if (results.size > 4) onSeeAllSongs else null)
        }
        if (isLoadingSongs && results.isEmpty()) {
            item { LoadingRow() }
        } else if (results.isEmpty()) {
            item { Text("No songs found", color = Color.Gray, fontSize = 13.sp) }
        } else {
            items(results.take(4)) { track ->
                val isCurrent = activeTrackId == track.id
                TrackListItem(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { onTrackClick(track) },
                    onFavoriteToggle = { onFavoriteToggle(track) },
                    onDownload = { onDownload(track) }
                )
            }
        }

        item {
            SectionHeader(title = "Albums", onSeeAll = if (albumResults.size > 4) onSeeAllAlbums else null)
        }
        item {
            if (isLoadingAlbums && albumResults.isEmpty()) {
                LoadingRow()
            } else if (albumResults.isEmpty()) {
                Text("No albums found", color = Color.Gray, fontSize = 13.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(albumResults.take(10)) { album ->
                        AlbumCard(album, modifier = Modifier.width(140.dp)) { onAlbumClick(album) }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Playlists", onSeeAll = if (playlistResults.size > 4) onSeeAllPlaylists else null)
        }
        item {
            if (isLoadingPlaylists && playlistResults.isEmpty()) {
                LoadingRow()
            } else if (playlistResults.isEmpty()) {
                Text("No playlists found", color = Color.Gray, fontSize = 13.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlistResults.take(10)) { playlist ->
                        AlbumCard(playlist, modifier = Modifier.width(140.dp)) { onAlbumClick(playlist) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        if (onSeeAll != null) {
            Text(
                text = "See all",
                fontSize = 12.sp,
                color = SpotifyGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.CenterStart) {
        CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun AlbumCard(
    album: AlbumResult,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier.clickable { onClick() }) {
        VibeAlbumArt(
            vibeCode = "youtube",
            thumbnailUrl = album.thumbnailUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (album.subtitle.isNotBlank()) {
            Text(
                text = album.subtitle,
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Shows the full tracklist of an album/playlist (e.g. every song from a
 * movie's soundtrack) in a bottom sheet. Tapping "Play All" queues the whole
 * tracklist so next/previous walk through the entire album.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumTracklistSheet(
    viewModel: MusicViewModel,
    album: AlbumResult,
    activeTrackId: String?,
    isPlaying: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tracks by viewModel.albumTracks.collectAsState()
    val isLoading by viewModel.isLoadingAlbumTracks.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (album.subtitle.isNotBlank()) {
                        Text(text = album.subtitle, color = Color.Gray, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            if (tracks.isNotEmpty()) {
                Button(
                    onClick = { viewModel.selectAndPlayTrack(tracks.first(), tracks) },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 8.dp)
                        .testTag("play_all_album_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = Color(0x14FFFFFF))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
            tracks.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("No tracks found in this album.", color = Color.Gray)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(tracks) { track ->
                        val isCurrent = activeTrackId == track.id
                        TrackListItem(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onClick = { viewModel.selectAndPlayTrack(track, tracks) },
                            onFavoriteToggle = { viewModel.toggleFavorite(track) },
                            onDownload = {
                                if (track.isDownloaded) viewModel.removeTrackDownload(context, track)
                                else viewModel.startTrackDownload(context, track)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBrowseCard(
    name: String,
    backgroundBrush: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundBrush, alpha = 0.5f)
            .border(BorderStroke(1.dp, Color(0x22FFFFFF)), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
