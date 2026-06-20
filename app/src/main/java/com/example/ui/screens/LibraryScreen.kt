package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Star
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
import com.example.ui.MusicViewModel
import com.example.ui.theme.SpotifyGreen
import com.example.ui.components.HtmlWebViewPlayer
import com.example.ui.components.HtmlWebPlayerGenerator


@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val activeTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // --- Token & Subscription States ---
    val activeToken by viewModel.activeApiToken.collectAsState()
    val activeTier by viewModel.activeSubscriptionTier.collectAsState()
    var showTokenEditor by remember { mutableStateOf(false) }
    var inputTokenText by remember { mutableStateOf(activeToken ?: "") }

    LaunchedEffect(activeToken) {
        inputTokenText = activeToken ?: ""
    }

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Favorites, 1: Downloads
    var offlinePlaybackOnlyMode by remember { mutableStateOf(false) }

    val activeDisplayList = remember(selectedSubTab, offlinePlaybackOnlyMode, favoriteTracks, downloadedTracks) {
        if (offlinePlaybackOnlyMode) {
            downloadedTracks
        } else {
            if (selectedSubTab == 0) favoriteTracks else downloadedTracks
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // --- 1. Library Title ---
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
        )

        // --- 1B. Auth0 / Firebase Token & Unlimited Tier Manager ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0x0CFFFFFF) // translucent white/5 glass
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0x1BFFFFFF) // subtle white glass border
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isUnlimited = activeTier.contains("Unlimited", ignoreCase = true)
                        Icon(
                            imageVector = if (isUnlimited) Icons.Default.Star else Icons.Default.Lock,
                            contentDescription = "Token indicator",
                            tint = if (isUnlimited) Color(0xFFFBBF24) else SpotifyGreen, // Amber or green tint
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Auth0 / Firebase Token Manager",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Tier: $activeTier",
                                fontSize = 11.sp,
                                color = if (isUnlimited) Color(0xFFFBBF24) else Color.LightGray,
                                fontWeight = if (isUnlimited) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    Button(
                        onClick = { showTokenEditor = !showTokenEditor },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x16FFFFFF),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (showTokenEditor) "Hide" else "Manage",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showTokenEditor) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "JWT Session Token",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = inputTokenText,
                        onValueChange = { inputTokenText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("api_token_input"),
                        placeholder = { Text("Enter Auth0 or Firebase Identity JWT", fontSize = 12.sp, color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x11FFFFFF),
                            unfocusedContainerColor = Color(0x07FFFFFF),
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = Color(0x15FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Action: Set Unlimited Token
                        Button(
                            onClick = {
                                viewModel.setTokenToUnlimited()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(36.dp)
                                .testTag("set_unlimited_token_button")
                        ) {
                            Text("Set Unlimited", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Apply manually edited token
                        Button(
                            onClick = {
                                viewModel.updateApiToken(inputTokenText.trim())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(36.dp)
                                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(8.dp))
                        ) {
                            Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Clear Token
                        Button(
                            onClick = {
                                viewModel.updateApiToken(null)
                                inputTokenText = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Red
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(0.8f)
                                .height(36.dp)
                                .border(BorderStroke(1.dp, Color(0x12FF0000)), RoundedCornerShape(8.dp))
                        ) {
                            Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Setting 'Unlimited' injects bypass scopes to bypass network API limits securely.",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // --- 2. Master Offline Simulation Toggle Banner ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (offlinePlaybackOnlyMode) Color(0x2210B981) else Color(0x0DFFFFFF) // Emerald tinted or translucent white/5 glass
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (offlinePlaybackOnlyMode) SpotifyGreen.copy(alpha = 0.5f) else Color(0x12FFFFFF) // thin glass borders
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (offlinePlaybackOnlyMode) Icons.Default.SignalWifiOff else Icons.Default.OfflinePin,
                        contentDescription = "Offline indicator",
                        tint = if (offlinePlaybackOnlyMode) SpotifyGreen else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (offlinePlaybackOnlyMode) "Simulating Offline Mode" else "Tested Offline Player",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (offlinePlaybackOnlyMode) "Hiding all non-downloaded streams." else "Simulate airplane mode to test downloaded tracks.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                Switch(
                    checked = offlinePlaybackOnlyMode,
                    onCheckedChange = { offlinePlaybackOnlyMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = SpotifyGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.testTag("offline_mode_toggle")
                )
            }
        }

        // --- 3. Tab Selectors (if not overridden by offline mode) ---
        if (!offlinePlaybackOnlyMode) {
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
                    text = {
                        Text(
                            "Liked Songs (${favoriteTracks.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    selectedContentColor = SpotifyGreen,
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_favorites")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Text(
                            "Downloads (${downloadedTracks.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    selectedContentColor = SpotifyGreen,
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_downloads")
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = {
                        Text(
                            "HTML5 Web Player",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    selectedContentColor = SpotifyGreen,
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_html_player")
                )
            }
        } else {
            Text(
                text = "Downloaded Cache Offline (${activeDisplayList.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SpotifyGreen,
                modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
            )
        }

        // --- 4. Render Tab Content or HTML5 WebView Player ---
        if (selectedSubTab == 2 && !offlinePlaybackOnlyMode) {
            val htmlString = remember(favoriteTracks, activeTier) {
                HtmlWebPlayerGenerator.generatePlayerHtml(favoriteTracks, activeTier)
            }
            HtmlWebViewPlayer(
                viewModel = viewModel,
                htmlContent = htmlString,
                modifier = Modifier.weight(1f)
            )
        } else {
            if (activeDisplayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                        Text(
                            text = if (offlinePlaybackOnlyMode) "No tracks cached yet!" else if (selectedSubTab == 0) "Your liked songs list is empty!" else "No downloaded songs!",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (offlinePlaybackOnlyMode) "Toggle offline mode off and tap 'Download' on any track." else "Enjoy and save tracks from your home playlists.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(activeDisplayList) { track ->
                        val isCurrent = activeTrack?.id == track.id
                        TrackListItem(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onClick = {
                                viewModel.selectAndPlayTrack(track, activeDisplayList)
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
    }
}
