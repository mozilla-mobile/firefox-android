/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import mozilla.components.service.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.AppMenu
import org.mozilla.fenix.GleanMetrics.HomeMenu
import org.mozilla.fenix.ext.settings

/**
 * A helper service for telemetry events that require multiple intermediate
 *     events to happen in order to send.
 */
class MetricsHelperService(private val context: Context) : MetricsService {
    override val type = MetricServiceType.Data

    val settings = context.settings()

    override fun start() = Unit

    override fun stop() = Unit

    override fun track(event: Event) {
        when (event) {
            is Event.UsageData.SetMenuAnchorFragment -> {
                settings.anchorFragment = event.anchorFragment
            }
            is Event.UsageData.OpenMenu -> {
                settings.noOperationMenuOpen = true
            }
            is Event.UsageData.CloseMenu -> {
                if (settings.noOperationMenuOpen) {
                    when (settings.anchorFragment) {
                        HOME_ANCHOR -> {
                            HomeMenu.userNoOperationExit.record(NoExtras())
                        }
                        BROWSER_ANCHOR -> {
                            AppMenu.userNoOperationExit.record(NoExtras())
                        }
                        else -> Unit
                    }
                }
                settings.noOperationMenuOpen = false
            }
            is Event.UsageData.InteractedWithMenu -> {
                settings.noOperationMenuOpen = false
            }
            else -> Unit
        }
    }

    override fun shouldTrack(event: Event): Boolean {
        return (event is Event.UsageData)
    }

    companion object {
        const val HOME_ANCHOR = "HomeFragment"
        const val BROWSER_ANCHOR = "BrowserFragment"
    }
}
