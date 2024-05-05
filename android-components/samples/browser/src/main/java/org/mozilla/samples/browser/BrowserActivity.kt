/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.fragment.app.Fragment
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.contextmenu.ext.DefaultSelectionActionDelegate
import mozilla.components.feature.intent.ext.getSessionId
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.locale.LocaleAwareAppCompatActivity
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.webextensions.WebExtensionPopupObserver
import org.mozilla.samples.browser.addons.WebExtensionActionPopupActivity
import org.mozilla.samples.browser.ext.components
import org.mozilla.samples.browser.summarize.SummaryFragment

/**
 * Activity that holds the [BrowserFragment].
 */
open class BrowserActivity : LocaleAwareAppCompatActivity(), ComponentCallbacks2 {
    private val webExtensionPopupObserver by lazy {
        WebExtensionPopupObserver(components.store, ::openPopup)
    }

    /**
     * Returns a new instance of [BrowserFragment] to display.
     */
    open fun createBrowserFragment(sessionId: String?): Fragment =
        BrowserFragment.create(sessionId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val sessionId = SafeIntent(intent).getSessionId()
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.container, createBrowserFragment(sessionId))
                commit()
            }
        }

        lifecycle.addObserver(webExtensionPopupObserver)
        components.historyStorage.registerStorageMaintenanceWorker()
        components.notificationsDelegate.bindToActivity(this)
    }

    override fun onBackPressed() {
        supportFragmentManager.fragments.forEach {
            if (it is UserInteractionHandler && it.onBackPressed()) {
                return
            }
        }

        onBackPressedDispatcher.onBackPressed()
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        when (name) {
            EngineView::class.java.name -> components.engine.createView(context, attrs).apply {
                selectionActionDelegate = DefaultSelectionActionDelegate(
                    store = components.store,
                    context = context,
                    summarizeTextClicked = { text ->
                        val summaryFragment = SummaryFragment.newInstance(text)
                        summaryFragment.show(supportFragmentManager, "summaryDialog")
                    }
                )
            }.asView()
            else -> super.onCreateView(parent, name, context, attrs)
        }

    private fun openPopup(webExtensionState: WebExtensionState) {
        val intent = Intent(this, WebExtensionActionPopupActivity::class.java)
        intent.putExtra("web_extension_id", webExtensionState.id)
        intent.putExtra("web_extension_name", webExtensionState.name)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        components.notificationsDelegate.unBindActivity(this)
    }
}
