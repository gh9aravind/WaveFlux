package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.MusicDao
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.PlaylistWithCount
import com.example.data.model.AlbumResult
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao
) {
    private val TAG = "MusicRepository"
    private val okHttpClient = OkHttpClient()

    val allTracks: Flow<List<Track>> = musicDao.getAllTracks()
    val downloadedTracks: Flow<List<Track>> = musicDao.getDownloadedTracks()
    val favoriteTracks: Flow<List<Track>> = musicDao.getFavoriteTracks()
    val recentlyPlayed: Flow<List<Track>> = musicDao.getRecentlyPlayedTracks()

    suspend fun preloadDefaultTracksIfEmpty() {
        val defaultTracks = listOf(
            Track(
                id = "synth-neon",
                title = "Neon Nights Grid",
                artist = "Synthwave Rider",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                genre = "Synthwave",
                durationMs = 372000L, // ~6 mins
                vibeCode = "synthwave"
            ),
            Track(
                id = "lofi-coffee",
                title = "Midnight Cozy Coffee",
                artist = "Retro LoFi Beats",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                genre = "Lofi",
                durationMs = 423000L, // ~7 mins
                vibeCode = "lofi"
            ),
            Track(
                id = "acoustic-road",
                title = "Cabin Porch Sunset",
                artist = "Cabin Harmony",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                genre = "Acoustic",
                durationMs = 302000L,
                vibeCode = "acoustic"
            ),
            Track(
                id = "classic-nocturne",
                title = "Nocturne Piano Whispers",
                artist = "Echoes of Classicism",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                genre = "Classical",
                durationMs = 302000L,
                vibeCode = "classical"
            ),
            Track(
                id = "lofi-dream",
                title = "Suburban Cloud Dreamer",
                artist = "Lofi Dreamer",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                genre = "Lofi",
                durationMs = 362000L,
                vibeCode = "lofi"
            ),
            Track(
                id = "synth-laser",
                title = "Hyperdrive Laser Grid",
                artist = "Electro Laser Crew",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                genre = "Synthwave",
                durationMs = 401000L,
                vibeCode = "synthwave"
            )
        )
        musicDao.insertInitialTracks(defaultTracks)
    }

    /**
     * Searches YouTube Music for [query] using NewPipeExtractor and returns
     * Track placeholders. Stream URLs are NOT resolved here - call
     * [resolvePlayableUrl] right before playback since YouTube stream URLs expire.
     */
    suspend fun searchYouTube(query: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            com.example.data.youtube.YouTubeMusicService.search(query)
        } catch (e: Exception) {
            Log.e(TAG, "Failed communicating with remote Elasticsearch indexing manager: ${e.message}")
            emptyList()
        }
    }

    // --- Playlists ---
    val allPlaylists: Flow<List<PlaylistWithCount>> = musicDao.getAllPlaylistsWithCount()

    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> = musicDao.getTracksForPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        musicDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        musicDao.renamePlaylist(playlistId, newName)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        musicDao.clearPlaylistTracks(playlistId)
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) = withContext(Dispatchers.IO) {
        val existing = musicDao.getTrackById(track.id)
        if (existing == null) {
            musicDao.insertInitialTracks(listOf(track))
        }
        musicDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackId = track.id))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        musicDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    /**
     * Returns a fresh, directly-playable stream URL for [track].
     * - If the track was downloaded, returns the local file path.
     * - If it's a YouTube track, resolves a brand-new stream URL (these expire
     *   after a few hours, so we never reuse a stored one).
     * - Otherwise falls back to the track's stored [Track.streamUrl].
     */
    suspend fun resolvePlayableUrl(track: Track): String? = withContext(Dispatchers.IO) {
        if (track.isDownloaded && track.localFilePath != null) {
            return@withContext track.localFilePath
        }
        val videoId = track.youtubeVideoId
        if (videoId != null) {
            return@withContext try {
                com.example.data.youtube.YouTubeMusicService.resolveAudioStreamUrl(videoId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed resolving YouTube stream for ${track.title}: ${e.message}")
                null
            }
        }
        track.streamUrl
    }
/** Pre-resolves stream URLs for upcoming tracks in the background, before the user taps play. */
    suspend fun prefetchStreams(tracks: List<Track>) = withContext(Dispatchers.IO) {
        val videoIds = tracks.mapNotNull { it.youtubeVideoId }
        com.example.data.youtube.YouTubeMusicService.prefetch(videoIds)
    }
    /**
     * Toggles favorite status for [track], inserting it into the local database
     * first if it doesn't exist yet (e.g. a track that only came from a YouTube
     * search result and was never persisted before).
     */
    suspend fun toggleFavoriteTrack(track: Track) = withContext(Dispatchers.IO) {
        val existing = musicDao.getTrackById(track.id)
        val newStatus = !track.isFavorite
        if (existing == null) {
            musicDao.insertInitialTracks(listOf(track.copy(isFavorite = newStatus)))
        } else {
            musicDao.updateFavoriteStatus(track.id, newStatus)
        }
    }

    suspend fun getTrackById(id: String): Track? {
        return musicDao.getTrackById(id)
    }

    suspend fun insertCustomTrack(track: Track) {
        musicDao.insertInitialTracks(listOf(track))
    }

    suspend fun toggleFavorite(id: String, isFav: Boolean) {
        musicDao.updateFavoriteStatus(id, isFav)
    }

    suspend fun updateLastPlayed(id: String) {
        musicDao.updateLastPlayedAt(id, System.currentTimeMillis())
    }

    suspend fun downloadTrack(trackId: String, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val track = musicDao.getTrackById(trackId) ?: return@withContext false
        val url = resolvePlayableUrl(track) ?: run {
            Log.e(TAG, "Could not resolve a download URL for ${track.title}")
            return@withContext false
        }

        val downloadsDir = File(context.filesDir, "downloads")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val destinationFile = File(downloadsDir, "${track.id}.mp3")
        Log.d(TAG, "Downloading track ${track.title} from $url to ${destinationFile.absolutePath}")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed downloading file, server returned code ${response.code}")
                    return@withContext false
                }

                val responseBody = response.body ?: return@withContext false
                val totalBytes = responseBody.contentLength()
                
                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloadedBytes = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                onProgress(progress)
                            }
                        }
                    }
                }

                Log.d(TAG, "Downloaded track successfully: ${destinationFile.absolutePath}")
                musicDao.updateDownloadStatus(track.id, true, destinationFile.absolutePath)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading song ${track.title}", e)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            return@withContext false
        }
    }

    suspend fun removeOfflineDownload(trackId: String) = withContext(Dispatchers.IO) {
        val track = musicDao.getTrackById(trackId) ?: return@withContext
        track.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        musicDao.updateDownloadStatus(trackId, false, null)
    }

    /**
     * Hits the high-availability Node.js/Express API Gateway endpoint
     * which routes fuzzy queries into the Elasticsearch cluster.
     */
    suspend fun searchTracksElastic(query: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            val response = com.example.data.api.ProductionStackManager.apiService.searchTracks(
                com.example.data.api.SoundSpotApiService.SearchRequest(query = query)
            )
            if (response.isSuccessful) {
                response.body()?.hits ?: emptyList()
            } else {
                Log.w(TAG, "Elasticsearch cluster request failed with code: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed communicating with remote Elasticsearch indexing manager: ${e.message}")
            emptyList()
        }
    }
}
