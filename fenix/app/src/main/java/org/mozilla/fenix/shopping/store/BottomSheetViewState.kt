/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shopping.store

/**
 * States the review quality check bottom sheet can be opened in.
 *
 * @property state Name of the state to be used in review quality check telemetry.
 */
enum class BottomSheetViewState(val state: String) {
    FULL_VIEW("full"),
    HALF_VIEW("half"),
}
