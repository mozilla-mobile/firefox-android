/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shopping.store

import mozilla.components.lib.state.State
import org.mozilla.fenix.shopping.store.ReviewQualityCheckState.HighlightType
import java.util.SortedMap

private const val NUMBER_OF_HIGHLIGHTS_FOR_COMPACT_MODE = 2

/**
 * UI state of the review quality check feature.
 */
sealed interface ReviewQualityCheckState : State {
    /**
     * The initial state of the feature, it's also the default state set in the store.
     */
    object Initial : ReviewQualityCheckState

    /**
     * The state when the user has not opted in for the feature.
     */
    object NotOptedIn : ReviewQualityCheckState

    /**
     * The state when the user has opted in for the feature.
     *
     * @property productReviewState The state of the product the user is browsing.
     * @property productRecommendationsPreference User preference whether to show product
     * recommendations. True if product recommendations should be shown. Null indicates that product
     * recommendations are disabled.
     */
    data class OptedIn(
        val productReviewState: ProductReviewState = ProductReviewState.Loading,
        val productRecommendationsPreference: Boolean?,
    ) : ReviewQualityCheckState {

        /**
         * The state of the product the user is browsing.
         */
        sealed interface ProductReviewState {
            /**
             * Denotes content is loading.
             */
            object Loading : ProductReviewState

            /**
             * Denotes an error has occurred.
             */
            sealed interface Error : ProductReviewState {
                /**
                 * Denotes a network error has occurred.
                 */
                object NetworkError : Error

                /**
                 * Denotes a product is not supported.
                 */
                object UnsupportedProductTypeError : Error

                /**
                 * Denotes a generic error has occurred.
                 */
                object GenericError : Error
            }

            /**
             * Denotes no analysis is present for the product the user is browsing.
             */
            data class NoAnalysisPresent(
                val isReanalyzing: Boolean = false,
            ) : ProductReviewState

            /**
             * Denotes the state where analysis of the product is fetched and present.
             *
             * @property productId The id of the product, e.g ASIN, SKU.
             * @property reviewGrade The review grade of the product.
             * @property analysisStatus The status of the product analysis.
             * @property adjustedRating The adjusted rating taking review quality into consideration.
             * @property productUrl The url of the product the user is browsing.
             * @property highlights Optional highlights based on recent reviews of the product.
             * @property recommendedProductState The state of the recommended product.
             */
            data class AnalysisPresent(
                val productId: String,
                val reviewGrade: Grade?,
                val analysisStatus: AnalysisStatus,
                val adjustedRating: Float?,
                val productUrl: String,
                val highlights: SortedMap<HighlightType, List<String>>?,
                val recommendedProductState: RecommendedProductState = RecommendedProductState.Initial,
            ) : ProductReviewState {
                init {
                    require(!(highlights == null && reviewGrade == null && adjustedRating == null)) {
                        "AnalysisPresent state should only be created when at least one of " +
                            "reviewGrade, adjustedRating or highlights is not null"
                    }
                }

                val showMoreButtonVisible: Boolean =
                    highlights != null && highlights != highlights.forCompactMode()

                val highlightsFadeVisible: Boolean =
                    highlights != null && showMoreButtonVisible &&
                        highlights.forCompactMode().entries.first().value.size > 1

                val notEnoughReviewsCardVisible: Boolean =
                    (reviewGrade == null || adjustedRating == null) &&
                        analysisStatus != AnalysisStatus.NEEDS_ANALYSIS &&
                        analysisStatus != AnalysisStatus.REANALYZING

                /**
                 * The status of the product analysis.
                 */
                enum class AnalysisStatus {
                    NEEDS_ANALYSIS, REANALYZING, UP_TO_DATE, COMPLETED
                }
            }
        }
    }

    /**
     * Review Grade of the product - A being the best and F being the worst. There is no grade E.
     */
    enum class Grade {
        A, B, C, D, F
    }

    /**
     * Factors for which highlights are available based on recent reviews of the product.
     */
    enum class HighlightType {
        QUALITY, PRICE, SHIPPING, PACKAGING_AND_APPEARANCE, COMPETITIVENESS
    }

    /**
     * Types of links that can be opened from the review quality check feature.
     */
    sealed class LinkType {
        /**
         * Opens a link to analyze a product.
         */
        data class AnalyzeLink(val url: String) : LinkType()

        /**
         * Opens an external "Learn more" link.
         */
        data class ExternalLink(val url: String) : LinkType()
    }

    /**
     * The state of the recommended product.
     */
    sealed interface RecommendedProductState {
        /**
         * The initial state of the recommended product.
         */
        object Initial : RecommendedProductState

        /**
         * The state when the recommended product is loading.
         */
        object Loading : RecommendedProductState

        /**
         * The state when an error has occurred while fetching the recommended product.
         */
        object Error : RecommendedProductState

        /**
         * The state when the recommended product is available.
         *
         * @property name The name of the product.
         * @property productUrl The url of the product.
         * @property imageUrl The url of the image of the product.
         * @property formattedPrice The formatted price of the product.
         * @property reviewGrade The review grade of the product.
         * @property adjustedRating The adjusted rating of the product.
         * @property isSponsored True if the product is sponsored.
         * @property analysisUrl The url of the analysis of the product.
         */
        data class Product(
            val name: String,
            val productUrl: String,
            val imageUrl: String,
            val formattedPrice: String,
            val reviewGrade: Grade,
            val adjustedRating: Float,
            val isSponsored: Boolean,
            val analysisUrl: String,
        ) : RecommendedProductState
    }

    /**
     * Returns [ReviewQualityCheckState] applying the given [transform] function if the current
     * state is [OptedIn].
     */
    fun mapIfOptedIn(transform: (OptedIn) -> ReviewQualityCheckState): ReviewQualityCheckState =
        if (this is OptedIn) {
            transform(this)
        } else {
            this
        }
}

/**
 * Highlights to display in compact mode that contains first 2 highlights of the first
 * highlight type.
 */
fun Map<HighlightType, List<String>>.forCompactMode(): Map<HighlightType, List<String>> =
    entries.first().let { entry ->
        mapOf(entry.key to entry.value.take(NUMBER_OF_HIGHLIGHTS_FOR_COMPACT_MODE))
    }
