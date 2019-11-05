/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.dataprotect

import android.annotation.TargetApi
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.util.Base64
import androidx.annotation.VisibleForTesting
import mozilla.components.support.base.log.logger.Logger
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException

private interface KeyValuePreferences {
    /**
     * Retrieves a stored [key]. See [putString] for storing a [key].
     *
     * @param key A key name.
     * @return An optional [String] if [key] is present in the store.
     */
    fun getString(key: String): String?

    /**
     * Stores [value] under [key]. Retrieve it using [getString].
     *
     * @param key A key name.
     * @param value A value for [key].
     */
    fun putString(key: String, value: String)

    /**
     * Removes key/value pair from storage for the provided [key].
     */
    fun remove(key: String)

    /**
     * Clears all key/value pairs from the storage.
     */
    fun clear()
}

/**
 * A wrapper around [SharedPreferences] which encrypts contents on supported API versions (23+).
 * Otherwise, this simply delegates to [SharedPreferences].
 *
 * In rare circumstances (such as APK signing key rotation) a master key which protects this storage may be lost,
 * in which case previously stored values will be lost as well. Applications are encouraged to instrument such events.
 *
 * @param context A [Context], used for accessing [SharedPreferences].
 */
class SecureAbove22Preferences(context: Context) : KeyValuePreferences {
    private val logger = Logger("SecureAbove22Preferences")

    init {
        migratePrefsIfNecessary(context)
    }

    private val impl = if (Build.VERSION.SDK_INT >= M) {
        SecurePreferencesImpl23(context)
    } else {
        InsecurePreferencesImpl21(context)
    }

    override fun getString(key: String) = impl.getString(key)

    override fun putString(key: String, value: String) = impl.putString(key, value)

    override fun remove(key: String) = impl.remove(key)

    override fun clear() = impl.clear()

    /**
     * Copies over [String] preferences from [plaintextPrefs].
     */
    @VisibleForTesting
    @Suppress("ApplySharedPref")
    internal fun migratePrefs(plaintextPrefs: SharedPreferences) {
        plaintextPrefs.all.forEach {
            if (it.value is String) {
                putString(it.key, it.value as String)
            } else {
                logger.error(
                    "Dropping key during migration because its value type isn't supported: ${it.key}"
                )
            }
        }
        // Using 'commit' here an not apply to speed up how quickly plaintext prefs are erased from disk.
        plaintextPrefs.edit().clear().commit()
    }

    private fun migratePrefsIfNecessary(context: Context) {
        // If we're running on an API level for which we support encryption, see if we have any plaintext values stored
        // on disk. That indicates that we've hit an API upgrade situation - we just went from pre-M to post-M. Since
        // we already have the plaintext keys, we can transparently migrate them to use the encrypted storage layer.
        if (Build.VERSION.SDK_INT >= M) {
            val plaintextPrefs = context.getSharedPreferences(KEY_PREFERENCES_PRE_M, MODE_PRIVATE)
            if (plaintextPrefs.all.isNotEmpty()) {
                migratePrefs(plaintextPrefs)
            }
        }
    }
}

private const val KEY_PREFERENCES_POST_M = "key_preferences_post_m"
private const val KEY_PREFERENCES_PRE_M = "key_preferences_pre_m"

/**
 * A simple [KeyValuePreferences] implementation which entirely delegates to [SharedPreferences] and doesn't perform any
 * encryption/decryption.
 */
private class InsecurePreferencesImpl21(context: Context) : KeyValuePreferences {
    private val prefs = context.getSharedPreferences(KEY_PREFERENCES_PRE_M, MODE_PRIVATE)

    override fun getString(key: String) = prefs.getString(key, null)

    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    override fun remove(key: String) = prefs.edit().remove(key).apply()

    override fun clear() = prefs.edit().clear().apply()
}

/**
 * A [KeyValuePreferences] which is backed by [SharedPreferences] and performs encryption/decryption of values.
 */
@TargetApi(M)
private class SecurePreferencesImpl23(context: Context) : KeyValuePreferences {
    companion object {
        private const val BASE_64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING
    }

    private val logger = Logger("SecurePreferencesImpl23")
    private val prefs = context.getSharedPreferences(KEY_PREFERENCES_POST_M, MODE_PRIVATE)
    private val keystore = Keystore(context.packageName)

    override fun getString(key: String): String? {
        // The fact that we're possibly generating a managed key here implies that this key could be lost after being
        // for some reason. One possible reason for a key to be lost is rotating signing keys for the APK.
        // Applications are encouraged to instrument such events.
        generateManagedKeyIfNecessary()

        if (!prefs.contains(key)) {
            return null
        }

        val value = prefs.getString(key, "")
        val encrypted = Base64.decode(value, BASE_64_FLAGS)

        return try {
            String(keystore.decryptBytes(encrypted), StandardCharsets.UTF_8)
        } catch (error: IllegalArgumentException) {
            logger.error("IllegalArgumentException exception: ", error)
            null
        } catch (error: GeneralSecurityException) {
            logger.error("Decrypt exception: ", error)
            null
        }
    }

    override fun putString(key: String, value: String) {
        generateManagedKeyIfNecessary()
        val editor = prefs.edit()

        val encrypted = keystore.encryptBytes(value.toByteArray(StandardCharsets.UTF_8))
        val data = Base64.encodeToString(encrypted, BASE_64_FLAGS)

        editor.putString(key, data).apply()
    }

    override fun remove(key: String) = prefs.edit().remove(key).apply()

    override fun clear() = prefs.edit().clear().apply()

    /**
     * Generates a "managed key" - a key used to encrypt data stored by this class. This key is "managed" by [Keystore],
     * which stores it in system's secure storage layer exposed via [AndroidKeyStore].
     */
    private fun generateManagedKeyIfNecessary() {
        if (!keystore.available()) {
            keystore.generateKey()
        }
    }
}
