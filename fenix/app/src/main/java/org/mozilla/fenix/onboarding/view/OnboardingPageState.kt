/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

/**
 * Model containing data for [OnboardingPage].
 *
 * @param imageResources The [ImageResources] of the page.
 * @param title [String] title of the page.
 * @param description [String] description of the page.
 * @param linkTextState [LinkTextState] part of description text with a link.
 * @param primaryButton [Action] action for the primary button.
 * @param secondaryButton [Action] action for the secondary button.
 * @param onRecordImpressionEvent Callback for recording impression event.
 */
data class OnboardingPageState(
    val imageResources: ImageResources,
    val title: String,
    val description: String,
    val linkTextState: LinkTextState? = null,
    val primaryButton: Action,
    val secondaryButton: Action? = null,
    val onRecordImpressionEvent: () -> Unit = {},
)

/**
 * Model containing link text, url and action.
 */
data class LinkTextState(
    val text: String,
    val url: String,
    val onClick: (String) -> Unit,
)

/**
 * Model containing text and action for a button.
 */
data class Action(
    val text: String,
    val onClick: () -> Unit,
)
