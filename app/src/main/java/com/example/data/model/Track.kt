package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val genre: String,
    val durationMs: Long,
    val isFavorite: Boolean = false,
    val vibeCode: String, // e.g., "synthwave", "lofi", "acoustic", "classical", "youtube"
    val lastPlayedAt: Long? = null,
    val youtubeVideoId: String? = null, // set when this track was found via YouTube search
    val thumbnailUrl: String? = null
)
