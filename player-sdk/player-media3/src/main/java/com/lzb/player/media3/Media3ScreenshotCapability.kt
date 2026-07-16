package com.lzb.player.media3

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.lzb.player.api.PlayerController
import com.lzb.player.api.ScreenshotCapability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Media3 screenshot capability (V1.4).
 *
 * Copies the current frame from the bound [PlayerView] video surface via PixelCopy /
 * [TextureView.getBitmap]. Requires [Media3Engine.bindPlayerView] beforehand.
 */
class Media3ScreenshotCapability(
    private val engine: Media3Engine,
) : ScreenshotCapability {

    override val id: String = ScreenshotCapability.ID

    override fun onAttach(controller: PlayerController) = Unit

    override fun onDetach() = Unit

    override suspend fun capture(): Bitmap? = withContext(Dispatchers.Main.immediate) {
        val playerView = engine.boundPlayerViewOrNull() ?: return@withContext null
        captureFromPlayerView(playerView)
    }

    @OptIn(UnstableApi::class) private suspend fun captureFromPlayerView(playerView: PlayerView): Bitmap? {
        val surfaceView = playerView.videoSurfaceView
        val width = when {
            surfaceView != null && surfaceView.width > 0 -> surfaceView.width
            playerView.width > 0 -> playerView.width
            else -> 0
        }
        val height = when {
            surfaceView != null && surfaceView.height > 0 -> surfaceView.height
            playerView.height > 0 -> playerView.height
            else -> 0
        }
        if (width <= 0 || height <= 0) return null

        return when (surfaceView) {
            is TextureView -> surfaceView.getBitmap(width, height)
            is SurfaceView -> copySurfaceView(surfaceView, width, height)
            else -> null
        }
    }

    private suspend fun copySurfaceView(
        surfaceView: SurfaceView,
        width: Int,
        height: Int,
    ): Bitmap? {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return suspendCancellableCoroutine { cont ->
            val listener = PixelCopy.OnPixelCopyFinishedListener { result ->
                if (cont.isActive) {
                    if (result == PixelCopy.SUCCESS) {
                        cont.resume(bitmap)
                    } else {
                        bitmap.recycle()
                        cont.resume(null)
                    }
                } else {
                    bitmap.recycle()
                }
            }
            try {
                PixelCopy.request(
                    surfaceView,
                    bitmap,
                    listener,
                    Handler(Looper.getMainLooper()),
                )
            } catch (_: IllegalArgumentException) {
                bitmap.recycle()
                cont.resume(null)
            }
            cont.invokeOnCancellation {
                // Bitmap may still be in use by PixelCopy; recycle only if cancelled before finish
            }
        }
    }
}
