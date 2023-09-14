/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import android.content.Context
import org.mozilla.experiments.nimbus.GleanPlumbMessageHelper
import org.mozilla.experiments.nimbus.internal.NimbusException
import org.mozilla.fenix.compose.LinkTextState
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.OnboardingCardData
import org.mozilla.fenix.nimbus.OnboardingCardType
import org.mozilla.fenix.settings.SupportUtils

/**
 * Returns a list of all the required Nimbus 'cards' that have been converted to [OnboardingPageUiData].
 */
internal fun Collection<OnboardingCardData>.toPageUiData(
    context: Context,
    showNotificationPage: Boolean,
    showAddWidgetPage: Boolean,
): List<OnboardingPageUiData> {
    val junoOnboardingFeature = FxNimbus.features.junoOnboarding.value()
    val jexlConditions = junoOnboardingFeature.conditions

    return filter { it.shouldDisplayCard(context, jexlConditions) }
        .filter {
            when (it.cardType) {
                OnboardingCardType.NOTIFICATION_PERMISSION -> {
                    it.enabled && showNotificationPage
                }

                OnboardingCardType.ADD_SEARCH_WIDGET -> {
                    it.enabled && showAddWidgetPage
                }

                else -> {
                    it.enabled
                }
            }
        }.sortedBy { it.ordering }
        .map { it.toPageUiData() }
}

private fun OnboardingCardData.shouldDisplayCard(
    context: Context,
    jexlConditions: Map<String, String>,
): Boolean {
    val jexlCache: MutableMap<String, Boolean> = mutableMapOf()
    val jexlHelper = context.components.analytics.messagingStorage.helper

    return verifyConditionEligibility(
        prerequisites,
        jexlConditions,
        jexlCache,
        jexlHelper,
    ) && !verifyConditionEligibility(disqualifiers, jexlConditions, jexlCache, jexlHelper)
}

private fun verifyConditionEligibility(
    cardConditions: List<String>,
    jexlConditions: Map<String, String>,
    jexlCache: MutableMap<String, Boolean>,
    jexlHelper: GleanPlumbMessageHelper,
): Boolean {
    val malFormedMap = mutableMapOf<String, String>()
    // Make sure conditions exist and have a value, and that the number
    // of valid conditions matches the number of conditions on the card's
    // respective prerequisite or disqualifier table. If these mismatch,
    // that means a card contains a condition that's not in the feature
    // conditions lookup table. JEXLs can only be evaluated on
    // supported conditions. Otherwise, consider the card invalid.
    val allConditionValues = cardConditions.mapNotNull { jexlConditions[it] }
    return if (allConditionValues.size == cardConditions.size) {
        return allConditionValues.all { condition ->
            jexlCache[condition]
                ?: try {
                    if (malFormedMap.containsKey(condition)) {
                        return false
                    }
                    jexlHelper.evalJexl(condition).also { isValid ->
                        jexlCache.set(condition, isValid)
                    }
                } catch (e: NimbusException.EvaluationException) {
                    malFormedMap[condition] = condition
                    false
                }
        }
    } else {
        false
    }
}

private fun OnboardingCardData.toPageUiData() = OnboardingPageUiData(
    type = cardType.toPageUiDataType(),
    imageRes = imageRes.resourceId,
    title = title,
    description = body,
    linkText = linkText,
    primaryButtonLabel = primaryButtonLabel,
    secondaryButtonLabel = secondaryButtonLabel,
)

private fun OnboardingCardType.toPageUiDataType() = when (this) {
    OnboardingCardType.DEFAULT_BROWSER -> OnboardingPageUiData.Type.DEFAULT_BROWSER
    OnboardingCardType.SYNC_SIGN_IN -> OnboardingPageUiData.Type.SYNC_SIGN_IN
    OnboardingCardType.NOTIFICATION_PERMISSION -> OnboardingPageUiData.Type.NOTIFICATION_PERMISSION
    OnboardingCardType.ADD_SEARCH_WIDGET -> OnboardingPageUiData.Type.ADD_SEARCH_WIDGET
}

/**
 * Mapper to convert [OnboardingPageUiData] to [OnboardingPageState] that is a param for
 * [OnboardingPage] composable.
 */
@Suppress("LongParameterList")
internal fun mapToOnboardingPageState(
    onboardingPageUiData: OnboardingPageUiData,
    onMakeFirefoxDefaultClick: () -> Unit,
    onMakeFirefoxDefaultSkipClick: () -> Unit,
    onPrivacyPolicyClick: (String) -> Unit,
    onSignInButtonClick: () -> Unit,
    onSignInSkipClick: () -> Unit,
    onNotificationPermissionButtonClick: () -> Unit,
    onNotificationPermissionSkipClick: () -> Unit,
    onAddFirefoxWidgetClick: () -> Unit,
    onAddFirefoxWidgetSkipClick: () -> Unit,
): OnboardingPageState = when (onboardingPageUiData.type) {
    OnboardingPageUiData.Type.DEFAULT_BROWSER -> createOnboardingPageState(
        onboardingPageUiData = onboardingPageUiData,
        onPositiveButtonClick = onMakeFirefoxDefaultClick,
        onNegativeButtonClick = onMakeFirefoxDefaultSkipClick,
        onUrlClick = onPrivacyPolicyClick,
    )

    OnboardingPageUiData.Type.ADD_SEARCH_WIDGET -> createOnboardingPageState(
        onboardingPageUiData = onboardingPageUiData,
        onPositiveButtonClick = onAddFirefoxWidgetClick,
        onNegativeButtonClick = onAddFirefoxWidgetSkipClick,
        onUrlClick = onPrivacyPolicyClick,
    )

    OnboardingPageUiData.Type.SYNC_SIGN_IN -> createOnboardingPageState(
        onboardingPageUiData = onboardingPageUiData,
        onPositiveButtonClick = onSignInButtonClick,
        onNegativeButtonClick = onSignInSkipClick,
    )

    OnboardingPageUiData.Type.NOTIFICATION_PERMISSION -> createOnboardingPageState(
        onboardingPageUiData = onboardingPageUiData,
        onPositiveButtonClick = onNotificationPermissionButtonClick,
        onNegativeButtonClick = onNotificationPermissionSkipClick,
    )
}

private fun createOnboardingPageState(
    onboardingPageUiData: OnboardingPageUiData,
    onPositiveButtonClick: () -> Unit,
    onNegativeButtonClick: () -> Unit,
    onUrlClick: (String) -> Unit = {},
): OnboardingPageState = OnboardingPageState(
    imageRes = onboardingPageUiData.imageRes,
    title = onboardingPageUiData.title,
    description = onboardingPageUiData.description,
    linkTextState = onboardingPageUiData.linkText?.let {
        LinkTextState(
            text = it,
            url = SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.PRIVATE_NOTICE),
            onClick = onUrlClick,
        )
    },
    primaryButton = Action(onboardingPageUiData.primaryButtonLabel, onPositiveButtonClick),
    secondaryButton = Action(onboardingPageUiData.secondaryButtonLabel, onNegativeButtonClick),
)
