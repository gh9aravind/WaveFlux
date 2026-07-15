package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.input.pointer.pointerInput
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

/**
 * Consumes every touch within this composable's bounds (taps *and* drags),
 * so nothing can pass through to whatever screen is showing underneath it.
 * Used as a base modifier for every full-screen overlay (Now Playing,
 * Search Results, Album Detail) to fix touches near the edges accidentally
 * clicking through to the previous screen.
 */
private fun Modifier.blockTouchPassthrough(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

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
                val openAlbum by viewModel.openAlbum.collectAsState()
                val activeSearchQuery by viewModel.activeSearchQuery.collectAsState()
                val activeTrack by viewModel.currentTrack.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()

                // Hardware/gesture back should collapse whichever full-screen
                // overlay is open, instead of exiting the app. Registered in
                // stack order (search results opened first, then album detail
                // can be opened from within it, then the player can be
                // expanded from on top of either) so back always unwinds the
                // most-recently-opened layer first, matching what's visible.
                BackHandler(enabled = activeSearchQuery != null) { viewModel.closeSearchResults() }
                BackHandler(enabled = openAlbum != null) { viewModel.closeAlbum() }
                BackHandler(enabled = isPlayerExpanded) { isPlayerExpanded = false }

                FrostedGlassBackground {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val maxW = maxWidth
                        val isWideScreen = maxW > 680.dp

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent, // Transparent base to pass-through the mesh layout
                            bottomBar = {
                                // The mini "Now Playing" bar should stay visible any time a
                                // track is playing/queued, even while browsing an album's
                                // tracklist - but it shouldn't show at all before anything has
                                // ever been played. Only the tab bar (Home/Search/Library)
                                // hides during album detail, since that's a drill-down screen.
                                if (!isPlayerExpanded) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = if (isWideScreen) Modifier.width(680.dp) else Modifier.fillMaxWidth()
                                        ) {
                                            AnimatedVisibility(
                                                visible = activeTrack != null,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                            ) {
                                                PersistentPlaybackBar(
                                                    viewModel = viewModel,
                                                    onExpand = { isPlayerExpanded = true }
                                                )
                                            }
                                            if (openAlbum == null) {
                                                SoundSpotBottomBar(viewModel)
                                            }
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

                                // --- Slide-up Search Results Screen ---
                                AnimatedVisibility(
                                    visible = activeSearchQuery != null,
                                    enter = slideInVertically(initialOffsetY = { it }),
                                    exit = slideOutVertically(targetOffsetY = { it }),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black)
                                                .blockTouchPassthrough()
                                        )
                                        SearchResultsScreen(
                                            viewModel = viewModel,
                                            onBack = { viewModel.closeSearchResults() },
                                            modifier = if (isWideScreen) Modifier.width(680.dp).fillMaxHeight() else Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // --- Album/Playlist Detail Screen: expands/grows into view
                                // rather than sliding up, since it's a drill-down from a tapped
                                // card rather than a full "new screen" like the player. Painted
                                // after Search Results so it always shows on top of it (an album
                                // is typically opened from within the results screen).
                                AnimatedVisibility(
                                    visible = openAlbum != null,
                                    enter = scaleIn(initialScale = 0.9f, animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                                    exit = scaleOut(targetScale = 0.9f, animationSpec = tween(180)) + fadeOut(animationSpec = tween(180)),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black)
                                                .blockTouchPassthrough()
                                        )
                                        openAlbum?.let { album ->
                                            AlbumDetailScreen(
                                                viewModel = viewModel,
                                                album = album,
                                                activeTrackId = activeTrack?.id,
                                                isPlaying = isPlaying,
                                                onBack = { viewModel.closeAlbum() },
                                                modifier = if (isWideScreen) Modifier.width(620.dp).fillMaxHeight() else Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                // --- Slide-up Full Deck Player Screen: painted LAST so it is
                                // always the topmost layer, no matter what else is open behind
                                // it (Search Results, Album Detail, or a plain tab). Expanding
                                // the player is always the most "recent" action a person takes -
                                // it should never end up hidden behind an earlier screen. ---
                                AnimatedVisibility(
                                    visible = isPlayerExpanded,
                                    enter = slideInVertically(initialOffsetY = { it }),
                                    exit = slideOutVertically(targetOffsetY = { it }),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        // Scrim drawn BEHIND the screen content (a sibling, not a
                                        // parent) — it only catches touches that fall through empty
                                        // areas of PlayerScreen (e.g. its side margins) so they can't
                                        // leak to the screen underneath. Being a sibling rather than
                                        // an ancestor means it never intercepts PlayerScreen's own
                                        // clicks/drags first, so scrolling/dragging inside it is
                                        // completely unaffected.
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .blockTouchPassthrough()
                                        )
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



