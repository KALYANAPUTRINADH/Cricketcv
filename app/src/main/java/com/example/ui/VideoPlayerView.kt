package com.example.ui

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerView(
    videoUrl: String,
    startTimeMs: Long,
    endTimeMs: Long,
    isClipMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoView: VideoView? by remember { mutableStateOf(null) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableStateOf(startTimeMs) }
    val coroutineScope = rememberCoroutineScope()

    val actualUrl = if (videoUrl.isNotEmpty()) videoUrl else "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    val actualStart = if (videoUrl.isNotEmpty()) 0L else startTimeMs
    val actualEnd = if (videoUrl.isNotEmpty()) (endTimeMs - startTimeMs) else endTimeMs

    LaunchedEffect(actualUrl) {
        videoView?.setVideoPath(actualUrl)
        videoView?.setOnPreparedListener { mp ->
            mediaPlayer = mp
            mp.isLooping = false
            videoView?.seekTo(actualStart.toInt())
            if (isPlaying) {
                videoView?.start()
            }
        }
    }

    LaunchedEffect(isPlaying, actualUrl, isClipMode, actualStart, actualEnd) {
        if (isPlaying) {
            while (isPlaying) {
                val pos = videoView?.currentPosition?.toLong() ?: 0L
                currentPos = pos

                if (isClipMode && pos >= actualEnd) {
                    videoView?.pause()
                    videoView?.seekTo(actualStart.toInt())
                    videoView?.start()
                }
                delay(100)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(actualUrl))
                            setOnPreparedListener { mp ->
                                mediaPlayer = mp
                                mp.isLooping = false
                                seekTo(actualStart.toInt())
                            }
                            setOnCompletionListener {
                                isPlaying = false
                            }
                        }.also { videoView = it }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view -> }
                )

                if (mediaPlayer == null) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Slider(
                    value = currentPos.toFloat().coerceIn(actualStart.toFloat(), actualEnd.toFloat()),
                    onValueChange = { value ->
                        coroutineScope.launch {
                            currentPos = value.toLong()
                            videoView?.seekTo(currentPos.toInt())
                        }
                    },
                    valueRange = actualStart.toFloat()..actualEnd.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(if (videoUrl.isNotEmpty()) currentPos else currentPos - startTimeMs),
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatTime(actualEnd - actualStart),
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val target = (videoView?.currentPosition ?: 0) - 3000
                    videoView?.seekTo(target.coerceAtLeast(actualStart.toInt()))
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Rewind 3 Seconds",
                        tint = Color(0xFF475569)
                    )
                }

                Button(
                    onClick = {
                        if (isPlaying) {
                            videoView?.pause()
                            isPlaying = false
                        } else {
                            if (videoView?.currentPosition ?: 0 >= actualEnd.toInt()) {
                                videoView?.seekTo(actualStart.toInt())
                            }
                            videoView?.start()
                            isPlaying = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(38.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isPlaying) "PAUSE" else "PLAY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = {
                    val target = (videoView?.currentPosition ?: 0) + 3000
                    videoView?.seekTo(target.coerceAtMost(actualEnd.toInt()))
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Forward 3 Seconds",
                        tint = Color(0xFF475569)
                    )
                }

                IconButton(onClick = {
                    videoView?.seekTo(actualStart.toInt())
                    videoView?.start()
                    isPlaying = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart clip",
                        tint = Color(0xFF475569)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000).toInt()
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return String.format("%02d.%01d", seconds, millis)
}
