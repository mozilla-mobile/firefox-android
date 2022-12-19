/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.cookiebannerexception

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.state.SessionState
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.cookiehandling.CookieBannersStorage
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.focus.GleanMetrics.CookieBanner
import org.mozilla.focus.cookiebanner.CookieBannerOption
import org.mozilla.focus.ext.components
import org.mozilla.focus.ext.settings

class CookieBannerExceptionMiddleware(
    private val ioScope: CoroutineScope,
    private val cookieBannersStorage: CookieBannersStorage,
    private val appContext: Context,
    private val currentTab: SessionState,
) :
    Middleware<CookieBannerExceptionState, CookieBannerExceptionAction> {

    override fun invoke(
        context: MiddlewareContext<CookieBannerExceptionState, CookieBannerExceptionAction>,
        next: (CookieBannerExceptionAction) -> Unit,
        action: CookieBannerExceptionAction,
    ) {
        when (action) {
            is CookieBannerExceptionAction.CookieBannerDetected -> {
                if (!action.isCookieBannerDetected) {
                    context.store.dispatch(
                        CookieBannerExceptionAction.UpdateCookieBannerExceptionStatus(
                            CookieBannerExceptionStatus.NoCookieBannerDetected,
                        ),
                    )
                } else {
                    showExceptionStatus(context)
                }
            }

            is CookieBannerExceptionAction.InitCookieBannerException -> {
                /**
                 * The initial CookieBannerExceptionState when the user enters first in the screen
                 */
                showExceptionStatus(context)
            }

            is CookieBannerExceptionAction.ToggleCookieBannerException -> {
                ioScope.launch {
                    if (action.isCookieBannerHandlingExceptionEnabled) {
                        cookieBannersStorage.removeException(currentTab.content.url, true)
                        CookieBanner.exceptionRemoved.record(NoExtras())
                        context.store.dispatch(
                            CookieBannerExceptionAction.UpdateCookieBannerExceptionStatus(
                                CookieBannerExceptionStatus.NoException,
                            ),
                        )
                    } else {
                        clearSiteData()
                        cookieBannersStorage.addPersistentExceptionInPrivateMode(currentTab.content.url)
                        CookieBanner.exceptionAdded.record(NoExtras())
                        context.store.dispatch(
                            CookieBannerExceptionAction.UpdateCookieBannerExceptionStatus(
                                CookieBannerExceptionStatus.HasException,
                            ),
                        )
                    }
                    appContext.components.sessionUseCases.reload()
                }
                next(action)
            }
            else -> {
                next(action)
            }
        }
    }

    private fun showExceptionStatus(
        context: MiddlewareContext<CookieBannerExceptionState, CookieBannerExceptionAction>,
    ) {
        val shouldShowCookieBannerItem = shouldShowCookieBannerExceptionItem()
        context.store.dispatch(
            CookieBannerExceptionAction.UpdateCookieBannerExceptionVisibility(
                shouldShowCookieBannerItem = shouldShowCookieBannerItem,
            ),
        )

        if (!shouldShowCookieBannerItem) {
            return
        }

        ioScope.launch {
            val hasException =
                cookieBannersStorage.hasException(currentTab.content.url, true)
            if (hasException) {
                context.store.dispatch(
                    CookieBannerExceptionAction.UpdateCookieBannerExceptionStatus(
                        CookieBannerExceptionStatus.HasException,
                    ),
                )
            } else {
                context.store.dispatch(
                    CookieBannerExceptionAction.UpdateCookieBannerExceptionStatus(
                        CookieBannerExceptionStatus.NoException,
                    ),
                )
            }
        }
    }

    /**
     * It returns the cookie banner exception item visibility from tracking protection panel .
     * If the item is invisible item details should also be invisible.
     */
    private fun shouldShowCookieBannerExceptionItem(): Boolean {
        return appContext.settings.isCookieBannerEnable &&
            appContext.settings.getCurrentCookieBannerOptionFromSharePref() !=
            CookieBannerOption.CookieBannerDisabled()
    }

    private suspend fun clearSiteData() {
        val host = currentTab.content.url.toUri().host.orEmpty()
        val domain = appContext.components.publicSuffixList.getPublicSuffixPlusOne(host).await()
        withContext(Dispatchers.Main) {
            appContext.components.engine.clearData(
                host = domain,
                data = Engine.BrowsingData.select(
                    Engine.BrowsingData.AUTH_SESSIONS,
                    Engine.BrowsingData.ALL_SITE_DATA,
                ),
            )
        }
    }
}
