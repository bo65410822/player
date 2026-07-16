package com.lzb.player.media3

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.lzb.player.api.MediaSource
import java.io.File

/**
 * Maps SDK [MediaSource] to Media3 [MediaItem].
 *
 * - [MediaSource.Url] → http(s) Uri (HLS .m3u8 when mime is set / module present)
 * - [MediaSource.LocalFile] → file Uri
 * - [MediaSource.Asset] → asset:/// Uri
 */
internal object MediaSourceMapper {

    fun toMediaItem(source: MediaSource): MediaItem {
        val uri: Uri = when (source) {
            is MediaSource.Url -> source.url.toUri()
            is MediaSource.LocalFile -> toFileUri(source.path)
            is MediaSource.Asset -> toAssetUri(source.assetPath)
        }

        val builder = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(uri.toString())

        if (source is MediaSource.Url && looksLikeHls(source.url)) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }

        source.title?.let { title ->
            builder.setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build()
            )
        }

        return builder.build()
    }

    private fun looksLikeHls(url: String): Boolean {
        val path = url.substringBefore('#').substringBefore('?').lowercase()
        return path.endsWith(".m3u8")
    }

    private fun toFileUri(path: String): Uri {
        val file = File(path)
        require(file.exists()) { "Local file not found: $path" }
        return Uri.fromFile(file)
    }

    private fun toAssetUri(assetPath: String): Uri {
        val normalized = assetPath.trimStart('/')
        return "asset:///$normalized".toUri()
    }
}
