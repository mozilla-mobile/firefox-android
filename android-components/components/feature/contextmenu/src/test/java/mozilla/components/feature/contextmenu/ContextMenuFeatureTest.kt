/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.contextmenu

import android.content.Context
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.HitResult
import mozilla.components.support.base.observer.Consumable
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ContextMenuFeatureTest {
    private val context: Context get() = RuntimeEnvironment.application

    @Test
    fun `New HitResult for selected session will cause fragment transaction`() {
        val sessionManager = SessionManager(mock())
        val session = Session("https://www.mozilla.org").apply { sessionManager.add(this) }

        val fragmentManager = mockFragmentManager()

        val feature = ContextMenuFeature(
            fragmentManager, sessionManager, ContextMenuCandidate.defaultCandidates(context, mock(), mock()))
        feature.start()

        val hitResult = HitResult.UNKNOWN("https://www.mozilla.org")
        session.hitResult = Consumable.from(hitResult)

        verify(fragmentManager).beginTransaction()
    }

    @Test
    fun `New HitResult for selected session will not cause fragment transaction if feature is stopped`() {
        val sessionManager = SessionManager(mock())
        val session = Session("https://www.mozilla.org").apply { sessionManager.add(this) }

        val fragmentManager = mockFragmentManager()

        val feature = ContextMenuFeature(
            fragmentManager, sessionManager, ContextMenuCandidate.defaultCandidates(context, mock(), mock()))
        feature.start()
        feature.stop()

        val hitResult = HitResult.UNKNOWN("https://www.mozilla.org")
        session.hitResult = Consumable.from(hitResult)

        verify(fragmentManager, never()).beginTransaction()
    }

    @Test
    fun `Feature will re-attach to already existing fragment`() {
        val sessionManager = SessionManager(mock())
        val session = Session("https://www.mozilla.org").apply {
            sessionManager.add(this)
            hitResult = Consumable.from(HitResult.UNKNOWN("https://www.mozilla.org"))
        }

        val fragment: ContextMenuFragment = mock()
        doReturn(session.id).`when`(fragment).sessionId

        val fragmentManager: FragmentManager = mock()
        doReturn(fragment).`when`(fragmentManager).findFragmentByTag(any())

        val feature = ContextMenuFeature(
            fragmentManager, sessionManager, ContextMenuCandidate.defaultCandidates(context, mock(), mock()))

        feature.start()

        verify(fragment).feature = feature
    }

    @Test
    fun `Already existing fragment will be removed if session has no HitResult set anymore`() {
        val sessionManager = SessionManager(mock())
        val session = Session("https://www.mozilla.org").apply {
            sessionManager.add(this)
        }

        val fragment: ContextMenuFragment = mock()
        doReturn(session.id).`when`(fragment).sessionId

        val transaction: FragmentTransaction = mock()

        val fragmentManager: FragmentManager = mock()
        doReturn(fragment).`when`(fragmentManager).findFragmentByTag(any())
        doReturn(transaction).`when`(fragmentManager).beginTransaction()
        doReturn(transaction).`when`(transaction).remove(fragment)

        val feature = ContextMenuFeature(
            fragmentManager, sessionManager, ContextMenuCandidate.defaultCandidates(context, mock(), mock()))

        feature.start()

        verify(fragmentManager).beginTransaction()
        verify(transaction).remove(fragment)
    }

    fun `Already existing fragment will be removed if session does not exist anymore`() {
        val sessionManager = SessionManager(mock())

        val fragment: ContextMenuFragment = mock()
        doReturn(UUID.randomUUID().toString()).`when`(fragment).sessionId

        val transaction: FragmentTransaction = mock()

        val fragmentManager: FragmentManager = mock()
        doReturn(fragment).`when`(fragmentManager).findFragmentByTag(any())
        doReturn(transaction).`when`(fragmentManager).beginTransaction()
        doReturn(transaction).`when`(transaction).remove(fragment)

        val feature = ContextMenuFeature(
            fragmentManager, sessionManager, ContextMenuCandidate.defaultCandidates(context, mock(), mock()))

        feature.start()

        verify(fragmentManager).beginTransaction()
        verify(transaction).remove(fragment)
    }

    @Test
    fun `No dialog will be shown if no item wants to be shown`() {
        val fragmentManager = mockFragmentManager()

        val candidate = ContextMenuCandidate(
            id = "test-id",
            label = "Test Item",
            showFor = { _, _ -> false },
            action = { _, _ -> Unit }
        )

        val feature = ContextMenuFeature(
            fragmentManager, mock(), listOf(candidate))

        feature.onLongPress(
            Session("https://www.mozilla.org"),
            HitResult.UNKNOWN("https://www.mozilla.org"))

        verifyNoMoreInteractions(fragmentManager)
    }

    @Test
    fun `Selecting context menu item will consume HitResult`() {
        val sessionManager = SessionManager(mock())
        val session = Session("https://www.mozilla.org").apply {
            sessionManager.add(this)
            hitResult = Consumable.from(HitResult.UNKNOWN("https://www.mozilla.org"))
        }

        val feature = ContextMenuFeature(
            mock(), sessionManager, ContextMenuCandidate.defaultCandidates(context, mock(), mock()))

        assertFalse(session.hitResult.isConsumed())

        feature.onMenuCancelled(session.id)

        assertTrue(session.hitResult.isConsumed())
    }

    @Test
    fun `Selecting context menu item will invoke action of candidate and consume HitResult`() {
        val sessionManager = SessionManager(mock())
        val session = Session("https://www.mozilla.org").apply {
            sessionManager.add(this)
            hitResult = Consumable.from(HitResult.UNKNOWN("https://www.mozilla.org"))
        }

        var actionInvoked = false

        val candidate = ContextMenuCandidate(
            id = "test-id",
            label = "Test Item",
            showFor = { _, _ -> true },
            action = { _, _ -> actionInvoked = true })

        val feature = ContextMenuFeature(mock(), sessionManager, listOf(candidate))

        assertFalse(session.hitResult.isConsumed())
        assertFalse(actionInvoked)

        feature.onMenuItemSelected(session.id, candidate.id)

        assertTrue(session.hitResult.isConsumed())
        assertTrue(actionInvoked)
    }

    private fun mockFragmentManager(): FragmentManager {
        val fragmentManager: FragmentManager = mock()

        val transaction: FragmentTransaction = mock()
        doReturn(transaction).`when`(fragmentManager).beginTransaction()

        return fragmentManager
    }
}
