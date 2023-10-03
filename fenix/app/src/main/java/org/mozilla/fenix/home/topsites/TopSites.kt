/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.feature.top.sites.TopSite
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.ContextualMenu
import org.mozilla.fenix.compose.Favicon
import org.mozilla.fenix.compose.MenuItem
import org.mozilla.fenix.compose.PagerIndicator
import org.mozilla.fenix.compose.annotation.LightDarkPreview
import org.mozilla.fenix.ext.bitmapForUrl
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.wallpapers.WallpaperState
import kotlin.math.ceil
import org.mozilla.fenix.GleanMetrics.TopSites as TopSitesMetrics

private const val TOP_SITES_PER_PAGE = 8
private const val TOP_SITES_PER_ROW = 4
private const val TOP_SITES_ITEM_SIZE = 84
private const val TOP_SITES_ROW_WIDTH = TOP_SITES_PER_ROW * TOP_SITES_ITEM_SIZE
private const val TOP_SITES_FAVICON_CARD_SIZE = 60
private const val TOP_SITES_FAVICON_SIZE = 36

/**
 * A list of top sites.
 *
 * @param topSites List of [TopSite] to display.
 * @param topSiteColors The color set defined by [TopSiteColors] used to style a top site.
 * @param onTopSiteClick Invoked when the user clicks on a top site.
 * @param onTopSiteLongClick Invoked when the user long clicks on a top site.
 * @param onOpenInPrivateTabClicked Invoked when the user clicks on the "Open in private tab"
 * menu item.
 * @param onRenameTopSiteClicked Invoked when the user clicks on the "Rename" menu item.
 * @param onRemoveTopSiteClicked Invoked when the user clicks on the "Remove" menu item.
 * @param onSettingsClicked Invoked when the user clicks on the "Settings" menu item.
 * @param onSponsorPrivacyClicked Invoked when the user clicks on the "Our sponsors & your privacy"
 * menu item.
 * @param onTopSitesItemBound Invoked during the composition of a top site item.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
@Suppress("LongParameterList", "LongMethod")
fun TopSites(
    topSites: List<TopSite>,
    topSiteColors: TopSiteColors = TopSiteColors.colors(),
    onTopSiteClick: (TopSite) -> Unit,
    onTopSiteLongClick: (TopSite) -> Unit,
    onOpenInPrivateTabClicked: (topSite: TopSite) -> Unit,
    onRenameTopSiteClicked: (topSite: TopSite) -> Unit,
    onRemoveTopSiteClicked: (topSite: TopSite) -> Unit,
    onSettingsClicked: () -> Unit,
    onSponsorPrivacyClicked: () -> Unit,
    onTopSitesItemBound: () -> Unit,
) {
    val pageCount = ceil((topSites.size.toDouble() / TOP_SITES_PER_PAGE)).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                testTagsAsResourceId = true
            }
            .testTag(TopSitesTestTag.topSites),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val pagerState = rememberPagerState(
            pageCount = { pageCount },
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pagerState,
            ) { page ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val topSitesWindows = topSites.windowed(
                        size = TOP_SITES_PER_PAGE,
                        step = TOP_SITES_PER_PAGE,
                        partialWindows = true,
                    )[page].chunked(TOP_SITES_PER_ROW)

                    for (items in topSitesWindows) {
                        Row(modifier = Modifier.defaultMinSize(minWidth = TOP_SITES_ROW_WIDTH.dp)) {
                            items.forEachIndexed { position, topSite ->
                                TopSiteItem(
                                    topSite = topSite,
                                    menuItems = getMenuItems(
                                        topSite = topSite,
                                        onOpenInPrivateTabClicked = onOpenInPrivateTabClicked,
                                        onRenameTopSiteClicked = onRenameTopSiteClicked,
                                        onRemoveTopSiteClicked = onRemoveTopSiteClicked,
                                        onSettingsClicked = onSettingsClicked,
                                        onSponsorPrivacyClicked = onSponsorPrivacyClicked,
                                    ),
                                    position = position,
                                    topSiteColors = topSiteColors,
                                    onTopSiteClick = { item -> onTopSiteClick(item) },
                                    onTopSiteLongClick = onTopSiteLongClick,
                                    onTopSitesItemBound = onTopSitesItemBound,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        if (pagerState.pageCount > 1) {
            Spacer(modifier = Modifier.height(8.dp))

            PagerIndicator(
                pagerState = pagerState,
                modifier = Modifier.padding(horizontal = 16.dp),
                spacing = 4.dp,
            )
        }
    }
}

/**
 * Represents the colors used by top sites.
 */
data class TopSiteColors(
    val titleTextColor: Color,
    val sponsoredTextColor: Color,
    val faviconCardBackgroundColor: Color,
) {
    companion object {
        /**
         * Builder function used to construct an instance of [TopSiteColors].
         */
        @Composable
        fun colors(
            titleTextColor: Color = FirefoxTheme.colors.textPrimary,
            sponsoredTextColor: Color = FirefoxTheme.colors.textSecondary,
            faviconCardBackgroundColor: Color = FirefoxTheme.colors.layer2,
        ) = TopSiteColors(
            titleTextColor = titleTextColor,
            sponsoredTextColor = sponsoredTextColor,
            faviconCardBackgroundColor = faviconCardBackgroundColor,
        )

        /**
         * Builder function used to construct an instance of [TopSiteColors] given a
         * [WallpaperState].
         */
        @Composable
        fun colors(wallpaperState: WallpaperState): TopSiteColors {
            val textColor: Long? = wallpaperState.currentWallpaper.textColor
            val (titleTextColor, sponsoredTextColor) = if (textColor == null) {
                FirefoxTheme.colors.textPrimary to FirefoxTheme.colors.textSecondary
            } else {
                Color(textColor) to Color(textColor)
            }

            var faviconCardBackgroundColor = FirefoxTheme.colors.layer2

            wallpaperState.composeRunIfWallpaperCardColorsAreAvailable { cardColorLight, cardColorDark ->
                faviconCardBackgroundColor = if (isSystemInDarkTheme()) {
                    cardColorDark
                } else {
                    cardColorLight
                }
            }

            return TopSiteColors(
                titleTextColor = titleTextColor,
                sponsoredTextColor = sponsoredTextColor,
                faviconCardBackgroundColor = faviconCardBackgroundColor,
            )
        }
    }
}

/**
 * A top site item.
 *
 * @param topSite The [TopSite] to display.
 * @param menuItems List of [MenuItem]s to display in a top site dropdown menu.
 * @param position The position of the top site.
 * @param topSiteColors The color set defined by [TopSiteColors] used to style a top site.
 * @param onTopSiteClick Invoked when the user clicks on a top site.
 * @param onTopSiteLongClick Invoked when the user long clicks on a top site.
 * @param onTopSitesItemBound Invoked during the composition of a top site item.
 */
@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun TopSiteItem(
    topSite: TopSite,
    menuItems: List<MenuItem>,
    position: Int,
    topSiteColors: TopSiteColors,
    onTopSiteClick: (TopSite) -> Unit,
    onTopSiteLongClick: (TopSite) -> Unit,
    onTopSitesItemBound: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .semantics {
                testTagsAsResourceId = true
            }
            .testTag(TopSitesTestTag.topSiteItemRoot),
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onTopSiteClick(topSite) },
                    onLongClick = {
                        onTopSiteLongClick(topSite)
                        menuExpanded = true
                    },
                )
                .width(TOP_SITES_ITEM_SIZE.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            TopSiteFaviconCard(
                topSite = topSite,
                backgroundColor = topSiteColors.faviconCardBackgroundColor,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.width(TOP_SITES_ITEM_SIZE.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (topSite is TopSite.Pinned || topSite is TopSite.Default) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_new_pin),
                        contentDescription = null,
                    )

                    Spacer(modifier = Modifier.width(2.dp))
                }

                Text(
                    modifier = Modifier
                        .semantics {
                            testTagsAsResourceId = true
                        }
                        .testTag(TopSitesTestTag.topSiteTitle),
                    text = topSite.title ?: topSite.url,
                    color = topSiteColors.titleTextColor,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = FirefoxTheme.typography.caption,
                )
            }

            Text(
                text = stringResource(id = R.string.top_sites_sponsored_label),
                modifier = Modifier
                    .width(TOP_SITES_ITEM_SIZE.dp)
                    .alpha(alpha = if (topSite is TopSite.Provided) 1f else 0f),
                color = topSiteColors.sponsoredTextColor,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        ContextualMenu(
            modifier = Modifier
                .testTag(TopSitesTestTag.topSiteContextualMenu),
            menuItems = menuItems,
            showMenu = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        )

        if (topSite is TopSite.Provided) {
            LaunchedEffect(topSite) {
                submitTopSitesImpressionPing(topSite = topSite, position = position)
            }
        }
    }

    LaunchedEffect(Unit) {
        onTopSitesItemBound()
    }
}

/**
 * The top site favicon card.
 *
 * @param topSite The [TopSite] to display.
 * @param backgroundColor The background [Color] of the card.
 */
@Composable
private fun TopSiteFaviconCard(
    topSite: TopSite,
    backgroundColor: Color,
) {
    Card(
        modifier = Modifier.size(TOP_SITES_FAVICON_CARD_SIZE.dp),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = backgroundColor,
        elevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(TOP_SITES_FAVICON_SIZE.dp),
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp),
            ) {
                if (topSite is TopSite.Provided) {
                    FaviconBitmap(topSite)
                } else {
                    FavIconForUrl(topSite.url)
                }
            }
        }
    }
}

@Composable
private fun FaviconImage(painter: Painter) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.size(TOP_SITES_FAVICON_SIZE.dp),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun FaviconBitmap(topSite: TopSite.Provided) {
    var faviconBitmapUiState by remember { mutableStateOf<FaviconBitmapUiState>(FaviconBitmapUiState.Loading) }

    val client = LocalContext.current.components.core.client

    LaunchedEffect(topSite.imageUrl) {
        val bitmapForUrl = client.bitmapForUrl(topSite.imageUrl)

        faviconBitmapUiState = if (bitmapForUrl == null) {
            FaviconBitmapUiState.Error
        } else {
            FaviconBitmapUiState.Data(bitmapForUrl.asImageBitmap())
        }
    }

    when (val uiState = faviconBitmapUiState) {
        is FaviconBitmapUiState.Data -> FaviconImage(BitmapPainter(uiState.imageBitmap))
        is FaviconBitmapUiState.Error -> FaviconDefault(topSite.url)
        is FaviconBitmapUiState.Loading -> {
            // no-op
            // Don't update the icon while loading else the top site icon could have a 'flashing' effect
            // caused by the 'place holder letter' icon being immediately updated with the desired bitmap.
        }
    }
}

private sealed class FaviconBitmapUiState {
    data class Data(val imageBitmap: ImageBitmap) : FaviconBitmapUiState()
    object Loading : FaviconBitmapUiState()
    object Error : FaviconBitmapUiState()
}

@Composable
private fun FaviconDefault(url: String) {
    Favicon(url = url, size = TOP_SITES_FAVICON_SIZE.dp)
}

@Composable
private fun FavIconForUrl(url: String) {
    when (url) {
        SupportUtils.POCKET_TRENDING_URL -> FaviconImage(painterResource(R.drawable.ic_pocket))
        SupportUtils.BAIDU_URL -> FaviconImage(painterResource(R.drawable.ic_baidu))
        SupportUtils.JD_URL -> FaviconImage(painterResource(R.drawable.ic_jd))
        SupportUtils.PDD_URL -> FaviconImage(painterResource(R.drawable.ic_pdd))
        SupportUtils.TC_URL -> FaviconImage(painterResource(R.drawable.ic_tc))
        SupportUtils.MEITUAN_URL -> FaviconImage(painterResource(R.drawable.ic_meituan))
        else -> FaviconDefault(url)
    }
}

@Composable
@Suppress("LongParameterList")
private fun getMenuItems(
    topSite: TopSite,
    onOpenInPrivateTabClicked: (topSite: TopSite) -> Unit,
    onRenameTopSiteClicked: (topSite: TopSite) -> Unit,
    onRemoveTopSiteClicked: (topSite: TopSite) -> Unit,
    onSettingsClicked: () -> Unit,
    onSponsorPrivacyClicked: () -> Unit,
): List<MenuItem> {
    val isPinnedSite = topSite is TopSite.Pinned || topSite is TopSite.Default
    val isProvidedSite = topSite is TopSite.Provided
    val result = mutableListOf<MenuItem>()

    result.add(
        MenuItem(
            title = stringResource(id = R.string.bookmark_menu_open_in_private_tab_button),
            testTag = TopSitesTestTag.openInPrivateTab,
            onClick = { onOpenInPrivateTabClicked(topSite) },
        ),
    )

    if (isPinnedSite) {
        result.add(
            MenuItem(
                title = stringResource(id = R.string.rename_top_site),
                testTag = TopSitesTestTag.rename,
                onClick = { onRenameTopSiteClicked(topSite) },
            ),
        )
    }

    if (!isProvidedSite) {
        result.add(
            MenuItem(
                title = stringResource(
                    id = if (isPinnedSite) {
                        R.string.remove_top_site
                    } else {
                        R.string.delete_from_history
                    },
                ),
                testTag = TopSitesTestTag.remove,
                onClick = { onRemoveTopSiteClicked(topSite) },
            ),
        )
    }

    if (isProvidedSite) {
        result.add(
            MenuItem(
                title = stringResource(id = R.string.delete_from_history),
                testTag = TopSitesTestTag.remove,
                onClick = { onRemoveTopSiteClicked(topSite) },
            ),
        )
    }

    if (isProvidedSite) {
        result.add(
            MenuItem(
                title = stringResource(id = R.string.top_sites_menu_settings),
                onClick = onSettingsClicked,
            ),
        )
    }

    if (isProvidedSite) {
        result.add(
            MenuItem(
                title = stringResource(id = R.string.top_sites_menu_sponsor_privacy),
                onClick = onSponsorPrivacyClicked,
            ),
        )
    }

    return result
}

private fun submitTopSitesImpressionPing(topSite: TopSite.Provided, position: Int) {
    TopSitesMetrics.contileImpression.record(
        TopSitesMetrics.ContileImpressionExtra(
            position = position + 1,
            source = "newtab",
        ),
    )

    topSite.id?.let { TopSitesMetrics.contileTileId.set(it) }
    topSite.title?.let { TopSitesMetrics.contileAdvertiser.set(it.lowercase()) }
    TopSitesMetrics.contileReportingUrl.set(topSite.impressionUrl)
    Pings.topsitesImpression.submit()
}

@Composable
@LightDarkPreview
private fun TopSitesPreview() {
    FirefoxTheme {
        Box(modifier = Modifier.background(color = FirefoxTheme.colors.layer1)) {
            TopSites(
                topSites = mutableListOf<TopSite>().apply {
                    for (index in 0 until 2) {
                        add(
                            TopSite.Pinned(
                                id = index.toLong(),
                                title = "Mozilla$index",
                                url = "mozilla.com",
                                createdAt = 0L,
                            ),
                        )
                    }

                    for (index in 0 until 2) {
                        add(
                            TopSite.Provided(
                                id = index.toLong(),
                                title = "Mozilla$index",
                                url = "mozilla.com",
                                clickUrl = "https://mozilla.com/click",
                                imageUrl = "https://test.com/image2.jpg",
                                impressionUrl = "https://example.com",
                                createdAt = 0L,
                            ),
                        )
                    }

                    for (index in 0 until 2) {
                        add(
                            TopSite.Default(
                                id = index.toLong(),
                                title = "Mozilla$index",
                                url = "mozilla.com",
                                createdAt = 0L,
                            ),
                        )
                    }

                    add(
                        TopSite.Default(
                            id = null,
                            title = "Top Articles",
                            url = "https://getpocket.com/fenix-top-articles",
                            createdAt = 0L,
                        ),
                    )
                },
                onTopSiteClick = {},
                onTopSiteLongClick = {},
                onOpenInPrivateTabClicked = {},
                onRenameTopSiteClicked = {},
                onRemoveTopSiteClicked = {},
                onSettingsClicked = {},
                onSponsorPrivacyClicked = {},
                onTopSitesItemBound = {},
            )
        }
    }
}
