/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import kotlinx.coroutines.runBlocking
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.DocumentType
import mozilla.components.concept.storage.HistoryMetadata
import mozilla.components.concept.storage.HistoryMetadataKey
import mozilla.components.concept.storage.HistoryMetadataStorage
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class HistoryMetadataSuggestionProviderTest {
    private val historyEntry = HistoryMetadata(
        key = HistoryMetadataKey("http://www.mozilla.com", null, null),
        title = "mozilla",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        totalViewTime = 10,
        documentType = DocumentType.Regular
    )

    @Test
    fun `provider returns empty list when text is empty`() = runBlocking {
        val provider = HistoryMetadataSuggestionProvider(mock(), mock())

        val suggestions = provider.onInputChanged("")
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `provider returns suggestions from configured history storage`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()

        whenever(storage.queryHistoryMetadata("moz", METADATA_SUGGESTION_LIMIT)).thenReturn(listOf(historyEntry))

        val provider = HistoryMetadataSuggestionProvider(storage, mock())
        val suggestions = provider.onInputChanged("moz")
        assertEquals(1, suggestions.size)
        assertEquals(historyEntry.key.url, suggestions[0].description)
        assertEquals(historyEntry.title, suggestions[0].title)
    }

    @Test
    fun `provider limits number of returned suggestions to 5 by default`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        val historyEntries = mutableListOf<HistoryMetadata>().apply {
            for (i in 1..10) {
                add(historyEntry)
            }
        }
        whenever(storage.queryHistoryMetadata("moz", METADATA_SUGGESTION_LIMIT)).thenReturn(historyEntries)
        val provider = HistoryMetadataSuggestionProvider(storage, mock())

        val suggestions = provider.onInputChanged("moz")
        assertEquals(5, suggestions.size)
    }

    @Test
    fun `provider allows lowering from outside the number of returned suggestions`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        val historyEntries = mutableListOf<HistoryMetadata>().apply {
            for (i in 1..10) {
                add(historyEntry)
            }
        }
        whenever(storage.queryHistoryMetadata("moz", METADATA_SUGGESTION_LIMIT)).thenReturn(historyEntries)
        val provider = HistoryMetadataSuggestionProvider(
            historyStorage = storage, loadUrlUseCase = mock(), maxNumberOfResults = 2
        )

        val suggestions = provider.onInputChanged("moz")
        assertEquals(2, suggestions.size)
    }

    @Test
    fun `provider doesn't allow increasing from outside the number of returned suggestions to past the default of 20`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        val historyEntries = mutableListOf<HistoryMetadata>().apply {
            for (i in 1..10) {
                add(historyEntry)
            }
        }
        whenever(storage.queryHistoryMetadata("moz", METADATA_SUGGESTION_LIMIT)).thenReturn(historyEntries)
        val provider = HistoryMetadataSuggestionProvider(
            historyStorage = storage, loadUrlUseCase = mock(), maxNumberOfResults = 8
        )

        val suggestions = provider.onInputChanged("moz")
        assertEquals(5, suggestions.size)
    }

    @Test
    fun `provider only as suggestions pages on which users actually spent some time`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        val historyEntries = mutableListOf<HistoryMetadata>().apply {
            add(historyEntry)
            add(historyEntry.copy(totalViewTime = 0))
        }
        whenever(storage.queryHistoryMetadata("moz", METADATA_SUGGESTION_LIMIT)).thenReturn(historyEntries)
        val provider = HistoryMetadataSuggestionProvider(storage, mock())

        val suggestions = provider.onInputChanged("moz")
        assertEquals(1, suggestions.size)
    }

    @Test
    fun `provider suggestion should not get cleared when text changes`() {
        val provider = HistoryMetadataSuggestionProvider(mock(), mock())
        assertFalse(provider.shouldClearSuggestions)
    }

    @Test
    fun `provider calls speculative connect for URL of highest scored suggestion`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        val engine: Engine = mock()
        val provider = HistoryMetadataSuggestionProvider(storage, mock(), engine = engine)

        var suggestions = provider.onInputChanged("")
        assertTrue(suggestions.isEmpty())
        verify(engine, never()).speculativeConnect(anyString())

        whenever(storage.queryHistoryMetadata("moz", METADATA_SUGGESTION_LIMIT)).thenReturn(listOf(historyEntry))

        suggestions = provider.onInputChanged("moz")
        assertEquals(1, suggestions.size)
        verify(engine, times(1)).speculativeConnect(historyEntry.key.url)
    }
}
