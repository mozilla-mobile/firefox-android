/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine

/**
 * Holds settings of an engine or session. Concrete engine
 * implementations define how these settings are applied i.e.
 * whether a setting is applied on an engine or session instance.
 */
interface Settings {
    /**
     * Setting to control whether or not JavaScript is enabled.
     */
    var javascriptEnabled: Boolean
        get() = throw UnsupportedSettingException()
        set(_) = throw UnsupportedSettingException()

    /**
     * Setting to control whether or not DOM Storage is enabled.
     */
    var domStorageEnabled: Boolean
        get() = throw UnsupportedSettingException()
        set(_) = throw UnsupportedSettingException()
}

/**
 * Exception thrown by default if a setting is not supported by an engine or session.
 */
class UnsupportedSettingException : RuntimeException("Setting not supported by this engine")
