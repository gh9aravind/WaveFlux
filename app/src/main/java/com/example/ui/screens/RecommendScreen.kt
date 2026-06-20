package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MusicViewModel
import com.example.ui.theme.SpotifyGreen

@Composable
fun RecommendScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibeText by viewModel.currentVibeText.collectAsState()
    val recommendations by viewModel.aiRecommendations.collectAsState()
    val isLoading by viewModel.isLoadingRecommendations.collectAsState()
    val error by viewModel.recommendationError.collectAsState()

    val allTracks by viewModel.allTracks.collectAsState()
    val activeTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val suggestedVibePills = listOf(
        "Late night coding under neon logs",
        "Coffee shop warmth on rainy Sunday",
        "Beating personal records in the gym",
        "Walking empty sunset streets",
        "Ambient focus reading sci-fi"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // --- 1. Screen Headline ---
        item {
            Column(modifier = Modifier.padding(top = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI symbol",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Consult AI Radio DJ",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Text(
                    text = "Describe your mood or setting to build customized Gemini-curated playlists in seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // --- 2. Mood Pill Selectors ---
        item {
            Column {
                Text(
                    text = "Quick Vibe Ideas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestedVibePills) { pillText ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (vibeText == pillText) Color(0x3310B981) else Color(0x0CFFFFFF), // translucent emerald tint or white/5 glass
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (vibeText == pillText) SpotifyGreen else Color(0x1BFFFFFF), // emerald border or subtle white border
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.setVibeText(pillText) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = pillText,
                                fontSize = 11.sp,
                                color = if (vibeText == pillText) SpotifyGreen else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- 3. Prompt Input Area ---
        item {
            Column {
                OutlinedTextField(
                    value = vibeText,
                    onValueChange = { viewModel.setVibeText(it) },
                    placeholder = {
                        Text(
                            "What is your atmosphere or activity right now?",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("ai_vibe_input_box"),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x19FFFFFF), // translucent white/10 glass
                        unfocusedContainerColor = Color(0x0CFFFFFF), // translucent white/5 glass
                        focusedBorderColor = SpotifyGreen,
                        unfocusedBorderColor = Color(0x12FFFFFF), // thin white glass edge
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.requestRecommendations(context) },
                    enabled = vibeText.trim().isNotEmpty() && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.DarkGray,
                        disabledContentColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_dj_generate_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Tuning Frequencies...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Curate My Soundtrack", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 4. Render Error / API Guidance Banner ---
        if (error != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C191D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AI Channel Offline",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE57373),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (error!!.contains("API Key")) {
                                "Your GEMINI_API_KEY is not configured. Please open the Secrets Panel in your Google AI Studio UI (top-right or side drawer) and securely key in your GEMINI_API_KEY to authorize real-time music curation!"
                            } else {
                                "Failed connecting: $error. Ensure you have an active network connection."
                            },
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // --- 5. Render Playlist recommendations ---
        if (recommendations.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Curated AI Broadcast Station",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SpotifyGreen
                    )
                    Text(
                        text = "Matches: \"${vibeText}\"",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            items(recommendations) { song ->
                val matchingTrackInDb = allTracks.find { it.title == song.title }
                val isCurrent = activeTrack?.title == song.title
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)), // translucent white/5 glass
                    border = BorderStroke(1.dp, Color(0x12FFFFFF)), // border border-white/5 thin glass stroke
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (matchingTrackInDb != null) {
                                viewModel.selectAndPlayTrack(matchingTrackInDb, allTracks)
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isCurrent) SpotifyGreen else Color.White
                                )
                                Text(
                                    text = "${song.artist} • ${song.genre.uppercase()}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            // Streaming Play Indicator
                            if (matchingTrackInDb != null) {
                                IconButton(onClick = {
                                    viewModel.selectAndPlayTrack(matchingTrackInDb, allTracks)
                                }) {
                                    Icon(
                                        imageVector = if (isCurrent && isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                        contentDescription = "Play custom track",
                                        tint = SpotifyGreen
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider(
                            color = Color(0x12FFFFFF), // Translucent partition line
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Text(
                            text = "\"${song.vibeDescription}\"",
                            fontStyle = FontStyle.Italic,
                            fontSize = 11.5.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
