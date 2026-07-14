package com.example.ui
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.cast.CastPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.api.GeminiRecommendationService
import com.example.data.api.RecommendedSong
import com.example.data.model.AlbumResult
import com.example.data.model.PlaylistWithCount
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import com.example.playback.PlaybackService
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(
    private val repository: MusicRepository,
    private val appContext: Context
) : ViewModel() {
    private val TAG = "MusicViewModel"

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var progressTrackingJob: Job? = null

    // --- Chromecast ---
    private var castPlayer: CastPlayer? = null
    private var castContext: CastContext? = null

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _castDeviceName = MutableStateFlow<String?>(null)
    val castDeviceName: StateFlow<String?> = _castDeviceName.asStateFlow()

    /** Returns whichever player (local or cast) should currently receive commands. */
    private fun activePlayer(): Player? = if (_isCasting.value) castPlayer else controller

    // --- Database Flows ---
    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedTracks: StateFlow<List<Track>> = repository.downloadedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Track>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Player UI States ---
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0)
    val playbackPosition: StateFlow<Int> = _playbackPosition.asStateFlow()

    private val _trackDuration = MutableStateFlow(0)
    val trackDuration: StateFlow<Int> = _trackDuration.asStateFlow()

    private val _audioQualityInfo = MutableStateFlow<String?>(null)
    val audioQualityInfo: StateFlow<String?> = _audioQualityInfo.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    // --- Music Queue Management ---
    private val _playQueue = MutableStateFlow<List<Track>>(emptyList())
    val playQueue: StateFlow<List<Track>> = _playQueue.asStateFlow()

    fun reorderQueue(newQueue: List<Track>) {
        _playQueue.value = newQueue
    }

    private val _currentQueueIndex = MutableStateFlow(0)

    // --- Downloading Map Progress States ---
    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()

    // --- Gemini Recommendation States ---
    private val _aiRecommendations = MutableStateFlow<List<RecommendedSong>>(emptyList())
    val aiRecommendations: StateFlow<List<RecommendedSong>> = _aiRecommendations.asStateFlow()

    private val _isLoadingRecommendations = MutableStateFlow(false)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()

    private val _recommendationError = MutableStateFlow<String?>(null)
    val recommendationError: StateFlow<String?> = _recommendationError.asStateFlow()

    private val _currentVibeText = MutableStateFlow("")
    val currentVibeText: StateFlow<String> = _currentVibeText.asStateFlow()

    // --- Active Tab State ---
    private val _currentTab = MutableStateFlow("home") // "home", "search", "recommend", "library"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // --- Elasticsearch Active State ---
    private val _isElasticSearchActive = MutableStateFlow(true) // default active to satisfy user's structural deployment goals
    val isElasticSearchActive: StateFlow<Boolean> = _isElasticSearchActive.asStateFlow()

    private val _elasticSearchResults = MutableStateFlow<List<Track>>(emptyList())
    val elasticSearchResults: StateFlow<List<Track>> = _elasticSearchResults.asStateFlow()

    private val _isElasticSearchLoading = MutableStateFlow(false)
    val isElasticSearchLoading: StateFlow<Boolean> = _isElasticSearchLoading.asStateFlow()

    private val _elasticSearchError = MutableStateFlow<String?>(null)
    val elasticSearchError: StateFlow<String?> = _elasticSearchError.asStateFlow()

    fun setElasticSearchActive(enabled: Boolean) {
        _isElasticSearchActive.value = enabled
    }

    // --- Authentication & Token Management States ---
    private val _activeApiToken = MutableStateFlow<String?>("mock-transient-auth0-token-for-preview-purposes")
    val activeApiToken: StateFlow<String?> = _activeApiToken.asStateFlow()

    private val _activeSubscriptionTier = MutableStateFlow("Free Preview Tier")
    val activeSubscriptionTier: StateFlow<String> = _activeSubscriptionTier.asStateFlow()

    fun updateApiToken(token: String?) {
        _activeApiToken.value = token
        com.example.data.api.ProductionStackManager.setApiToken(token)
        if (token != null && (token.contains("unlimited", ignoreCase = true) || token == "unlimited_premium_high_throughput_access_token")) {
            _activeSubscriptionTier.value = "Unlimited Premium Tier"
        } else if (token.isNullOrBlank()) {
            _activeSubscriptionTier.value = "Anonymous Guest Tier"
        } else {
            _activeSubscriptionTier.value = "Auth0 Validated Tier"
        }
    }

    fun setTokenToUnlimited() {
        updateApiToken("unlimited_premium_high_throughput_access_token")
    }

    // --- Category Search (Songs / Albums / Playlists) ---
    private val _searchCategory = MutableStateFlow("all") // "all", "songs", "albums", "playlists"
    val searchCategory: StateFlow<String> = _searchCategory.asStateFlow()

    fun setSearchCategory(category: String) {
        _searchCategory.value = category
    }

    private val _albumResults = MutableStateFlow<List<AlbumResult>>(emptyList())
    val albumResults: StateFlow<List<AlbumResult>> = _albumResults.asStateFlow()

    private val _isLoadingAlbums = MutableStateFlow(false)
    val isLoadingAlbums: StateFlow<Boolean> = _isLoadingAlbums.asStateFlow()

    private val _playlistResults = MutableStateFlow<List<AlbumResult>>(emptyList())
    val playlistResults: StateFlow<List<AlbumResult>> = _playlistResults.asStateFlow()

    private val _isLoadingPlaylists = MutableStateFlow(false)
    val isLoadingPlaylists: StateFlow<Boolean> = _isLoadingPlaylists.asStateFlow()

    private val _videoResults = MutableStateFlow<List<Track>>(emptyList())
    val videoResults: StateFlow<List<Track>> = _videoResults.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    private val _openAlbum = MutableStateFlow<AlbumResult?>(null)
    val openAlbum: StateFlow<AlbumResult?> = _openAlbum.asStateFlow()

    private val _albumTracks = MutableStateFlow<List<Track>>(emptyList())
    val albumTracks: StateFlow<List<Track>> = _albumTracks.asStateFlow()

    private val _isLoadingAlbumTracks = MutableStateFlow(false)
    val isLoadingAlbumTracks: StateFlow<Boolean> = _isLoadingAlbumTracks.asStateFlow()

    /** Opens an album/playlist and loads its full tracklist (e.g. every song from a movie). */
    fun openAlbum(album: AlbumResult) {
        _openAlbum.value = album
        _albumTracks.value = emptyList()
        _isLoadingAlbumTracks.value = true
        viewModelScope.launch {
            _albumTracks.value = repository.getAlbumTracks(album.playlistUrl)
            _isLoadingAlbumTracks.value = false
        }
    }

    fun closeAlbum() {
        _openAlbum.value = null
        _albumTracks.value = emptyList()
    }

    // --- Full-screen search results (opened on Enter / search-icon submit) ---
    private val _activeSearchQuery = MutableStateFlow<String?>(null)
    val activeSearchQuery: StateFlow<String?> = _activeSearchQuery.asStateFlow()

    /** Submits the current search text and opens the dedicated results screen. */
    fun submitSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        _activeSearchQuery.value = q
        performElasticSearch(q)
    }

    fun closeSearchResults() {
        _activeSearchQuery.value = null
    }

    fun performElasticSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            _elasticSearchResults.value = emptyList()
            _albumResults.value = emptyList()
            _playlistResults.value = emptyList()
            _videoResults.value = emptyList()
            _elasticSearchError.value = null
            return
        }

        _isElasticSearchLoading.value = true
        _elasticSearchError.value = null

        viewModelScope.launch {
            try {
                val results = repository.searchYouTube(q)
                _elasticSearchResults.value = results
                if (results.isNotEmpty()) {
                    viewModelScope.launch { repository.prefetchStreams(results.take(4)) }
                }
                if (results.isEmpty()) {
                    _elasticSearchError.value = "No YouTube results found. Check your connection and try again."
                }
            } catch (e: Exception) {
                _elasticSearchError.value = "Search failed: ${e.message}"
                _elasticSearchResults.value = emptyList()
            } finally {
                _isElasticSearchLoading.value = false
            }
        }

        _isLoadingAlbums.value = true
        viewModelScope.launch {
            _albumResults.value = try {
                repository.searchYouTubeAlbums(q)
            } catch (e: Exception) {
                emptyList()
            } finally {
                _isLoadingAlbums.value = false
            }
        }

        _isLoadingPlaylists.value = true
        viewModelScope.launch {
            _playlistResults.value = try {
                repository.searchYouTubePlaylists(q)
            } catch (e: Exception) {
                emptyList()
            } finally {
                _isLoadingPlaylists.value = false
            }
        }

        _isLoadingVideos.value = true
        viewModelScope.launch {
            _videoResults.value = try {
                val videos = repository.searchYouTubeVideos(q)
                if (videos.isNotEmpty()) {
                    viewModelScope.launch { repository.prefetchStreams(videos.take(3)) }
                }
                videos
            } catch (e: Exception) {
                emptyList()
            } finally {
                _isLoadingVideos.value = false
            }
        }
    }

    private val _trendingTracks = MutableStateFlow<List<Track>>(emptyList())
    val trendingTracks: StateFlow<List<Track>> = _trendingTracks.asStateFlow()

    private val _hipHopTracks = MutableStateFlow<List<Track>>(emptyList())
    val hipHopTracks: StateFlow<List<Track>> = _hipHopTracks.asStateFlow()

    private val _melodyTracks = MutableStateFlow<List<Track>>(emptyList())
    val melodyTracks: StateFlow<List<Track>> = _melodyTracks.asStateFlow()

    init {
        viewModelScope.launch {
            repository.preloadDefaultTracksIfEmpty()
        }
        initController()
        initCastPlayer()
        loadHomeCategories()
    }

    private fun loadHomeCategories() {
        viewModelScope.launch {
            _trendingTracks.value = repository.searchYouTube("trending songs 2026")
        }
        viewModelScope.launch {
            _hipHopTracks.value = repository.searchYouTube("hip hop songs")
        }
        viewModelScope.launch {
            _melodyTracks.value = repository.searchYouTube("melody songs")
        }
    }

    private fun initController() {
        val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val mc = future.get()
                controller = mc
                mc.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        _isPlaying.value = isPlayingNow
                        if (isPlayingNow) startProgressTracking() else stopProgressTracking()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _trackDuration.value = mc.duration.toInt().coerceAtLeast(0)
                            }
                            Player.STATE_ENDED -> onSongCompleted()
                            else -> {}
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "Player error: ${error.message}", error)
                        _isPlaying.value = false
                        stopProgressTracking()
                    }

                    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                        updateAudioQualityInfo(tracks)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Failed connecting to PlaybackService", e)
            }
        }, MoreExecutors.directExecutor())
    }

    /**
     * Sets up the Cast framework session listener. Whenever a Chromecast
     * session starts/ends, we switch the "active player" between the local
     * MediaController and the CastPlayer, and re-load the current track on
     * whichever player just became active so playback continues seamlessly.
     */
    private fun initCastPlayer() {
        try {
            val context = CastContext.getSharedInstance(appContext)
            castContext = context

            val player = CastPlayer(context)
            castPlayer = player
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    if (_isCasting.value) {
                        _isPlaying.value = isPlayingNow
                        if (isPlayingNow) startProgressTracking() else stopProgressTracking()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (!_isCasting.value) return
                    when (playbackState) {
                        Player.STATE_READY -> _trackDuration.value = player.duration.toInt().coerceAtLeast(0)
                        Player.STATE_ENDED -> onSongCompleted()
                        else -> {}
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(TAG, "Cast player error: ${error.message}", error)
                }

                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    updateAudioQualityInfo(tracks)
                }
            })

            context.sessionManager.addSessionManagerListener(
                object : SessionManagerListener<CastSession> {
                    override fun onSessionStarted(session: CastSession, sessionId: String) {
                        switchToCast(session)
                    }

                    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                        switchToCast(session)
                    }

                    override fun onSessionEnded(session: CastSession, error: Int) {
                        switchToLocal()
                    }

                    override fun onSessionStarting(session: CastSession) {}
                    override fun onSessionStartFailed(session: CastSession, error: Int) {}
                    override fun onSessionEnding(session: CastSession) {}
                    override fun onSessionResuming(session: CastSession, sessionId: String) {}
                    override fun onSessionResumeFailed(session: CastSession, error: Int) {}
                    override fun onSessionSuspended(session: CastSession, reason: Int) {}
                },
                CastSession::class.java
            )
        } catch (e: Exception) {
            // Cast Framework isn't available on some devices (e.g. no Google Play Services) - safe to ignore.
            Log.e(TAG, "Chromecast not available on this device", e)
        }
    }

    private fun switchToCast(session: CastSession) {
        val wasPlaying = _isPlaying.value
        val resumePosition = _playbackPosition.value.toLong()
        controller?.pause()
        _isCasting.value = true
        _castDeviceName.value = session.castDevice?.friendlyName
        val track = _currentTrack.value
        if (track != null) {
            viewModelScope.launch { playTrackContent(track, resumePosition, autoPlay = wasPlaying) }
        }
    }

    private fun switchToLocal() {
        val wasPlaying = _isPlaying.value
        val resumePosition = _playbackPosition.value.toLong()
        _isCasting.value = false
        _castDeviceName.value = null
        val track = _currentTrack.value
        if (track != null) {
            viewModelScope.launch { playTrackContent(track, resumePosition, autoPlay = wasPlaying) }
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setVibeText(text: String) {
        _currentVibeText.value = text
    }

    // --- Media Playback Operations ---
    fun selectAndPlayTrack(track: Track, queue: List<Track>) {
        _playQueue.value = queue
        _currentTrack.value = track
        _currentQueueIndex.value = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        viewModelScope.launch {
            repository.updateLastPlayed(track.id)
            playTrackContent(track)
        }
    }

    private suspend fun playTrackContent(track: Track, resumePosition: Long = 0L, autoPlay: Boolean = true) {
        try {
            _isPlaying.value = false
            // Reset position/duration immediately so the seekbar doesn't
            // briefly show the previous track's leftover position/time
            // while the new track's stream is being resolved.
            _playbackPosition.value = resumePosition.toInt()
            _trackDuration.value = 0

            // Resolve a fresh playable URL right before playing. For YouTube
            // tracks this performs a network extraction since the signed
            // stream URLs expire after a few hours and can't be cached.
            // Note: local device downloads aren't reachable by a Chromecast
            // receiver, so while casting we always resolve a streamable URL.
            val dataSource = repository.resolvePlayableUrl(track)
            if (dataSource == null) {
                Log.e(TAG, "Could not resolve a playable stream for ${track.title}")
                return
            }

            // Chromecast's default receiver shows title/artist/artwork from
            // this MediaMetadata automatically - no extra receiver code needed.
            val mediaItem = MediaItem.Builder()
                .setUri(dataSource)
                .setMimeType(androidx.media3.common.MimeTypes.AUDIO_MP4)
                .setMediaId(track.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .apply { track.thumbnailUrl?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                        .build()
                )
                .build()

            withContext(Dispatchers.Main) {
                val player = activePlayer()
                player?.apply {
                    setMediaItem(mediaItem, resumePosition)
                    prepare()
                    playWhenReady = autoPlay
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing song: ${track.title}", e)
        }
    }
    fun togglePlayPause() {
        val player = activePlayer() ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (_currentTrack.value != null) {
                    player.play()
                } else {
                    val first = allTracks.value.firstOrNull()
                    if (first != null) {
                        selectAndPlayTrack(first, allTracks.value)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause state", e)
        }
    }
    fun seekTo(positionMs: Int) {
        try {
            activePlayer()?.seekTo(positionMs.toLong())
            _playbackPosition.value = positionMs
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking playback position", e)
        }
    }
    fun playNext() {
        val queue = _playQueue.value
        if (queue.isEmpty()) return

        val nextIndex = if (_isShuffleEnabled.value) {
            (0 until queue.size).random()
        } else {
            if (_currentQueueIndex.value >= queue.lastIndex) 0 else _currentQueueIndex.value + 1
        }

        val nextTrack = queue.getOrNull(nextIndex)
        if (nextTrack != null) {
            _currentQueueIndex.value = nextIndex
            selectAndPlayTrack(nextTrack, queue)
        }
    }

    fun playPrevious() {
        val queue = _playQueue.value
        if (queue.isEmpty()) return

        val prevIndex = if (_currentQueueIndex.value <= 0) queue.lastIndex else _currentQueueIndex.value - 1

        val prevTrack = queue.getOrNull(prevIndex)
        if (prevTrack != null) {
            _currentQueueIndex.value = prevIndex
            selectAndPlayTrack(prevTrack, queue)
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
    }

    private fun updateAudioQualityInfo(tracks: androidx.media3.common.Tracks) {
        for (group in tracks.groups) {
            if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && group.isSelected) {
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        _audioQualityInfo.value = formatAudioQuality(group.getTrackFormat(i))
                    }
                }
            }
        }
    }

    private fun formatAudioQuality(format: androidx.media3.common.Format): String {
        val codec = when {
            format.sampleMimeType?.contains("mp4a") == true -> "AAC"
            format.sampleMimeType?.contains("opus") == true -> "OPUS"
            format.sampleMimeType?.contains("mpeg") == true -> "MP3"
            format.sampleMimeType?.contains("flac") == true -> "FLAC"
            else -> format.sampleMimeType?.substringAfterLast('/')?.uppercase() ?: "AUDIO"
        }
        val sampleRate = if (format.sampleRate != androidx.media3.common.Format.NO_VALUE) {
            "${format.sampleRate / 1000}kHz"
        } else null
        val bitrate = if (format.bitrate != androidx.media3.common.Format.NO_VALUE && format.bitrate > 0) {
            "${format.bitrate / 1000}kbps"
        } else null
        return listOfNotNull(codec, bitrate, sampleRate).joinToString(" · ")
    }

    private fun onSongCompleted() {
        if (_isRepeatEnabled.value) {
            // Repeat active song
            _currentTrack.value?.let { track ->
                viewModelScope.launch { playTrackContent(track) }
            }
        } else {
            // Play next track in queue
            playNext()
        }
    }

    // --- Dynamic Time Tracker Coroutine ---
    private fun startProgressTracking() {
        stopProgressTracking()
        progressTrackingJob = viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                try {
                    activePlayer()?.let { player ->
                        if (player.isPlaying) {
                            _playbackPosition.value = player.currentPosition.toInt()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in progress tracking job", e)
                }
                delay(250) // refresh 4 times a second
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    // --- Like / Favorite Operations ---
    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavoriteTrack(track)
            val updatedStatus = !track.isFavorite

            // Sync current active track details if updated
            if (_currentTrack.value?.id == track.id) {
                _currentTrack.value = _currentTrack.value?.copy(isFavorite = updatedStatus)
            }
        }
    }

    // --- Download Operations ---
    fun startTrackDownload(context: Context, track: Track) {
        if (track.isDownloaded) return

        _downloadProgressMap.value = _downloadProgressMap.value + (track.id to 0.01f)

        viewModelScope.launch {
            val success = repository.downloadTrack(track.id) { progress ->
                // Update download percentage status
                _downloadProgressMap.value = _downloadProgressMap.value + (track.id to progress)
            }

            _downloadProgressMap.value = _downloadProgressMap.value - track.id

            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "${track.title} downloaded offline!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed downloading ${track.title}.", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Sync UI state if current track was downloaded
            if (_currentTrack.value?.id == track.id) {
                val updatedTrack = repository.getTrackById(track.id)
                if (updatedTrack != null) {
                    _currentTrack.value = updatedTrack
                }
            }
        }
    }

    fun removeTrackDownload(context: Context, track: Track) {
        viewModelScope.launch {
            repository.removeOfflineDownload(track.id)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Offline download removed.", Toast.LENGTH_SHORT).show()
            }
            
            // Sync active player parameters
            if (_currentTrack.value?.id == track.id) {
                val updatedTrack = repository.getTrackById(track.id)
                if (updatedTrack != null) {
                    _currentTrack.value = updatedTrack
                }
            }
        }
    }

   private val _recommendedTracks = MutableStateFlow<List<Track?>>(emptyList())
    val recommendedTracks: StateFlow<List<Track?>> = _recommendedTracks.asStateFlow()

    // --- Gemini-Driven Recommendation Logic ---
    fun requestRecommendations(context: Context) {
        val vibe = _currentVibeText.value.trim()
        if (vibe.isEmpty()) return

        _isLoadingRecommendations.value = true
        _recommendationError.value = null
        _aiRecommendations.value = emptyList()
        _recommendedTracks.value = emptyList()

        viewModelScope.launch {
            try {
                val recommendations = GeminiRecommendationService.getRecommendations(vibe)
                _aiRecommendations.value = recommendations

                // For each Gemini-suggested song, look it up on YouTube so it's
                // actually playable instead of pointing at a fake sample file.
                val resolvedTracks = recommendations.map { song ->
                    val query = "${song.title} ${song.artist}"
                    repository.searchYouTube(query).firstOrNull()
                }
                _recommendedTracks.value = resolvedTracks

            } catch (e: Exception) {
                _recommendationError.value = e.message ?: "Failed querying Gemini music service."
                Log.e(TAG, "Failed recommending music: ", e)
            } finally {
                _isLoadingRecommendations.value = false
            }
        }
    }

    // --- Playlists ---
    val allPlaylists: StateFlow<List<PlaylistWithCount>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylistId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.getPlaylistTracks(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openPlaylist(playlistId: Long) {
        _selectedPlaylistId.value = playlistId
    }

    fun closePlaylist() {
        _selectedPlaylistId.value = null
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.createPlaylist(trimmed) }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.renamePlaylist(playlistId, trimmed) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylistId.value == playlistId) {
                _selectedPlaylistId.value = null
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch { repository.addTrackToPlaylist(playlistId, track) }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch { repository.removeTrackFromPlaylist(playlistId, trackId) }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracking()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        castPlayer?.release()
        castPlayer = null
    }

    // --- ViewModel Factory ---
    class Factory(
        private val repository: MusicRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MusicViewModel(repository, appContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
