/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.search

import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.SearchEngineManager
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.test.any
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SearchUseCasesTest {

    private lateinit var searchEngine: SearchEngine
    private lateinit var searchEngineManager: SearchEngineManager
    private lateinit var sessionManager: SessionManager
    private lateinit var useCases: SearchUseCases

    @Before
    fun setup() {
        searchEngine = mock(SearchEngine::class.java)
        searchEngineManager = mock(SearchEngineManager::class.java)
        sessionManager = mock(SessionManager::class.java)
        useCases = SearchUseCases(RuntimeEnvironment.application, searchEngineManager, sessionManager)
    }

    @Test
    fun defaultSearch() {
        val searchTerms = "mozilla android"
        val searchUrl = "http://search-url.com?$searchTerms"

        val session = Session("mozilla.org")
        val engineSession = mock(EngineSession::class.java)

        `when`(searchEngine.buildSearchUrl(searchTerms)).thenReturn(searchUrl)
        `when`(searchEngineManager.getDefaultSearchEngine(RuntimeEnvironment.application)).thenReturn(searchEngine)
        `when`(sessionManager.getOrCreateEngineSession(session)).thenReturn(engineSession)

        useCases.defaultSearch.invoke(searchTerms, session)

        assertEquals(searchTerms, session.searchTerms)
        verify(engineSession).loadUrl(searchUrl)
    }

    @Test
    fun defaultSearchOnNewSession() {
        val searchTerms = "mozilla android"
        val searchUrl = "http://search-url.com?$searchTerms"

        val engineSession = mock(EngineSession::class.java)
        `when`(searchEngine.buildSearchUrl(searchTerms)).thenReturn(searchUrl)
        `when`(searchEngineManager.getDefaultSearchEngine(RuntimeEnvironment.application)).thenReturn(searchEngine)
        `when`(sessionManager.getOrCreateEngineSession(any())).thenReturn(engineSession)

        useCases.defaultSearch.invoke(searchTerms, Session.Source.NEW_TAB)
        verify(engineSession).loadUrl(searchUrl)
    }
}