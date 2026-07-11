package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.ui.MusicViewModel
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.PersistentPlaybackBar
import com.example.ui.components.VibeAlbumArt
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpotifyGreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Core State & Storage Initialization
        val database = MusicDatabase.getDatabase(applicationContext)
        val repository = MusicRepository(applicationContext, database.musicDao())
        val viewModel = ViewModelProvider(this, MusicViewModel.Factory(repository, applicationContext))[MusicViewModel::class.java]
        setContent {
            MyApplicationTheme {
                var isPlayerExpanded by remember { mutableStateOf(false) }

                FrostedGlassBackground {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val maxW = maxWidth
                        val isWideScreen = maxW > 680.dp

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent, // Transparent base to pass-through the mesh layout
                            bottomBar = {
                                // Keep bottom bar and persistent music layout visible unless the player is expanded
                                if (!isPlayerExpanded) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = if (isWideScreen) Modifier.width(680.dp) else Modifier.fillMaxWidth()
                                        ) {
                                            PersistentPlaybackBar(
                                                viewModel = viewModel,
                                                onExpand = { isPlayerExpanded = true }
                                            )
                                            SoundSpotBottomBar(viewModel)
                                        }
                                    }
                                }
                            },
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                val activeTab by viewModel.currentTab.collectAsState()

                                // --- Main Tab Switchboard Layout ---
                                Box(
                                    modifier = if (isWideScreen) Modifier.width(680.dp).fillMaxHeight() else Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (activeTab) {
                                        "home" -> HomeScreen(viewModel = viewModel)
                                        "search" -> SearchScreen(viewModel = viewModel)
                                        "library" -> LibraryScreen(viewModel = viewModel)
                                        else -> HomeScreen(viewModel = viewModel)
                                    }
                                }

                                // --- Slide-up Full Deck Player Screen ---
                                AnimatedVisibility(
                                    visible = isPlayerExpanded,
                                    enter = slideInVertically(initialOffsetY = { it }),
                                    exit = slideOutVertically(targetOffsetY = { it }),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PlayerScreen(
                                            viewModel = viewModel,
                                            onCollapse = { isPlayerExpanded = false },
                                            modifier = if (isWideScreen) Modifier.width(620.dp).fillMaxHeight() else Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundSpotBottomBar(viewModel: MusicViewModel) {
    val activeTab by viewModel.currentTab.collectAsState()

    NavigationBar(
        containerColor = Color(0xF2121212), // Deep Slate Translucent navigation bar
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("soundspot_bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = activeTab == "home",
            onClick = { viewModel.setTab("home") },
            icon = {
                Icon(
                    imageVector = if (activeTab == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color(0x2210B981) // bg-emerald-500/10 visual matching design
            ),
            modifier = Modifier.testTag("nav_home_button")
        )

        NavigationBarItem(
            selected = activeTab == "search",
            onClick = { viewModel.setTab("search") },
            icon = {
                Icon(
                    imageVector = if (activeTab == "search") Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "Search"
                )
            },
            label = { Text("Search", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color(0x2210B981)
            ),
            modifier = Modifier.testTag("nav_search_button")
        )

        NavigationBarItem(
            selected = activeTab == "library",
            onClick = { viewModel.setTab("library") },
            icon = {
                Icon(
                    imageVector = if (activeTab == "library") Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                    contentDescription = "Your Library"
                )
            },
            label = { Text("Library", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color(0x2210B981)
            ),
            modifier = Modifier.testTag("nav_library_button")
        )
    }
}



