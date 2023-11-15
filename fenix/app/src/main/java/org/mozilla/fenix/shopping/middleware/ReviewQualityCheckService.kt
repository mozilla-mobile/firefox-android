/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shopping.middleware

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.shopping.ProductAnalysis
import mozilla.components.concept.engine.shopping.ProductRecommendation
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

    /**
     * Returns the selected tab url.
     */
    fun selectedTabUrl(): String?

    /**
     * Fetches product recommendations related to the product user is browsing in the current tab.
     *
     * @return [ProductRecommendation] if request succeeds, null otherwise.
     */
    suspend fun productRecommendation(): ProductRecommendation?

    /**
     * Sends a click attribution event for a given product aid.
     */
    suspend fun recordRecommendedProductClick(productAid: String)

    /**
     * Sends an impression attribution event for a given product aid.
     */
    suspend fun recordRecommendedProductImpression(productAid: String)
}

/**
 * Service that handles the network requests for the review quality check feature.
 *
 * @param browserStore Reference to the application's [BrowserStore] to access state.
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
                        continuation.resume(it.asEnumOrDefault(AnalysisStatusDto.OTHER))
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
                        continuation.resume(it.asEnumOrDefault(AnalysisStatusDto.OTHER))
                    },
                    onException = {
                        logger.error("Error fetching analysis status", it)
                        continuation.resume(null)
                    },
                )
            }
        }
    }

    override fun selectedTabUrl(): String? =
        browserStore.state.selectedTab?.content?.url

    override suspend fun productRecommendation(): ProductRecommendation? =
        withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                browserStore.state.selectedTab?.let { tab ->
                    tab.engineState.engineSession?.requestProductRecommendations(
                        url = tab.content.url,
                        onResult = {
                            // Return the first available recommendation since ui requires only
                            // one recommendation.
                            continuation.resume(it.firstOrNull())
                        },
                        onException = {
                            logger.error("Error fetching product recommendation", it)
                            continuation.resume(null)
                        },
                    )
                }
            }
        }

    override suspend fun recordRecommendedProductClick(productAid: String) =
        withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                browserStore.state.selectedTab?.engineState?.engineSession?.sendClickAttributionEvent(
                    aid = productAid,
                    onResult = {
                        continuation.resume(Unit)
                    },
                    onException = {
                        logger.error("Error sending click attribution event", it)
                        continuation.resume(Unit)
                    },
                )
            }
        }

    override suspend fun recordRecommendedProductImpression(productAid: String) {
        withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                browserStore.state.selectedTab?.engineState?.engineSession?.sendImpressionAttributionEvent(
                    aid = productAid,
                    onResult = {
                        continuation.resume(Unit)
                    },
                    onException = {
                        logger.error("Error sending impression attribution event", it)
                        continuation.resume(Unit)
                    },
                )
            }
        }
    }
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
     * Any other status.
     */
    OTHER,
}
