/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import mozilla.components.support.base.log.logger.Logger

/**
 * Runs a lambda, catching and handling FxaException
 *
 * - On success: return the lambda result
 * - On FxaException: return null
 */
suspend fun<T> handleFxaExceptions(logger: Logger, operation: String, block: suspend () -> T?): T? {
    return try {
        logger.info("Executing: $operation")
        val res = block()
        logger.info("Successfully executed: $operation")
        res
    } catch (e: FxaException) {
        // We'd like to simply crash in case of certain errors (e.g. panics).
        if (e.shouldPropagate()) {
            throw e
        }
        logger.error("Error while running: $operation", e)
        null
    }
}
