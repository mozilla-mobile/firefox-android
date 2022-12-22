/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.cookiebanner

import android.content.Context
import androidx.preference.PreferenceManager
import org.mozilla.focus.R
import org.mozilla.focus.ext.settings
import org.mozilla.focus.nimbus.FocusNimbus
import java.util.concurrent.TimeUnit

/**
 *  Utils class for Cookie Banner Reducer Dialog
 */
class CookieBannerDialogUtils {
    companion object {
        /**
         *   Returns the selected cookieBannerVariant from Nimbus.
         *   If the variant is not in the list, it returns one for testing purposes.
         */
        @Suppress("MagicNumber")
        fun getCookieBannerSelectedVariant(context: Context): CookieBannerDialogVariant {
            val cookieBannerDialogVariants = getCookieBannerDialogVariants(context)
            return if (
                FocusNimbus.features.cookieBanner.value(context).cookieBannerDialogVersion in
                cookieBannerDialogVariants.indices
            ) {
                cookieBannerDialogVariants[
                    FocusNimbus.features.cookieBanner.value(context).cookieBannerDialogVersion + 1,
                ]
            } else {
                cookieBannerDialogVariants[3]
            }
        }

        /**
         *   Returns a list of cookieBannerVariants.
         *   The last item in the list is for testing purposes with Re-engagement timer set to 5 minutes.
         */
        @Suppress("MagicNumber")
        private fun getCookieBannerDialogVariants(context: Context): Array<CookieBannerDialogVariant> {
            return arrayOf(
                CookieBannerDialogVariant(
                    context.getString(R.string.cookie_banner_dialog_variant_one_title),
                    context.getString(
                        R.string.cookie_banner_dialog_variant_one_message,
                        context.getString(R.string.app_name),
                    ),
                    TimeUnit.DAYS.toMillis(1),
                ),
                CookieBannerDialogVariant(
                    context.getString(R.string.cookie_banner_dialog_variant_two_title),
                    context.getString(
                        R.string.cookie_banner_dialog_variant_two_message,
                        context.getString(R.string.app_name),
                    ),
                    TimeUnit.DAYS.toMillis(2),
                ),
                CookieBannerDialogVariant(
                    context.getString(R.string.cookie_banner_dialog_variant_three_title),
                    context.getString(
                        R.string.cookie_banner_dialog_variant_three_message,
                        context.getString(R.string.app_name),
                    ),
                    TimeUnit.DAYS.toMillis(7),
                ),
                CookieBannerDialogVariant(
                    context.getString(R.string.cookie_banner_dialog_variant_one_title),
                    context.getString(
                        R.string.cookie_banner_dialog_variant_one_message,
                        context.getString(R.string.app_name),
                    ),
                    TimeUnit.MINUTES.toMillis(5),
                ),
            )
        }

        /**
         * This method sets the display value of the dialog.
         * If the user clicks on the confirm button, the dialog should not appear again.
         */
        fun setCookieBannerDialogDisabled(context: Context) {
            val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
            preferenceManager.edit()
                .putBoolean(
                    context.getString(R.string.pref_key_cookie_banner_dialog_enabled),
                    false,
                ).apply()
        }

        /**
         * This method returns the display value of the dialog based on the enabled
         * value from share preference and dismissed time.
         */
        fun shouldShowCookieBannerDialog(reEngagementTime: Long, context: Context): Boolean {
            val lastDismissedTime = PreferenceManager.getDefaultSharedPreferences(context).getLong(
                context.getString(R.string.pref_key_cookie_banner_dialog_last_dismissed_time),
                0L,
            )

            return !context.settings.isFirstRun && isCookieBannerDialogEnabled(context) && (
                lastDismissedTime + reEngagementTime <= System.currentTimeMillis()
                )
        }

        /**
         * This method sets the dismissed time in share preferences.
         */
        fun setCookieBannerDialogDismissedTime(context: Context) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putLong(
                    context.getString(R.string.pref_key_cookie_banner_dialog_last_dismissed_time),
                    System.currentTimeMillis(),
                ).apply()
        }

        private fun isCookieBannerDialogEnabled(context: Context): Boolean {
            val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
            return preferenceManager.getBoolean(
                context.getString(R.string.pref_key_cookie_banner_dialog_enabled),
                true,
            )
        }
    }

    /**
     *  Data class for cookie banner dialog variant
     *  @property title of the dialog
     *  @property message of the dialog
     *  @property reEngagementTime the period of time after the dialog will reappear
     */
    data class CookieBannerDialogVariant(
        val title: String,
        val message: String,
        val reEngagementTime: Long,
    )
}
