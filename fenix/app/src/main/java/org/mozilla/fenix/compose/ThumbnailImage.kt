/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import mozilla.components.concept.base.images.ImageLoadRequest
import org.mozilla.fenix.components.components
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Thumbnail belonging to a [key]. Asynchronously fetches the bitmap from storage.
 *
 * @param key Key used to remember the thumbnail for future compositions.
 * @param size [Dp] size of the thumbnail.
 * @param modifier [Modifier] used to draw the image content.
 * @param contentScale [ContentScale] used to draw image content.
 * @param alignment [Alignment] used to draw the image content.
 */
@Composable
@Suppress("LongParameterList")
fun ThumbnailImage(
    key: String,
    size: Dp,
    modifier: Modifier,
    contentScale: ContentScale,
    alignment: Alignment,
    fallbackContent: @Composable () -> Unit,
) {
    if (inComposePreview) {
        Box(modifier = Modifier.background(color = FirefoxTheme.colors.layer3))
    } else {
        val thumbnailSize = LocalDensity.current.run { size.toPx().toInt() }
        val request = ImageLoadRequest(key, thumbnailSize)
        val storage = components.core.thumbnailStorage
        var state by rememberSaveable { mutableStateOf(ThumbnailImageState(null, false)) }
        val scope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            if (!state.hasLoaded) {
                scope.launch {
                    val thumbnailBitmap = storage.loadThumbnail(request).await()
                    thumbnailBitmap?.prepareToDraw()
                    state = ThumbnailImageState(
                        bitmap = thumbnailBitmap,
                        hasLoaded = true,
                    )
                }
            }

            onDispose {
                // Recycle the bitmap to liberate the RAM. Without this, a list of [ThumbnailImage]
                // will bloat the memory. This is a trade-off, however, as the bitmap
                // will be re-fetched if this Composable is disposed and re-loaded.
                state.bitmap?.recycle()
                state = ThumbnailImageState(
                    bitmap = null,
                    hasLoaded = false,
                )
            }
        }

        if (state.bitmap == null && state.hasLoaded) {
            fallbackContent()
        } else {
            state.bitmap?.let { bitmap ->
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = null,
                    modifier = modifier,
                    contentScale = contentScale,
                    alignment = alignment,
                )
            }
        }
    }
}

/**
 * State wrapper for [ThumbnailImage].
 */
@Parcelize
private data class ThumbnailImageState(
    val bitmap: Bitmap?,
    val hasLoaded: Boolean,
) : Parcelable

/**
 * This preview does not demo anything. This is to ensure that [ThumbnailImage] does not break other previews.
*/
@Preview
@Composable
private fun ThumbnailImagePreview() {
    FirefoxTheme {
        ThumbnailImage(
            key = "",
            size = 1.dp,
            modifier = Modifier,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            fallbackContent = {},
        )
    }
}
