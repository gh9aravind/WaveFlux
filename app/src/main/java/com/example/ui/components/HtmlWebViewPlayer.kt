package com.example.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.Toast
import com.example.ui.theme.SpotifyGreen
import com.example.ui.MusicViewModel

@Composable
fun HtmlWebViewPlayer(
    viewModel: MusicViewModel,
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val trackDuration by viewModel.trackDuration.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Stream native Android media player events (song changes, play status, slider position) 
    // into the JavaScript context inside the WebView without reloading the page.
    LaunchedEffect(currentTrack, isPlaying, playbackPosition, trackDuration, webViewRef) {
        val webView = webViewRef ?: return@LaunchedEffect
        val trackId = currentTrack?.id ?: ""
        val posSeconds = playbackPosition / 1000
        val durSeconds = trackDuration / 1000
        val jsCmd = "if (typeof window.updateStateFromAndroid === 'function') { window.updateStateFromAndroid('$trackId', $isPlaying, $posSeconds, $durSeconds); }"
        webView.evaluateJavascript(jsCmd, null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 120.dp)
    ) {
        // Upper Quick Actions: Share or Copy Source Code to Clipboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(htmlContent))
                    Toast.makeText(context, "HTML5 Source Code copied to clipboard!", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("copy_html_code_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Player HTML", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, htmlContent)
                        type = "text/html"
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share SoundSpot HTML Web Player"))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x1EFFFFFF),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("share_html_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Web File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Web Client Canvas Container Representing Rendered HTML
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Color(0x1BFFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("html_webview_card")
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        setBackgroundColor(0)

                        // Register the bridge between Javascript and modern Jetpack Compose
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun playTrack(trackId: String) {
                                post {
                                    val tracksForPlay = viewModel.favoriteTracks.value
                                    val matchedTrack = tracksForPlay.find { it.id == trackId }
                                    if (matchedTrack != null) {
                                        viewModel.selectAndPlayTrack(matchedTrack, tracksForPlay)
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun togglePlayPause() {
                                post {
                                    viewModel.togglePlayPause()
                                }
                            }

                            @JavascriptInterface
                            fun playNext() {
                                post {
                                    viewModel.playNext()
                                }
                            }

                            @JavascriptInterface
                            fun playPrevious() {
                                post {
                                    viewModel.playPrevious()
                                }
                            }
                        }, "AndroidApp")

                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    // Store the reference when updated, bypassing repeated loadData triggers to avoid flickers
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
