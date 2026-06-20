package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.MusicViewModel
import com.example.ui.theme.SpotifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var queryText by remember { mutableStateOf("") }
    val allTracks by viewModel.allTracks.collectAsState()
    val activeTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Collect Elasticsearch integration states
    val isElasticActive by viewModel.isElasticSearchActive.collectAsState()
    val elasticResults by viewModel.elasticSearchResults.collectAsState()
    val isElasticLoading by viewModel.isElasticSearchLoading.collectAsState()
    val elasticError by viewModel.elasticSearchError.collectAsState()

    // Filter tracks dynamically based on local storage cache
    val filteredTracks = remember(queryText, allTracks) {
        if (queryText.trim().isEmpty()) {
            emptyList()
        } else {
            allTracks.filter {
                it.title.contains(queryText, ignoreCase = true) ||
                        it.artist.contains(queryText, ignoreCase = true) ||
                        it.genre.contains(queryText, ignoreCase = true)
            }
        }
    }

    // Trigger Elasticsearch dynamic cluster requests
    LaunchedEffect(queryText, isElasticActive) {
        if (isElasticActive) {
            viewModel.performElasticSearch(queryText)
        }
    }

    // Unify display strategy based on network capability and indexing results
    val finalTracksToDisplay = remember(queryText, isElasticActive, elasticResults, filteredTracks) {
        if (queryText.trim().isEmpty()) {
            emptyList()
        } else if (isElasticActive && elasticResults.isNotEmpty()) {
            elasticResults
        } else {
            filteredTracks
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // --- 1. Search Screen Title ---
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
        )

        // --- Elasticsearch Mode Indicator / Selector ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(Color(0x0CFFFFFF), RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isElasticActive) SpotifyGreen else Color.Gray, RoundedCornerShape(50.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isElasticActive) "YouTube Music Search" else "Local Library Scanner",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isElasticLoading) "Searching YouTube..."
                           else if (elasticError != null) "${elasticError}"
                           else if (queryText.isNotEmpty() && elasticResults.isNotEmpty()) "Live results from YouTube Music"
                           else "Type to search YouTube Music",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
            Box(
                modifier = Modifier
                    .background(if (isElasticActive) SpotifyGreen.copy(alpha = 0.2f) else Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, if (isElasticActive) SpotifyGreen else Color(0x22FFFFFF)), RoundedCornerShape(8.dp))
                    .clickable { viewModel.setElasticSearchActive(!isElasticActive) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isElasticActive) "YOUTUBE" else "LOCAL",
                    color = if (isElasticActive) SpotifyGreen else Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- 2. Search Text Field ---
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            placeholder = { Text("What do you want to listen to?", color = Color.Gray) },
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
                focusedContainerColor = Color(0x19FFFFFF), // Translucent white/10 glass
                unfocusedContainerColor = Color(0x0CFFFFFF), // Translucent white/5 glass
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = Color(0x1FFFFFFF), // Thin white border representing frosted edge
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .testTag("search_field_bar")
        )

        // --- 3. Render Query Results or Category Hub ---
        if (queryText.trim().isNotEmpty()) {
            if (finalTracksToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No results found for \"$queryText\"",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Double check the spelling or explore categories.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Text(
                    text = if (isElasticActive && elasticResults.isNotEmpty()) "YouTube Results" else "Local Library Matches",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(finalTracksToDisplay) { track ->
                        val isCurrent = activeTrack?.id == track.id
                        TrackListItem(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onClick = {
                                viewModel.selectAndPlayTrack(track, finalTracksToDisplay)
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
        } else {
            // Category Browse Cards Grid (Spotify style category hubs)
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
                        // Click searches for the genre automatically for quick responsive previewing!
                        queryText = name.split(" ").first()
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
            .background(backgroundBrush, alpha = 0.5f) // 0.5f alpha for beautiful backglass mesh merging
            .border(BorderStroke(1.dp, Color(0x22FFFFFF)), RoundedCornerShape(12.dp)) // border-white/10 thin white border
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
