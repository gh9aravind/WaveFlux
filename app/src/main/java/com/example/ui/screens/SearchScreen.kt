package com.example.ui.screens

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val activeTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val results by viewModel.elasticSearchResults.collectAsState()
    val isLoading by viewModel.isElasticSearchLoading.collectAsState()
    val error by viewModel.elasticSearchError.collectAsState()

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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                .padding(bottom = 20.dp)
                .testTag("search_field_bar")
        )

        if (queryText.trim().isNotEmpty()) {
            when {
                isLoading && results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SpotifyGreen)
                    }
                }
                results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = error ?: "No results found for \"$queryText\"",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(results) { track ->
                            val isCurrent = activeTrack?.id == track.id
                            TrackListItem(
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && isPlaying,
                                onClick = {
                                    viewModel.selectAndPlayTrack(track, results)
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
