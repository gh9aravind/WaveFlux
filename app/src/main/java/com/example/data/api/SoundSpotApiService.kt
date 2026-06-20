package com.example.data.api

import com.example.data.model.Track
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

/**
 * Production-ready API service interface modeling the integration points between 
 * the SoundSpot mobile application and the backend production tech stack:
 * - Backend: Node.js + Express
 * - Database: PostgreSQL (Tracks, Favorites, Playlists) + Redis (Cache)
 * - Search: Elasticsearch (Fuzzy & Boosted full-text search)
 * - Storage: AWS S3 + CloudFront CDN (Audio files delivery)
 * - Auth: Auth0 or Firebase Auth (JWT verification)
 */
interface SoundSpotApiService {

    // --- 1. Authentication & Profiling (Auth0 / Firebase Auth integration) ---
    @JsonClass(generateAdapter = true)
    data class UserProfile(
        val uid: String,
        val email: String,
        val displayName: String?,
        val customToken: String?,
        val subscriptionTier: String
    )

    @GET("api/v1/users/profile")
    suspend fun getUserProfile(): Response<UserProfile>

    // --- 2. Track Catalog & CloudFront Streaming (S3 backend delivery) ---
    @GET("api/v1/tracks")
    suspend fun getTracksCatalog(
        @Query("genre") genre: String? = null,
        @Query("vibeCode") vibeCode: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<List<Track>>

    @GET("api/v1/tracks/{id}")
    suspend fun getTrackDetails(@Path("id") trackId: String): Response<Track>

    // --- 3. Full-Text Search backed by Elasticsearch ---
    @JsonClass(generateAdapter = true)
    data class SearchRequest(
        val query: String,
        val fuzziness: String = "AUTO",
        val searchFields: List<String> = listOf("title^3", "artist^2", "genre") // Field boosting mappings
    )

    @JsonClass(generateAdapter = true)
    data class SearchResponse(
        val totalHits: Long,
        val tookMs: Long,
        val hits: List<Track>
    )

    @POST("api/v1/search")
    suspend fun searchTracks(@Body request: SearchRequest): Response<SearchResponse>

    // --- 4. User Interactions & Favorites Synchronization (PostgreSQL State) ---
    @JsonClass(generateAdapter = true)
    data class SyncFavoritesRequest(
        val favoriteIds: List<String>,
        val lastSyncTimestamp: Long
    )

    @JsonClass(generateAdapter = true)
    data class SyncFavoritesResponse(
        val syncedCount: Int,
        val updatedFavorites: List<Track>,
        val serverTimestamp: Long
    )

    @POST("api/v1/sync/favorites")
    suspend fun syncFavorites(@Body request: SyncFavoritesRequest): Response<SyncFavoritesResponse>

    @POST("api/v1/tracks/{id}/favorite")
    suspend fun favoriteTrack(@Path("id") trackId: String): Response<Unit>

    @DELETE("api/v1/tracks/{id}/favorite")
    suspend fun unfavoriteTrack(@Path("id") trackId: String): Response<Unit>

    // --- 5. Background Work, Metrics, Queueing (BullMQ ingestion) ---
    @JsonClass(generateAdapter = true)
    data class PlaybackEvent(
        val trackId: String,
        val action: String, // "play", "pause", "skip", "complete"
        val positionMs: Long,
        val timestamp: Long = System.currentTimeMillis()
    )

    @POST("api/v1/analytics/playback")
    suspend fun reportPlaybackActivity(@Body event: PlaybackEvent): Response<Unit>
}
