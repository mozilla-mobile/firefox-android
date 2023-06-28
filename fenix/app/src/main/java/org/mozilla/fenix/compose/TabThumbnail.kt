/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import org.mozilla.fenix.theme.FirefoxTheme

private const val THUMBNAIL_SIZE = 108
private const val FALLBACK_ICON_SIZE = 36

/**
 * Thumbnail belonging to a [tab]. If a thumbnail is not available, the favicon
 * will be displayed until the thumbnail is loaded.
 *
 * @param tab The given [TabSessionState] to render a thumbnail for.
 * @param size [Dp] size of the thumbnail.
 * @param backgroundColor [Color] used for the background of the favicon.
 * @param modifier [Modifier] used to draw the image content.
 * @param contentDescription Text used by accessibility services
 * to describe what this image represents.
 * @param contentScale [ContentScale] used to draw image content.
 * @param alignment [Alignment] used to draw the image content.
 */
@Composable
@Suppress("LongParameterList")
fun TabThumbnail(
    tab: TabSessionState,
    modifier: Modifier = Modifier,
    size: Dp = THUMBNAIL_SIZE.dp,
    backgroundColor: Color = FirefoxTheme.colors.layer2,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.FillWidth,
    alignment: Alignment = Alignment.TopCenter,
) {
    Card(
        modifier = modifier,
        backgroundColor = backgroundColor,
    ) {
        ThumbnailImage(
            key = tab.id,
            size = size,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment,
        ) {
            Box(
                modifier = Modifier.size(FALLBACK_ICON_SIZE.dp),
                contentAlignment = Alignment.Center,
            ) {
                val icon = tab.content.icon
                if (icon != null) {
                    icon.prepareToDraw()
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .size(FALLBACK_ICON_SIZE.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = contentScale,
                    )
                } else {
                    Favicon(
                        url = tab.content.url,
                        size = FALLBACK_ICON_SIZE.dp,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ThumbnailCardPreview() {
    FirefoxTheme {
        TabThumbnail(
            tab = createTab(url = "www.mozilla.com", title = "Mozilla"),
            modifier = Modifier
                .size(THUMBNAIL_SIZE.dp, 80.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}
