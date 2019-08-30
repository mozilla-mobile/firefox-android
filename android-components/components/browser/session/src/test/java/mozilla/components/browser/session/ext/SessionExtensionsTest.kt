package mozilla.components.browser.session.ext

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.session.Session
import mozilla.components.browser.state.state.CustomTabConfig
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalStateException

@RunWith(AndroidJUnit4::class)
class SessionExtensionsTest {

    @Test
    fun `toCustomTabSessionState - Can convert custom tab session`() {
        val session = Session("https://mozilla.org")
        session.customTabConfig = CustomTabConfig()

        val customTabState = session.toCustomTabSessionState()
        assertEquals(customTabState.id, session.id)
        assertEquals(customTabState.content.url, session.url)
        assertSame(customTabState.config, session.customTabConfig)
    }

    @Test(expected = IllegalStateException::class)
    fun `toCustomTabSessionState - Throws exception when converting a non-custom tab session`() {
        val session: Session = mock()
        session.toCustomTabSessionState()
    }
}