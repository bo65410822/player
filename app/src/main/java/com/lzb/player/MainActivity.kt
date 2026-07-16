package com.lzb.player

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.lzb.player.api.MediaSource
import com.lzb.player.api.PlayerController
import com.lzb.player.api.PlayerListener
import com.lzb.player.api.PlayerState
import com.lzb.player.core.speed.DefaultSpeedCapability
import com.lzb.player.core.subtitle.DefaultSubtitleCapability
import com.lzb.player.media3.Media3CacheCapability
import com.lzb.player.media3.Media3PlayerFactory
import com.lzb.player.media3.Media3ScreenshotCapability
import com.lzb.player.ui.theme.PlayerTheme
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

/**
 * Sample: HTTP/HLS/Asset/Local + Cache + Subtitle + Speed + Screenshot.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Media3Demo(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun Media3Demo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handle = remember { Media3PlayerFactory(context).create() }
    val controller = handle.controller
    val engine = handle.engine
    val cacheCapability = remember { Media3CacheCapability(context = context, engine = engine) }
    val subtitleCapability = remember { DefaultSubtitleCapability() }
    val speedCapability = remember { DefaultSpeedCapability(engine = engine) }
    val screenshotCapability = remember { Media3ScreenshotCapability(engine = engine) }
    var cachedBytes by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var pointAMs by remember { mutableStateOf<Long?>(null) }
    var loopEnabled by remember { mutableStateOf(controller.config.loop) }
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    var snapshotStatus by remember { mutableStateOf("Screenshot: -") }

    DisposableEffect(controller, cacheCapability, subtitleCapability, speedCapability, screenshotCapability) {
        controller.install(cacheCapability)
        controller.install(subtitleCapability)
        controller.install(speedCapability)
        controller.install(screenshotCapability)
        cachedBytes = cacheCapability.getCachedBytes()
        val progressListener = object : PlayerListener {
            override fun onProgress(pos: Long, dur: Long) {
                positionMs = pos
                durationMs = dur
            }
        }
        controller.addListener(progressListener)
        onDispose {
            controller.removeListener(progressListener)
            snapshot?.recycle()
            snapshot = null
            controller.release()
            cacheCapability.release()
        }
    }

    val playerState by controller.state.collectAsState()
    val currentSource by controller.currentSource.collectAsState()
    val currentCue by subtitleCapability.currentCue.collectAsState()
    val subtitleEnabled by subtitleCapability.enabled.collectAsState()
    val playbackSpeed by speedCapability.speed.collectAsState()
    val abRepeat by speedCapability.abRepeat.collectAsState()
    val canPlay = playerState == PlayerState.Prepared ||
        playerState == PlayerState.Paused ||
        playerState == PlayerState.Completed
    val canPause = playerState == PlayerState.Playing ||
        playerState == PlayerState.Buffering
    val canCapture = playerState == PlayerState.Playing ||
        playerState == PlayerState.Paused ||
        playerState == PlayerState.Prepared ||
        playerState == PlayerState.Completed

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Player SDK Demo", style = MaterialTheme.typography.titleLarge)
        Text(text = "State: ${playerState.name}", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Media: ${currentSource?.title ?: "None"}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Pos: ${formatMs(positionMs)} / ${formatMs(durationMs)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Speed: ${playbackSpeed}x | Loop: ${if (loopEnabled) "ON" else "OFF"} | AB: ${
                abRepeat?.let { "${formatMs(it.startMs)}-${formatMs(it.endMs)}" } ?: "OFF"
            }",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Cache: ${formatBytes(cachedBytes)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Subtitle: ${if (subtitleEnabled) "ON" else "OFF"}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = snapshotStatus,
            style = MaterialTheme.typography.bodyMedium,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).also { engine.bindPlayerView(it) }
                },
                onRelease = { view ->
                    engine.unbindPlayerView(view)
                },
            )
            val cueText = currentCue?.text
            if (!cueText.isNullOrBlank()) {
                Text(
                    text = cueText,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    controller.loadAndPrepare(
                        MediaSource.Url(url = DEMO_HTTP_MP4, title = "Demo HTTP MP4")
                    )
                },
            ) { Text("HTTP") }
            Button(
                onClick = {
                    controller.loadAndPrepare(
                        MediaSource.Url(url = DEMO_HLS, title = "Demo HLS")
                    )
                },
            ) { Text("HLS") }
            Button(
                onClick = {
                    controller.loadAndPrepare(
                        MediaSource.Asset(assetPath = DEMO_ASSET_PATH, title = "Demo Asset MP4")
                    )
                },
            ) { Text("Asset") }
            Button(
                onClick = {
                    val localFile = copyAssetToFilesDir(context, DEMO_ASSET_PATH, "demo_local.mp4")
                    controller.loadAndPrepare(
                        MediaSource.LocalFile(path = localFile.absolutePath, title = "Demo Local MP4")
                    )
                },
            ) { Text("Local") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { controller.play() }, enabled = canPlay) { Text("Play") }
            Button(onClick = { controller.pause() }, enabled = canPause) { Text("Pause") }
            Button(
                onClick = {
                    scope.launch {
                        snapshotStatus = "Screenshot: capturing..."
                        val bmp = screenshotCapability.capture()
                        val old = snapshot
                        snapshot = bmp
                        old?.recycle()
                        snapshotStatus = if (bmp != null) {
                            "Screenshot: ${bmp.width}x${bmp.height}"
                        } else {
                            "Screenshot: failed (bind view & play first)"
                        }
                    }
                },
                enabled = canCapture,
            ) { Text("Capture") }
        }

        val preview = snapshot
        if (preview != null && !preview.isRecycled) {
            Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = "Screenshot preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.DarkGray),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.5f, 1f, 1.5f, 2f).forEach { rate ->
                Button(onClick = { speedCapability.setSpeed(rate) }) {
                    Text("${rate}x")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val next = !loopEnabled
                    loopEnabled = next
                    controller.setConfig(controller.config.copy(loop = next))
                },
            ) { Text(if (loopEnabled) "Loop OFF" else "Loop ON") }
            Button(onClick = { pointAMs = positionMs }) { Text("Set A") }
            Button(
                onClick = {
                    val a = pointAMs
                    if (a != null && positionMs > a) {
                        speedCapability.setAbRepeat(a, positionMs)
                        loopEnabled = false
                        controller.setConfig(controller.config.copy(loop = false))
                    }
                },
            ) { Text("Set B") }
            Button(onClick = { speedCapability.clearAbRepeat() }) { Text("Clear AB") }
        }
        Text(
            text = "A: ${pointAMs?.let { formatMs(it) } ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val srt = context.assets.open(DEMO_SRT_PATH).bufferedReader().use { it.readText() }
                    subtitleCapability.loadSrt(srt, label = "Demo SRT", language = "en")
                    subtitleCapability.setEnabled(true)
                },
            ) { Text("Load SRT") }
            Button(
                onClick = {
                    val vtt = context.assets.open(DEMO_VTT_PATH).bufferedReader().use { it.readText() }
                    subtitleCapability.loadWebVtt(vtt, label = "Demo VTT", language = "en")
                    subtitleCapability.setEnabled(true)
                },
            ) { Text("Load VTT") }
            Button(
                onClick = { subtitleCapability.setEnabled(!subtitleEnabled) },
            ) { Text(if (subtitleEnabled) "Sub OFF" else "Sub ON") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    cacheCapability.preload(MediaSource.Url(url = DEMO_HTTP_MP4, title = "Preload HTTP"))
                    cachedBytes = cacheCapability.getCachedBytes()
                },
            ) { Text("Preload") }
            Button(onClick = { cachedBytes = cacheCapability.getCachedBytes() }) { Text("Refresh Cache") }
            Button(
                onClick = {
                    cacheCapability.clear()
                    cachedBytes = cacheCapability.getCachedBytes()
                },
            ) { Text("Clear Cache") }
        }
    }
}

private fun PlayerController.loadAndPrepare(source: MediaSource) {
    setMediaSource(source)
    prepare()
}

private fun copyAssetToFilesDir(
    context: Context,
    assetPath: String,
    outputName: String,
): File {
    val outFile = File(context.filesDir, outputName)
    if (outFile.exists() && outFile.length() > 0L) return outFile
    context.assets.open(assetPath).use { input ->
        FileOutputStream(outFile).use { output -> input.copyTo(output) }
    }
    return outFile
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}

private fun formatMs(ms: Long): String {
    if (ms < 0L) return "--:--"
    val totalSec = ms / 1000L
    val m = totalSec / 60L
    val s = totalSec % 60L
    return String.format("%d:%02d", m, s)
}

private const val DEMO_HTTP_MP4 =
    "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4"
/** Public HLS test stream (Mux). Replace with your own .m3u8 when needed. */
private const val DEMO_HLS =
    "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
private const val DEMO_ASSET_PATH = "media/demo.mp4"
private const val DEMO_SRT_PATH = "media/demo.srt"
private const val DEMO_VTT_PATH = "media/demo.vtt"