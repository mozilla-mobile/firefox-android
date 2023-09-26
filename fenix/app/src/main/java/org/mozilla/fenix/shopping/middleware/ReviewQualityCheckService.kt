/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shopping.middleware

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.shopping.ProductAnalysis
import mozilla.components.support.base.log.logger.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Service that handles the network requests for the review quality check feature.
 */
interface ReviewQualityCheckService {

    /**
     * Fetches the product review for the current tab.
     *
     * @return [ProductAnalysis] if the request succeeds, null otherwise.
     */
    suspend fun fetchProductReview(): ProductAnalysis?

    /**
     * Triggers a reanalysis of the product review for the current tab.
     *
     * @return [AnalysisStatusDto] if the request succeeds, null otherwise.
     */
    suspend fun reanalyzeProduct(): AnalysisStatusDto?

    /**
     * Fetches the status of the product review for the current tab.
     *
     * @return [AnalysisStatusDto] if the request succeeds, null otherwise.
     */
    suspend fun analysisStatus(): AnalysisStatusDto?
}

/**
 * Service that handles the network requests for the review quality check feature.
 *
 * @property browserStore Reference to the application's [BrowserStore] to access state.
 */
class DefaultReviewQualityCheckService(
    private val browserStore: BrowserStore,
) : ReviewQualityCheckService {

    private val logger = Logger("DefaultReviewQualityCheckService")

    override suspend fun fetchProductReview(): ProductAnalysis? = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            browserStore.state.selectedTab?.let { tab ->
                tab.engineState.engineSession?.requestProductAnalysis(
                    url = tab.content.url,
                    onResult = {
                        continuation.resume(it)
                    },
                    onException = {
                        logger.error("Error fetching product review", it)
                        continuation.resume(null)
                    },
                )
            }
        }
    }

    override suspend fun reanalyzeProduct(): AnalysisStatusDto? = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            browserStore.state.selectedTab?.let { tab ->
                tab.engineState.engineSession?.reanalyzeProduct(
                    url = tab.content.url,
                    onResult = {
                        continuation.resume(it.asEnumOrDefault<AnalysisStatusDto>())
                    },
                    onException = {
                        logger.error("Error starting reanalysis", it)
                        continuation.resume(null)
                    },
                )
            }
        }
    }

    override suspend fun analysisStatus(): AnalysisStatusDto? = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            browserStore.state.selectedTab?.let { tab ->
                tab.engineState.engineSession?.requestAnalysisStatus(
                    url = tab.content.url,
                    onResult = {
                        continuation.resume(it.asEnumOrDefault<AnalysisStatusDto>())
                    },
                    onException = {
                        logger.error("Error fetching analysis status", it)
                        continuation.resume(null)
                    },
                )
            }
        }
    }

    private inline fun <reified T : Enum<T>> String.asEnumOrDefault(defaultValue: T? = null): T? =
        enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) } ?: defaultValue
}

/**
 * Enum that represents the status of the product review analysis.
 */
enum class AnalysisStatusDto {
    /**
     * Analysis is waiting to be picked up.
     */
    PENDING,

    /**
     * Analysis is in progress.
     */
    IN_PROGRESS,

    /**
     * Analysis is completed.
     */
    COMPLETED,

    /**
     * Product can not be analyzed.
     */
    NOT_ANALYZABLE,

    /**
     * Current analysis status with provided params not found.
     */
    NOT_FOUND,

    /**
     * Wrong product params provided.
     */
    UNPROCESSABLE,
}
