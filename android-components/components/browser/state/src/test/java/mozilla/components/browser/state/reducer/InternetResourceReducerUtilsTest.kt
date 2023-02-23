package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import org.junit.Assert
import org.junit.Test

class InternetResourceReducerUtilsTest {
    @Test
    fun `updateTheContentState will return a new BrowserState with updated ContentState`() {
        val initialContentState = ContentState("emptyStateUrl")
        val browserState = BrowserState(tabs = listOf(TabSessionState("tabId", initialContentState)))

        val result = updateTheContentState(browserState, "tabId") { it.copy(url = "updatedUrl") }

        Assert.assertFalse(browserState == result)
        Assert.assertEquals("updatedUrl", result.tabs[0].content.url)
    }
}
