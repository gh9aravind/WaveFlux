package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.android.gms.cast.framework.CastContext

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var localPlayer: ExoPlayer
    private var castPlayer: CastPlayer? = null

    override fun onCreate() {
        super.onCreate()
        localPlayer = ExoPlayer.Builder(this).build()

        val sessionIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = sessionIntent?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val sessionBuilder = MediaSession.Builder(this, localPlayer)
        if (sessionActivityPendingIntent != null) {
            sessionBuilder.setSessionActivity(sessionActivityPendingIntent)
        }
        mediaSession = sessionBuilder.build()

        // Cast support: when a Cast device connects, hand playback off to
        // CastPlayer (this is what actually sends title/artist/artwork/audio
        // to the TV/speaker - previously the Cast button had nothing wired
        // to it, hence "No media selected"). When it disconnects, control
        // returns to the local ExoPlayer at the same position.
        try {
            val castContext = CastContext.getSharedInstance(this)
            val cast = CastPlayer(castContext)
            cast.setSessionAvailabilityListener(object : SessionAvailabilityListener {
                override fun onCastSessionAvailable() {
                    switchToPlayer(cast)
                }
                override fun onCastSessionUnavailable() {
                    switchToPlayer(localPlayer)
                }
            })
            castPlayer = cast
        } catch (e: Exception) {
            // Cast framework unavailable on this device/build - local playback only.
            castPlayer = null
        }
    }

    private fun switchToPlayer(newPlayer: Player) {
        val session = mediaSession ?: return
        val currentPlayer = session.player
        if (currentPlayer === newPlayer) return

        val mediaItems: List<MediaItem> = (0 until currentPlayer.mediaItemCount).map {
            currentPlayer.getMediaItemAt(it)
        }
        val currentIndex = currentPlayer.currentMediaItemIndex
        val position = currentPlayer.currentPosition
        val playWhenReady = currentPlayer.playWhenReady

        if (mediaItems.isNotEmpty()) {
            newPlayer.setMediaItems(mediaItems, currentIndex.coerceIn(0, mediaItems.lastIndex), position)
            newPlayer.prepare()
            newPlayer.playWhenReady = playWhenReady
        }

        session.player = newPlayer
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
