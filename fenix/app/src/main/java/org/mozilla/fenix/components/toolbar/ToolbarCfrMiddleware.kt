package org.mozilla.fenix.components.toolbar

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transformWhile
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.compose.cfr.CFRPopup
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.lib.state.ext.flowScoped
import org.mozilla.fenix.R
import org.mozilla.fenix.shopping.ShoppingExperienceFeature
import org.mozilla.fenix.utils.Settings

class ToolbarCfrMiddleware(
    private val browserStore: BrowserStore,
    private val toolbar: BrowserToolbar,
    private val settings: Settings,
    private val shoppingExperienceFeature: ShoppingExperienceFeature,
    private val customTabId: String?,
    private val showTcpCfr: () -> Unit,
    private val showShoppingCFR: (Boolean) -> Unit,
    private val showEraseCfr: () -> Unit,
) : Middleware<BrowserFragmentState, BrowserFragmentAction> {

    @VisibleForTesting
    internal var scope: CoroutineScope? = null

    @VisibleForTesting
    internal var popup: CFRPopup? = null
    override fun invoke(
        context: MiddlewareContext<BrowserFragmentState, BrowserFragmentAction>,
        next: (BrowserFragmentAction) -> Unit,
        action: BrowserFragmentAction
    ) {
        when (action) {
            is BrowserFragmentAction.Init -> {
                val isPrivate = customTabId?.let {
                    browserStore.state.findCustomTabOrSelectedTab(it)?.content?.private
                } ?: browserStore.state.selectedTab?.content?.private ?: false
                val cfr = getCFRToShow(isPrivate)
                handleCfr(cfr)
            }
        }
    }

    private fun handleCfr(cfr: ToolbarCFR) = when(cfr) {
        ToolbarCFR.TCP -> {
            scope = browserStore.flowScoped { flow ->
                flow.mapNotNull { it.findCustomTabOrSelectedTab(customTabId)?.content?.progress }
                    // The "transformWhile" below ensures that the 100% progress is only collected once.
                    .transformWhile { progress ->
                        emit(progress)
                        progress != 100
                    }.filter { popup == null && it == 100 }.collect {
                        scope?.cancel()
                        showTcpCfr()
                    }
            }
        }

        ToolbarCFR.SHOPPING, ToolbarCFR.SHOPPING_OPTED_IN -> {
            scope = browserStore.flowScoped { flow ->
                val shouldShowCfr: Boolean? = flow.mapNotNull { it.selectedTab }
                    .filter { it.content.isProductUrl && it.content.progress == 100 && !it.content.loading }
                    .distinctUntilChanged()
                    .map { toolbar.findViewById<View>(R.id.mozac_browser_toolbar_page_actions).isVisible }
                    .filter { popup == null && it }
                    .firstOrNull()

                if (shouldShowCfr == true) {
                    showShoppingCFR(cfr == ToolbarCFR.SHOPPING_OPTED_IN)
                }

                scope?.cancel()
            }
        }

        ToolbarCFR.ERASE -> {
            scope = browserStore.flowScoped { flow ->
                flow
                    .mapNotNull { it.findCustomTabOrSelectedTab(customTabId) }
                    .filter { it.content.private }
                    .map { it.content.progress }
                    // The "transformWhile" below ensures that the 100% progress is only collected once.
                    .transformWhile { progress ->
                        emit(progress)
                        progress != 100
                    }
                    .filter { popup == null && it == 100 }
                    .collect {
                        scope?.cancel()
                        showEraseCfr()
                    }
            }
        }

        ToolbarCFR.NONE -> {
            // no-op
        }
    }

    private fun getCFRToShow(isPrivate: Boolean): ToolbarCFR = when {
        settings.shouldShowEraseActionCFR && isPrivate -> {
            ToolbarCFR.ERASE
        }

        settings.shouldShowTotalCookieProtectionCFR && (
                settings.openTabsCount >= CFR_MINIMUM_NUMBER_OPENED_TABS
                ) -> ToolbarCFR.TCP

        shoppingExperienceFeature.isEnabled &&
                settings.shouldShowReviewQualityCheckCFR -> whichShoppingCFR()

        else -> ToolbarCFR.NONE
    }

    private fun whichShoppingCFR(): ToolbarCFR {
        fun Long.isInitialized(): Boolean = this != 0L
        fun Long.afterOneDay(): Boolean = this.isInitialized() &&
                System.currentTimeMillis() - this > Settings.ONE_DAY_MS

        val optInTime = settings.reviewQualityCheckOptInTimeInMillis
        val firstCfrShownTime = settings.reviewQualityCheckCfrDisplayTimeInMillis

        return when {
            // First CFR should be displayed on first product page visit
            !firstCfrShownTime.isInitialized() ->
                ToolbarCFR.SHOPPING
            // First CFR should be displayed again 24 hours later only for not opted in users
            !optInTime.isInitialized() && firstCfrShownTime.afterOneDay() ->
                ToolbarCFR.SHOPPING
            // Second CFR should be shown 24 hours after opt in
            optInTime.afterOneDay() ->
                ToolbarCFR.SHOPPING_OPTED_IN
            else -> {
                ToolbarCFR.NONE
            }
        }
    }
}
