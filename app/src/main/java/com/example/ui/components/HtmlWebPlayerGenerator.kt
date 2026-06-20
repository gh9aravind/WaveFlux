package com.example.ui.components

import com.example.data.model.Track

object HtmlWebPlayerGenerator {

    fun generatePlayerHtml(
        tracks: List<Track>,
        userTier: String
    ): String {
        val tracksJson = tracks.joinToString(separator = ",\n") { track ->
            """
            {
                id: "${track.id}",
                title: "${track.title.replace("\"", "\\\"")}",
                artist: "${track.artist.replace("\"", "\\\"")}",
                vibe: "${track.vibeCode.replace("\"", "\\\"")}",
                genre: "${track.genre.replace("\"", "\\\"")}",
                isPlaying: false
            }
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>SoundSpot HTML Web Player</title>
            <style>
                :root {
                    --spotify-green: #1DB954;
                    --neon-blue: #00F0FF;
                    --neon-pink: #FF007F;
                    --bg-dark: #070417;
                    --glass: rgba(255, 255, 255, 0.05);
                    --glass-border: rgba(255, 255, 255, 0.1);
                }
                
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                }
                
                body {
                    background: radial-gradient(circle at 50% 0%, #17114d 0%, #060411 100%);
                    color: #ffffff;
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    padding: 24px;
                    overflow-x: hidden;
                }
                
                header {
                    text-align: center;
                    margin-bottom: 24px;
                }
                
                h1 {
                    font-size: 24px;
                    font-weight: 800;
                    background: linear-gradient(135deg, #ffffff 0%, var(--spotify-green) 100%);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    letter-spacing: -0.5px;
                }
                
                .subtitle {
                    font-size: 12px;
                    color: rgba(255, 255, 255, 0.6);
                    margin-top: 4px;
                    text-transform: uppercase;
                    letter-spacing: 2px;
                }
                
                .tier-badge {
                    display: inline-block;
                    margin-top: 8px;
                    padding: 4px 12px;
                    background: linear-gradient(135deg, var(--neon-pink), var(--neon-blue));
                    border-radius: 50px;
                    font-size: 10px;
                    font-weight: bold;
                    letter-spacing: 1px;
                    text-transform: uppercase;
                    box-shadow: 0 0 15px rgba(255, 0, 127, 0.4);
                }

                .player-container {
                    width: 100%;
                    max-width: 480px;
                    background: var(--glass);
                    backdrop-filter: blur(20px);
                    -webkit-backdrop-filter: blur(20px);
                    border: 1px solid var(--glass-border);
                    border-radius: 20px;
                    padding: 24px;
                    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5);
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                }

                /* Rotating Vinyl CD Art */
                .vinyl-deck {
                    position: relative;
                    width: 160px;
                    height: 160px;
                    margin-bottom: 20px;
                }

                .vinyl-record {
                    width: 100%;
                    height: 100%;
                    background: repeating-radial-gradient(
                        circle,
                        #111 0px,
                        #111 4px,
                        #222 5px,
                        #111 6px
                    );
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    box-shadow: 0 10px 25px rgba(0,0,0,0.6), 
                                inset 0 0 10px rgba(255,255,255,0.1);
                    animation: spin 3.5s linear infinite;
                    animation-play-state: paused;
                }

                .vinyl-record.playing {
                    animation-play-state: running;
                }

                .vinyl-center {
                    width: 50px;
                    height: 50px;
                    border-radius: 50%;
                    background: linear-gradient(135deg, #120e32, #1db954);
                    border: 4px solid #000;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }

                .vinyl-center::after {
                    content: '';
                    width: 10px;
                    height: 10px;
                    border-radius: 50%;
                    background: #fff;
                    box-shadow: 0 0 8px rgba(255,255,255,0.8);
                }

                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }

                .track-details {
                    text-align: center;
                    margin-bottom: 20px;
                    width: 100%;
                }

                .song-title {
                    font-size: 18px;
                    font-weight: 700;
                    margin-bottom: 6px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .song-artist {
                    font-size: 14px;
                    color: rgba(255, 255, 255, 0.6);
                }

                /* Retro Wave Visualizer Container */
                .visualizer-box {
                    width: 100%;
                    height: 50px;
                    background: rgba(0, 0, 0, 0.4);
                    border-radius: 10px;
                    margin-bottom: 20px;
                    display: flex;
                    align-items: flex-end;
                    justify-content: space-around;
                    padding: 8px;
                    overflow: hidden;
                    border: 1px solid rgba(255, 255, 255, 0.05);
                }

                .eq-bar {
                    width: 5%;
                    height: 10%;
                    background: linear-gradient(to top, var(--spotify-green), var(--neon-blue));
                    border-radius: 4px;
                    transition: height 0.1s ease;
                }

                /* Controls Layout */
                .playback-controls {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 20px;
                    width: 100%;
                    margin-bottom: 24px;
                }

                .btn {
                    background: none;
                    border: none;
                    cursor: pointer;
                    color: #ffffff;
                    outline: none;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    transition: transform 0.2s, color 0.2s;
                }

                .btn:active {
                    transform: scale(0.9);
                }

                .btn-secondary {
                    width: 44px;
                    height: 44px;
                    color: rgba(255, 255, 255, 0.7);
                }

                .btn-secondary:hover {
                    color: white;
                }

                .btn-primary {
                    width: 56px;
                    height: 56px;
                    background: var(--spotify-green);
                    border-radius: 50%;
                    color: #0d0d0d;
                    box-shadow: 0 4px 15px rgba(29, 185, 84, 0.4);
                }

                .btn-primary:hover {
                    transform: scale(1.05);
                }

                /* Seek Bar */
                .timeline-bar {
                    width: 100%;
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    font-size: 11px;
                    color: rgba(255, 255, 255, 0.5);
                    margin-bottom: 24px;
                }

                .progress-container {
                    flex: 1;
                    height: 4px;
                    background: rgba(255, 255, 255, 0.1);
                    border-radius: 10px;
                    position: relative;
                    cursor: pointer;
                }

                .progress-fill {
                    width: 35%;
                    height: 100%;
                    background: var(--spotify-green);
                    border-radius: 10px;
                    box-shadow: 0 0 8px var(--spotify-green);
                }

                /* Playlist Block */
                .playlist-header {
                    width: 100%;
                    display: flex;
                    justify-content: space-between;
                    font-size: 12px;
                    font-weight: 700;
                    margin-bottom: 12px;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                    color: var(--spotify-green);
                    border-bottom: 1px solid rgba(255,255,255,0.1);
                    padding-bottom: 6px;
                }

                .playlist-list {
                    width: 100%;
                    list-style: none;
                    max-height: 150px;
                    overflow-y: auto;
                }

                .playlist-item {
                    display: flex;
                    align-items: center;
                    padding: 8px 12px;
                    border-radius: 8px;
                    cursor: pointer;
                    font-size: 13px;
                    transition: background 0.2s;
                }

                .playlist-item:hover {
                    background: rgba(255, 255, 255, 0.05);
                }

                .playlist-item.active {
                    background: rgba(29, 185, 84, 0.15);
                    border-left: 3px solid var(--spotify-green);
                }

                .song-num {
                    width: 24px;
                    font-size: 11px;
                    color: rgba(255, 255, 255, 0.4);
                }

                .song-num.playing {
                    color: var(--spotify-green);
                    font-weight: bold;
                }

                .song-info {
                    flex: 1;
                }

                .song-item-title {
                    font-weight: 600;
                }

                .song-item-artist {
                    font-size: 11px;
                    color: rgba(255, 255, 255, 0.5);
                }

                .vibe-tag {
                    font-size: 10px;
                    padding: 2px 6px;
                    background: rgba(255,255,255,0.1);
                    border-radius: 4px;
                }

                /* Scrollbar */
                ::-webkit-scrollbar {
                    width: 4px;
                }
                ::-webkit-scrollbar-track {
                    background: transparent;
                }
                ::-webkit-scrollbar-thumb {
                    background: rgba(255, 255, 255, 0.2);
                    border-radius: 4px;
                }
            </style>
        </head>
        <body>
            <header>
                <h1>SoundSpot Web HTML5</h1>
                <div class="subtitle">Personalized Dynamic Space</div>
                <div class="tier-badge">$userTier</div>
            </header>

            <div class="player-container">
                <!-- CD Record Deck -->
                <div class="vinyl-deck">
                    <div id="record" class="vinyl-record">
                        <div class="vinyl-center"></div>
                    </div>
                </div>

                <!-- Track Description -->
                <div class="track-details">
                    <div id="songTitle" class="song-title">Select a Song</div>
                    <div id="songArtist" class="song-artist">To begin listening</div>
                </div>

                <!-- Live Animated Visualizer Bars -->
                <div class="visualizer-box">
                    <div class="eq-bar" style="height: 15%"></div>
                    <div class="eq-bar" style="height: 38%"></div>
                    <div class="eq-bar" style="height: 64%"></div>
                    <div class="eq-bar" style="height: 48%"></div>
                    <div class="eq-bar" style="height: 22%"></div>
                    <div class="eq-bar" style="height: 75%"></div>
                    <div class="eq-bar" style="height: 85%"></div>
                    <div class="eq-bar" style="height: 52%"></div>
                    <div class="eq-bar" style="height: 33%"></div>
                    <div class="eq-bar" style="height: 18%"></div>
                </div>

                <!-- Seek Timeline -->
                <div class="timeline-bar">
                    <span id="currentTime">0:00</span>
                    <div class="progress-container" id="progressContainer">
                        <div class="progress-fill" id="progressFill" style="width: 0%"></div>
                    </div>
                    <span id="totalTime">0:00</span>
                </div>

                <!-- Playback Controllers -->
                <div class="playback-controls">
                    <button class="btn btn-secondary" onclick="prevSong()">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>
                        </svg>
                    </button>
                    <button class="btn btn-primary" onclick="togglePlay()">
                        <svg id="playIcon" width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M8 5v14l11-7z"/>
                        </svg>
                    </button>
                    <button class="btn btn-secondary" onclick="nextSong()">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M6 18l8.5-6L6 6zm9-12h2v12h-2z"/>
                        </svg>
                    </button>
                </div>

                <!-- Playlist Box -->
                <div class="playlist-header">
                    <span>Library Playlist Cache</span>
                    <span style="font-weight: 500; font-size: 11px; text-transform: lowercase;">${tracks.size} item(s)</span>
                </div>
                
                <ul class="playlist-list" id="playlist">
                    <!-- Loaded programmatically -->
                </ul>
            </div>

            <script>
                // Live Tracks Data injected directly from Android Room Cache
                const playlistData = [
                    $tracksJson
                ];

                let currentTrackIndex = 0;
                let isPlaying = false;
                let currentTime = 0;
                let totalTime = 180; // mock default 3 min
                let progressInterval;
                let visualizerInterval;

                // Elements
                const record = document.getElementById('record');
                const songTitle = document.getElementById('songTitle');
                const songArtist = document.getElementById('songArtist');
                const playIcon = document.getElementById('playIcon');
                const progressFill = document.getElementById('progressFill');
                const timeLabel = document.getElementById('currentTime');
                const durationLabel = document.getElementById('totalTime');
                const playlistContainer = document.getElementById('playlist');

                // Render playlist list
                function renderPlaylist() {
                    playlistContainer.innerHTML = '';
                    playlistData.forEach((track, index) => {
                        const li = document.createElement('li');
                        li.className = 'playlist-item' + (index === currentTrackIndex ? ' active' : '');
                        li.onclick = () => selectSong(index);
                        
                        li.innerHTML = `
                            <span class="song-num" style="color: ${'$'}{index === currentTrackIndex ? 'var(--spotify-green)' : ''}">
                                ${'$'}{index === currentTrackIndex && isPlaying ? '▶' : index + 1}
                            </span>
                            <div class="song-info">
                                <div class="song-item-title">${'$'}{track.title}</div>
                                <div class="song-item-artist">${'$'}{track.artist}</div>
                            </div>
                            <span class="vibe-tag">${'$'}{track.vibe || 'Default'}</span>
                        `;
                        playlistContainer.appendChild(li);
                    });
                }

                function loadTrack(index) {
                    if (playlistData.length === 0) return;
                    currentTrackIndex = index;
                    const track = playlistData[index];
                    songTitle.textContent = track.title;
                    songArtist.textContent = track.artist;
                    
                    // Generate a random duration between 2:30 and 4:30
                    totalTime = Math.floor(Math.random() * 120) + 150;
                    currentTime = 0;
                    updateTimelineDisplay();
                    renderPlaylist();
                }

                // Pushed from native Android app via JavaScript Interface
                window.updateStateFromAndroid = function(trackId, isPlayingState, currentSecs, totalSecs) {
                    const index = playlistData.findIndex(t => t.id === trackId);
                    if (index !== -1) {
                        currentTrackIndex = index;
                        const track = playlistData[index];
                        songTitle.textContent = track.title;
                        songArtist.textContent = track.artist;
                        renderPlaylist();
                    }
                    
                    isPlaying = isPlayingState;
                    if (isPlaying) {
                        playIcon.setAttribute('d', 'M6 19h4V5H6v14zm8-14v14h4V5h-4z'); // Pause Icon SVG
                        record.classList.add('playing');
                        
                        // Begin Visualizer animation
                        startVisualizerEffect();
                        
                        // Local fallback progress simulation only if no real time is provided from native
                        if (currentSecs === undefined || currentSecs === 0) {
                            clearInterval(progressInterval);
                            progressInterval = setInterval(() => {
                                currentTime++;
                                if (currentTime >= totalTime) {
                                    nextSong();
                                } else {
                                    updateTimelineDisplay();
                                }
                            }, 1000);
                        }
                    } else {
                        playIcon.setAttribute('d', 'M8 5v14l11-7z'); // Play Icon SVG
                        record.classList.remove('playing');
                        clearInterval(progressInterval);
                        stopVisualizerEffect();
                        renderPlaylist();
                    }
                    
                    if (currentSecs !== undefined && totalSecs !== undefined && totalSecs > 0) {
                        currentTime = currentSecs;
                        totalTime = totalSecs;
                        updateTimelineDisplay();
                    }
                };

                function selectSong(index) {
                    if (playlistData.length === 0) return;
                    const track = playlistData[index];
                    if (window.AndroidApp && typeof window.AndroidApp.playTrack === 'function') {
                        window.AndroidApp.playTrack(track.id);
                    } else {
                        loadTrack(index);
                        isPlaying = true;
                        play();
                    }
                }

                function togglePlay() {
                    if (playlistData.length === 0) return;
                    if (window.AndroidApp && typeof window.AndroidApp.togglePlayPause === 'function') {
                        window.AndroidApp.togglePlayPause();
                    } else {
                        isPlaying = !isPlaying;
                        if (isPlaying) {
                            play();
                        } else {
                            pause();
                        }
                    }
                }

                function play() {
                    isPlaying = true;
                    playIcon.setAttribute('d', 'M6 19h4V5H6v14zm8-14v14h4V5h-4z'); // Pause Icon SVG
                    record.classList.add('playing');
                    
                    clearInterval(progressInterval);
                    progressInterval = setInterval(() => {
                        currentTime++;
                        if (currentTime >= totalTime) {
                            nextSong();
                        } else {
                            updateTimelineDisplay();
                        }
                    }, 1000);

                    startVisualizerEffect();
                }

                function pause() {
                    isPlaying = false;
                    playIcon.setAttribute('d', 'M8 5v14l11-7z'); // Play Icon SVG
                    record.classList.remove('playing');
                    clearInterval(progressInterval);
                    stopVisualizerEffect();
                    renderPlaylist();
                }

                function nextSong() {
                    if (playlistData.length === 0) return;
                    if (window.AndroidApp && typeof window.AndroidApp.playNext === 'function') {
                        window.AndroidApp.playNext();
                    } else {
                        let next = currentTrackIndex + 1;
                        if (next >= playlistData.length) next = 0;
                        selectSong(next);
                    }
                }

                function prevSong() {
                    if (playlistData.length === 0) return;
                    if (window.AndroidApp && typeof window.AndroidApp.playPrevious === 'function') {
                        window.AndroidApp.playPrevious();
                    } else {
                        let prev = currentTrackIndex - 1;
                        if (prev < 0) prev = playlistData.length - 1;
                        selectSong(prev);
                    }
                }

                function updateTimelineDisplay() {
                    const progressPercent = (currentTime / totalTime) * 100;
                    progressFill.style.width = progressPercent + '%';

                    const currentMin = Math.floor(currentTime / 60);
                    const currentSec = currentTime % 60;
                    timeLabel.textContent = currentMin + ':' + (currentSec < 10 ? '0' : '') + currentSec;

                    const totalMin = Math.floor(totalTime / 60);
                    const totalSec = totalTime % 60;
                    durationLabel.textContent = totalMin + ':' + (totalSec < 10 ? '0' : '') + totalSec;
                }

                function startVisualizerEffect() {
                    clearInterval(visualizerInterval);
                    const bars = document.querySelectorAll('.eq-bar');
                    visualizerInterval = setInterval(() => {
                        bars.forEach(bar => {
                            const newHeight = Math.floor(Math.random() * 80) + 15;
                            bar.style.height = newHeight + '%';
                        });
                    }, 120);
                }

                function stopVisualizerEffect() {
                    clearInterval(visualizerInterval);
                    const bars = document.querySelectorAll('.eq-bar');
                    bars.forEach((bar, index) => {
                        bar.style.height = (12 + (index * 4)) + '%';
                    });
                }

                // Make progress bar clickable
                document.getElementById('progressContainer').onclick = function(e) {
                    if (!isPlaying && currentTime === 0) return;
                    const rect = this.getBoundingClientRect();
                    const percentage = (e.clientX - rect.left) / this.offsetWidth;
                    currentTime = Math.floor(percentage * totalTime);
                    updateTimelineDisplay();
                };

                // Init
                if (playlistData.length > 0) {
                    loadTrack(0);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
