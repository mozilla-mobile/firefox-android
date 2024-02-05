/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import mozilla.components.feature.fxsuggest.FxSuggestIngestionScheduler
import mozilla.components.feature.fxsuggest.FxSuggestStorage
import org.mozilla.fenix.perf.lazyMonitored

/**
 * Component group for Firefox Suggest.
 *
 * @param context The Android application context.
 */
class FxSuggest(context: Context) {
    val storage by lazyMonitored {
        FxSuggestStorage(context)
    }

    val ingestionScheduler by lazyMonitored {
        FxSuggestIngestionScheduler(context)
    }
}
