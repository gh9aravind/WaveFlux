package com.example.data.local

import androidx.room.*
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.PlaylistWithCount
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1")
    fun getDownloadedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 20")
    fun getRecentlyPlayedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Query("UPDATE tracks SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun updateLastPlayedAt(id: String, timestamp: Long)

    @Query("UPDATE tracks SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFav: Boolean)

    @Query("UPDATE tracks SET isDownloaded = :isDownloaded, localFilePath = :filePath WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, filePath: String?)

    // --- Playlists ---
    @Query("""
        SELECT p.id as id, p.name as name, p.createdAt as createdAt, COUNT(x.trackId) as trackCount
        FROM playlists p
        LEFT JOIN playlist_track_cross_ref x ON x.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    fun getAllPlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_track_cross_ref x ON x.trackId = t.id
        WHERE x.playlistId = :playlistId
        ORDER BY x.addedAt DESC
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)
}
