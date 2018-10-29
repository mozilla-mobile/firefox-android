/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.storages

/**
 * Base interface intended to be implemented by the different
 * storage engines
 */
internal interface StorageEngine {
    /**
     * Get a snapshot of the stored data as a JSON object.
     *
     * @param storeName the name of the desired store
     * @param clearStore whether or not to clearStore the requested store
     *
     * @return the JSON object containing the recorded data. This could be either
     *         a [JSONObject] or a [JSONArray]. Unfortunately, the only common
     *         ancestor is [Object], so we need to return [Any].
     */
    fun getSnapshotAsJSON(storeName: String, clearStore: Boolean): Any?
}
