/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import mozilla.components.feature.fxsuggest.FxSuggestImpressionInfo
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState

/**
 * A middleware that will map incoming actions to relevant events for [metrics].
 */
class MetricsMiddleware(
    private val metrics: MetricController,
) : Middleware<AppState, AppAction> {
    override fun invoke(
        context: MiddlewareContext<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction,
    ) {
        handleAction(context, action)
        next(action)
    }

    private fun handleAction(context: MiddlewareContext<AppState, AppAction>, action: AppAction) = when (action) {
        is AppAction.AppLifecycleAction.ResumeAction -> {
            metrics.track(Event.GrowthData.SetAsDefault)
            metrics.track(Event.GrowthData.FirstAppOpenForDay)
            metrics.track(Event.GrowthData.FirstWeekSeriesActivity)
            metrics.track(Event.GrowthData.UsageThreshold)
            metrics.track(Event.GrowthData.UserActivated(fromSearch = false))
        }

        is AppAction.AwesomeBarAction.EngagementFinished -> {
            context.state.awesomeBarVisibilityState.visibleProviderGroups.entries.forEachIndexed { groupIndex, (_, suggestions) ->
                for ((suggestionIndex, suggestion) in suggestions.withIndex()) {
                    val impressionInfo = suggestion.metadata?.get("impressionInfo") as? FxSuggestImpressionInfo.Amp
                        ?: continue
                    val positionInGroup = suggestionIndex + 1
                    val positionInAwesomeBar = groupIndex + positionInGroup
                    val isClicked = context.state.clickedSuggestion == suggestion
                    // TODO: Record Glean impression ping here.
                    println("Recording impression for $impressionInfo at ($positionInGroup:$positionInAwesomeBar) for click: $isClicked")
                }
            }
        }
        else -> Unit
    }
}
