/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.Intent
import androidx.navigation.NavDestination
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.feature.intent.ext.getSessionId
import mozilla.components.support.utils.SafeIntent
import org.mozilla.fenix.customtabs.ExternalAppBrowserActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings

fun FenixActivity.getBreadcrumbMessage(destination: NavDestination): String = when (this) {
    is ExternalAppBrowserActivity -> {
        getExternalAppBrowserBreadcrumbMessage(destination)
    }

    else -> {
        getFenixBreadcrumbMessage(destination)
    }
}

fun FenixActivity.getIntentSource(intent: SafeIntent): String? = when (this) {
    is ExternalAppBrowserActivity -> {
        getExternalAppBrowserIntentSource()
    }

    else -> {
        getFenixIntentSource(intent)
    }
}


fun FenixActivity.getIntentSessionId(intent: SafeIntent): String? = when (this) {
    is ExternalAppBrowserActivity -> {
        getExternalAppBrowserIntentSessionId(intent)
    }

    else -> {
        getFenixIntentSessionId()
    }
}

private fun FenixActivity.getFenixBreadcrumbMessage(destination: NavDestination): String {
    val fragmentName = resources.getResourceEntryName(destination.id)
    return "Changing to fragment $fragmentName, isCustomTab: false"
}

private fun getFenixIntentSource(intent: SafeIntent): String? {
    return when {
        intent.isLauncherIntent -> FenixActivity.APP_ICON
        intent.action == Intent.ACTION_VIEW -> "LINK"
        else -> null
    }
}

private fun getFenixIntentSessionId(): String? = null

private fun FenixActivity.getExternalAppBrowserBreadcrumbMessage(destination: NavDestination): String {
    val fragmentName = resources.getResourceEntryName(destination.id)
    return "Changing to fragment $fragmentName, isCustomTab: true"
}

private fun getExternalAppBrowserIntentSource() = "CUSTOM_TAB"

private fun getExternalAppBrowserIntentSessionId(intent: SafeIntent) = intent.getSessionId()

fun FenixActivity.handleRequestDesktopMode(tabId: String) {
    components.useCases.sessionUseCases.requestDesktopSite(true, tabId)
    components.core.store.dispatch(ContentAction.UpdateDesktopModeAction(tabId, true))

    // Reset preference value after opening the tab in desktop mode
    settings().openNextTabInDesktopMode = false
}
