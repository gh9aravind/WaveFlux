package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlbumResult
import com.example.data.model.Track
import com.example.ui.MusicViewModel
import com.example.ui.components.VibeAlbumArt
import com.example.ui.theme.SpotifyGreen

/**
 * Landing search tab: search field + a "Browse all" grid. Tapping the field
 * animates the browse grid away and reveals suggestions instead — 5 song
 * suggestions when the field is empty, or up to 3 matching-text suggestions
 * while typing. Submitting (Enter / search action / tapping a suggestion)
 * opens the dedicated [SearchResultsScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var queryText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    val trending by viewModel.trendingTracks.collectAsState()
    val recent by viewModel.recentlyPlayed.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()

    val songSuggestions = remember(trending, recent) {
        (if (recent.isNotEmpty()) recent else trending).take(5)
    }
    val typeSuggestions = remember(queryText, trending, recent, allTracks) {
        if (queryText.isBlank()) {
            emptyList()
        } else {
            (allTracks + recent + trending)
                .filter { it.title.contains(queryText, ignoreCase = true) }
                .distinctBy { it.title.lowercase() }
                .take(3)
        }
    }

    fun submit(text: String = queryText) {
        if (text.trim().isNotEmpty()) {
            focusManager.clearFocus()
            viewModel.submitSearch(text)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
            .padding(horizontal = 16.dp)
    ) {
        // "Search" title collapses away once the field is focused, so the
        // search bar visually rises to the top of the screen.
        AnimatedVisibility(
            visible = !isFocused,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = "Search",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(top = 28.dp, bottom = 20.dp)
            )
        }

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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
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
                .padding(top = 8.dp, bottom = 14.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .testTag("search_field_bar")
        )

        AnimatedVisibility(
            visible = isFocused,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (queryText.isBlank()) {
                    Text(
                        text = "Suggested songs",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    songSuggestions.forEach { track ->
                        SuggestionRow(
                            title = track.title,
                            subtitle = track.artist,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.selectAndPlayTrack(track, songSuggestions)
                            }
                        )
                    }
                } else {
                    typeSuggestions.forEach { track ->
                        SuggestionRow(
                            title = track.title,
                            subtitle = track.artist,
                            leadingIcon = Icons.Default.Search,
                            onClick = { submit(track.title) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isFocused,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(browseCategories) { (name, brush) ->
                        CategoryBrowseCard(name, brush) {
                            val q = name.split(" ").first()
                            queryText = q
                            submit(q)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.PlayArrow
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(leadingIcon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Dedicated full-screen results view, opened once a search is submitted
 * (Enter / keyboard search action / tapping a browse card) rather than
 * shown inline under the search field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var queryText by remember { mutableStateOf(viewModel.activeSearchQuery.value ?: "") }
    val activeTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val results by viewModel.elasticSearchResults.collectAsState()
    val isLoading by viewModel.isElasticSearchLoading.collectAsState()
    val error by viewModel.elasticSearchError.collectAsState()

    val albumResults by viewModel.albumResults.collectAsState()
    val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsState()
    val playlistResults by viewModel.playlistResults.collectAsState()
    val isLoadingPlaylists by viewModel.isLoadingPlaylists.collectAsState()
    val videoResults by viewModel.videoResults.collectAsState()
    val isLoadingVideos by viewModel.isLoadingVideos.collectAsState()

    val category by viewModel.searchCategory.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("search_results_back_button")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = queryText,
                onValueChange = {
                    queryText = it
                    viewModel.performElasticSearch(it)
                },
                placeholder = {
                    Text("What do you want to listen to?", color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = Color.LightGray) },
                trailingIcon = {
                    if (queryText.isNotEmpty()) {
                        IconButton(onClick = {
                            queryText = ""
                            viewModel.performElasticSearch("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search query", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch(queryText) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x19FFFFFF),
                    unfocusedContainerColor = Color(0x0CFFFFFF),
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = Color(0x1FFFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_results_field_bar")
            )
        }

        // --- Category Tabs ---
        val categories = listOf("all" to "All", "songs" to "Songs", "videos" to "Videos", "albums" to "Albums", "playlists" to "Playlists")
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
            "videos" -> SongsResultsList(
                results = videoResults,
                isLoading = isLoadingVideos,
                error = null,
                queryText = queryText,
                activeTrackId = activeTrack?.id,
                isPlaying = isPlaying,
                onTrackClick = { track -> viewModel.selectAndPlayTrack(track, videoResults) },
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
                videoResults = videoResults,
                isLoadingVideos = isLoadingVideos,
                albumResults = albumResults,
                isLoadingAlbums = isLoadingAlbums,
                playlistResults = playlistResults,
                isLoadingPlaylists = isLoadingPlaylists,
                activeTrackId = activeTrack?.id,
                isPlaying = isPlaying,
                onTrackClick = { track -> viewModel.selectAndPlayTrack(track, results) },
                onVideoClick = { track -> viewModel.selectAndPlayTrack(track, videoResults) },
                onFavoriteToggle = { track -> viewModel.toggleFavorite(track) },
                onDownload = { track ->
                    if (track.isDownloaded) viewModel.removeTrackDownload(context, track)
                    else viewModel.startTrackDownload(context, track)
                },
                onAlbumClick = { viewModel.openAlbum(it) },
                onSeeAllSongs = { viewModel.setSearchCategory("songs") },
                onSeeAllVideos = { viewModel.setSearchCategory("videos") },
                onSeeAllAlbums = { viewModel.setSearchCategory("albums") },
                onSeeAllPlaylists = { viewModel.setSearchCategory("playlists") }
            )
        }
    }
}

@Composable
private fun ColumnScope.SongsResultsList(
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
private fun ColumnScope.AlbumOrPlaylistGrid(
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
private fun ColumnScope.AllResultsSections(
    results: List<com.example.data.model.Track>,
    isLoadingSongs: Boolean,
    videoResults: List<com.example.data.model.Track>,
    isLoadingVideos: Boolean,
    albumResults: List<AlbumResult>,
    isLoadingAlbums: Boolean,
    playlistResults: List<AlbumResult>,
    isLoadingPlaylists: Boolean,
    activeTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (com.example.data.model.Track) -> Unit,
    onVideoClick: (com.example.data.model.Track) -> Unit,
    onFavoriteToggle: (com.example.data.model.Track) -> Unit,
    onDownload: (com.example.data.model.Track) -> Unit,
    onAlbumClick: (AlbumResult) -> Unit,
    onSeeAllSongs: () -> Unit,
    onSeeAllVideos: () -> Unit,
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
            SectionHeader(title = "Videos", onSeeAll = if (videoResults.size > 3) onSeeAllVideos else null)
        }
        if (isLoadingVideos && videoResults.isEmpty()) {
            item { LoadingRow() }
        } else if (videoResults.isEmpty()) {
            item { Text("No video songs found", color = Color.Gray, fontSize = 13.sp) }
        } else {
            items(videoResults.take(3)) { track ->
                val isCurrent = activeTrackId == track.id
                TrackListItem(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { onVideoClick(track) },
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
 * movie's soundtrack) as its own dedicated screen, rather than a bottom
 * sheet. Tapping "Play All" queues the whole tracklist so next/previous
 * walk through the entire album.
 */
@Composable
fun AlbumDetailScreen(
    viewModel: MusicViewModel,
    album: AlbumResult,
    activeTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tracks by viewModel.albumTracks.collectAsState()
    val isLoading by viewModel.isLoadingAlbumTracks.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(4.dp).testTag("album_detail_back_button")
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    VibeAlbumArt(
                        vibeCode = "youtube",
                        thumbnailUrl = album.thumbnailUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = album.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                if (album.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = album.subtitle, color = Color.Gray, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (tracks.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.selectAndPlayTrack(tracks.first(), tracks) },
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("play_all_album_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0x14FFFFFF))
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                isLoading -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SpotifyGreen)
                    }
                }
                tracks.isEmpty() -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No tracks found in this album.", color = Color.Gray)
                    }
                }
                else -> items(tracks) { track ->
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

            item { Spacer(modifier = Modifier.height(120.dp)) }
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
