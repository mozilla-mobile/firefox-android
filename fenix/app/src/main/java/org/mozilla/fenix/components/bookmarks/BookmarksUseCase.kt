/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.bookmarks

import androidx.annotation.WorkerThread
import mozilla.appservices.places.BookmarkRoot
import mozilla.appservices.places.uniffi.PlacesApiException
import mozilla.components.concept.storage.BookmarksStorage
import mozilla.components.concept.storage.HistoryStorage
import org.mozilla.fenix.home.recentbookmarks.RecentBookmark

/**
 * Use cases that allow for modifying and retrieving bookmarks.
 */
class BookmarksUseCase(
    bookmarksStorage: BookmarksStorage,
    historyStorage: HistoryStorage,
) {

    class AddBookmarksUseCase internal constructor(private val storage: BookmarksStorage) {

        /**
         * Adds a new bookmark with the provided [url] and [title].
         *
         * @return The result if the operation was executed or not. A bookmark may not be added if
         * one with the identical [url] already exists.
         */
        @WorkerThread
        suspend operator fun invoke(url: String, title: String, position: UInt? = null): Boolean {
            return try {
                val canAdd = storage.getBookmarksWithUrl(url).firstOrNull { it.url == url } == null

                if (canAdd) {
                    storage.addItem(
                        BookmarkRoot.Mobile.id,
                        url = url,
                        title = title,
                        position = position,
                    )
                }
                canAdd
            } catch (e: PlacesApiException.UrlParseFailed) {
                false
            }
        }
    }

    /**
     * Uses for retrieving recently added bookmarks.
     *
     * @param bookmarksStorage [BookmarksStorage] to retrieve the bookmark data.
     * @param historyStorage Optional [HistoryStorage] to retrieve the preview image of a visited
     * page associated with a bookmark.
     */
    class RetrieveRecentBookmarksUseCase internal constructor(
        private val bookmarksStorage: BookmarksStorage,
        private val historyStorage: HistoryStorage? = null,
    ) {
        /**
         * Retrieves a list of recently added bookmarks, if any, up to maximum.
         *
         * @param count The number of recent bookmarks to return.
         * @return a list of [RecentBookmark] if any, up to a number specified by [count].
         */
        @WorkerThread
        suspend operator fun invoke(
            count: Int = DEFAULT_BOOKMARKS_TO_RETRIEVE,
        ): List<RecentBookmark> {
            val currentTime = System.currentTimeMillis()

            val bookmarks = bookmarksStorage.getRecentBookmarks(count)

            val startTime = if (bookmarks.isNotEmpty()) {
                bookmarks.last().dateAdded
            } else {
                currentTime
            }
            // Fetch visit information within the time range of now and the specified maximum age.
            val history = historyStorage?.getDetailedVisits(
                start = startTime,
                end = currentTime,
            )

            return bookmarks
                .map { bookmark ->
                    RecentBookmark(
                        title = bookmark.title,
                        url = bookmark.url,
                        previewImageUrl = history?.find { bookmark.url == it.url }?.previewImageUrl,
                    )
                }
        }
    }

    val addBookmark by lazy { AddBookmarksUseCase(bookmarksStorage) }
    val retrieveRecentBookmarks by lazy {
        RetrieveRecentBookmarksUseCase(
            bookmarksStorage,
            historyStorage,
        )
    }

    companion object {
        // Number of recent bookmarks to retrieve.
        const val DEFAULT_BOOKMARKS_TO_RETRIEVE = 4
    }
}
