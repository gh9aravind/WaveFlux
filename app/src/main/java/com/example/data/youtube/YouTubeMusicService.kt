package com.example.data.youtube

import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.TimeUnit

/**
 * Lightweight, standalone YouTube search + audio-stream resolver.
 *
 * Ported/simplified from the "Eco YouTube Music" Echo extension. That project
 * leans on ytm-kt + NewPipeExtractor together with a three-tier fallback chain
 * because it needs to work inside Echo's plugin model. Here we only need to
 * search and play audio, so we use NewPipeExtractor directly which keeps the
 * dependency surface small and easy to build standalone.
 *
 * IMPORTANT: YouTube's signed stream URLs expire (a few hours). Never persist
 * [resolveAudioStreamUrl] results for long-term storage - always resolve a
 * fresh URL right before playback.
 */
object YouTubeMusicService {

    private val streamCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
    private const val CACHE_TTL_MS = 4 * 60 * 60 * 1000L // 4 hours
    @Volatile
    private var initialized = false

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build()
    }

    /** Must be called once before any search/resolve call. Safe to call multiple times. */
    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            NewPipe.init(object : Downloader() {
                override fun execute(request: Request): Response {
                    val builder = okhttp3.Request.Builder().url(request.url())
                    request.headers().forEach { (name, values) ->
                        values.forEach { value -> builder.addHeader(name, value) }
                    }
                    request.dataToSend()?.let { data ->
                        builder.post(data.toRequestBody(null))
                    }

                    val response = runBlocking(Dispatchers.IO) {
                        httpClient.newCall(builder.build()).execute()
                    }
                    val body = response.body?.string()
                    return Response(
                        response.code,
                        response.message,
                        response.headers.toMultimap(),
                        body,
                        response.request.url.toString()
                    )
                }
            })

            initialized = true
        }
    }

    /**
     * Searches YouTube (Music) for [query] and maps the results into [Track]s.
     * The returned tracks have an empty [Track.streamUrl] - call
     * [resolveAudioStreamUrl] with [Track.youtubeVideoId] right before playback.
     */
    suspend fun search(query: String, limit: Int = 25): List<Track> = withContext(Dispatchers.IO) {
        ensureInitialized()
        try {
            // "music_songs" content filter biases results towards official YT Music
            // tracks rather than arbitrary videos.
            val extractor = ServiceList.YouTube.getSearchExtractor(
                query,
                listOf("music_songs"),
                ""
            )
            extractor.fetchPage()

            extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .take(limit)
                .mapNotNull { item -> item.toTrackOrNull() }
        } catch (e: ExtractionException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun StreamInfoItem.toTrackOrNull(): Track? {
        val videoId = extractVideoId(this.url) ?: return null
        return Track(
            id = "yt-$videoId",
            title = this.name ?: "Unknown title",
            artist = this.uploaderName ?: "Unknown artist",
            streamUrl = "", // resolved lazily at play-time, see resolveAudioStreamUrl
            genre = "YouTube",
            durationMs = this.duration.coerceAtLeast(0) * 1000L,
            vibeCode = "youtube",
            youtubeVideoId = videoId,
            thumbnailUrl = this.thumbnails.maxByOrNull { it.height * it.width }?.url
        )
    }

    private fun extractVideoId(watchUrl: String): String? {
        val regex = Regex("(?:v=|/)([0-9A-Za-z_-]{11})(?:[&?/]|$)")
        return regex.find(watchUrl)?.groupValues?.getOrNull(1)
    }

    /**
     * Resolves a fresh, directly-playable audio stream URL for [videoId].
     * Picks the highest-bitrate audio-only stream available. Throws on failure;
     * callers should catch and surface a friendly error.
     */
    suspend fun resolveAudioStreamUrl(videoId: String): String = withContext(Dispatchers.IO) {
        streamCache[videoId]?.let { (cachedUrl, expiry) ->
            if (System.currentTimeMillis() < expiry) return@withContext cachedUrl
        }

        ensureInitialized()
        val url = "https://www.youtube.com/watch?v=$videoId"
        val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, url)

        val bestAudio = streamInfo.audioStreams
            .filter { it.isUrl && !it.content.isNullOrEmpty() }
            .maxByOrNull { it.averageBitrate }
            ?: throw IllegalStateException("No playable audio stream found for $videoId")

        val resolvedUrl = bestAudio.content!!
        streamCache[videoId] = resolvedUrl to (System.currentTimeMillis() + CACHE_TTL_MS)
        resolvedUrl
    }

    /** Resolves stream URLs for several videos ahead of time, ignoring individual failures. */
    suspend fun prefetch(videoIds: List<String>) = withContext(Dispatchers.IO) {
        videoIds.forEach { id ->
            try {
                resolveAudioStreamUrl(id)
            } catch (e: Exception) {
                // Ignore - playback will just retry when actually requested
            }
        }
    }
