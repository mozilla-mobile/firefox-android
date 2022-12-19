/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.cookiebannerexception

/**
 * Sealed class for the cookie banner exception Gui item
 * from Tracking Protection panel.
 */
sealed class CookieBannerExceptionStatus {

    /**
     * If the site is excepted from cookie banner reduction.
     */
    object HasException : CookieBannerExceptionStatus()

    /**
     * If the site is not excepted from cookie banner reduction.
     */
    object NoException : CookieBannerExceptionStatus()

    /**
     * If the site doesn't have a cookie banner.
     */
    object NoCookieBannerDetected : CookieBannerExceptionStatus()
}
