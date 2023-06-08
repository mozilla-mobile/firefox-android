/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.pocket

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import mozilla.components.service.pocket.PocketStory
import mozilla.components.service.pocket.PocketStory.PocketRecommendedStory
import mozilla.components.service.pocket.PocketStory.PocketSponsoredStory
import mozilla.components.service.pocket.ext.getCurrentFlightImpressions
import org.junit.Test
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState

class DefaultPocketStoriesControllerTest {

    @Test
    fun `GIVEN a category is selected WHEN that same category is clicked THEN deselect it and record telemetry`() {
        val category1 = PocketRecommendedStoriesCategory("cat1", emptyList())
        val category2 = PocketRecommendedStoriesCategory("cat2", emptyList())
        val selections = listOf(PocketRecommendedStoriesSelectedCategory(category2.name))
        val store = spyk(
            AppStore(
                AppState(
                    pocketStoriesCategories = listOf(category1, category2),
                    pocketStoriesCategoriesSelections = selections,
                ),
            ),
        )
        val controller = DefaultPocketStoriesController(mockk(), store)

        controller.handleCategoryClick(category2)
        verify(exactly = 0) { store.dispatch(AppAction.SelectPocketStoriesCategory(category2.name)) }
        verify { store.dispatch(AppAction.DeselectPocketStoriesCategory(category2.name)) }
    }

    @Test
    fun `GIVEN 8 categories are selected WHEN when a new one is clicked THEN the oldest selected is deselected before selecting the new one and record telemetry`() {
        val category1 = PocketRecommendedStoriesSelectedCategory(name = "cat1", selectionTimestamp = 111)
        val category2 = PocketRecommendedStoriesSelectedCategory(name = "cat2", selectionTimestamp = 222)
        val category3 = PocketRecommendedStoriesSelectedCategory(name = "cat3", selectionTimestamp = 333)
        val oldestSelectedCategory = PocketRecommendedStoriesSelectedCategory(name = "oldestSelectedCategory", selectionTimestamp = 0)
        val category4 = PocketRecommendedStoriesSelectedCategory(name = "cat4", selectionTimestamp = 444)
        val category5 = PocketRecommendedStoriesSelectedCategory(name = "cat5", selectionTimestamp = 555)
        val category6 = PocketRecommendedStoriesSelectedCategory(name = "cat6", selectionTimestamp = 678)
        val category7 = PocketRecommendedStoriesSelectedCategory(name = "cat7", selectionTimestamp = 890)
        val newSelectedCategory = PocketRecommendedStoriesSelectedCategory(name = "newSelectedCategory", selectionTimestamp = 654321)
        val store = spyk(
            AppStore(
                AppState(
                    pocketStoriesCategoriesSelections = listOf(
                        category1,
                        category2,
                        category3,
                        category4,
                        category5,
                        category6,
                        category7,
                        oldestSelectedCategory,
                    ),
                ),
            ),
        )
        val controller = DefaultPocketStoriesController(mockk(), store)

        controller.handleCategoryClick(PocketRecommendedStoriesCategory(newSelectedCategory.name))

        verify { store.dispatch(AppAction.DeselectPocketStoriesCategory(oldestSelectedCategory.name)) }
        verify { store.dispatch(AppAction.SelectPocketStoriesCategory(newSelectedCategory.name)) }
    }

    @Test
    fun `GIVEN fewer than 8 categories are selected WHEN when a new one is clicked THEN don't deselect anything but select the newly clicked category and record telemetry`() {
        val category1 = PocketRecommendedStoriesSelectedCategory(name = "cat1", selectionTimestamp = 111)
        val category2 = PocketRecommendedStoriesSelectedCategory(name = "cat2", selectionTimestamp = 222)
        val category3 = PocketRecommendedStoriesSelectedCategory(name = "cat3", selectionTimestamp = 333)
        val oldestSelectedCategory = PocketRecommendedStoriesSelectedCategory(name = "oldestSelectedCategory", selectionTimestamp = 0)
        val category4 = PocketRecommendedStoriesSelectedCategory(name = "cat4", selectionTimestamp = 444)
        val category5 = PocketRecommendedStoriesSelectedCategory(name = "cat5", selectionTimestamp = 555)
        val category6 = PocketRecommendedStoriesSelectedCategory(name = "cat6", selectionTimestamp = 678)
        val store = spyk(
            AppStore(
                AppState(
                    pocketStoriesCategoriesSelections = listOf(
                        category1,
                        category2,
                        category3,
                        category4,
                        category5,
                        category6,
                        oldestSelectedCategory,
                    ),
                ),
            ),
        )
        val newSelectedCategoryName = "newSelectedCategory"
        val controller = DefaultPocketStoriesController(mockk(), store)

        controller.handleCategoryClick(PocketRecommendedStoriesCategory(newSelectedCategoryName))

        verify(exactly = 0) { store.dispatch(AppAction.DeselectPocketStoriesCategory(oldestSelectedCategory.name)) }
        verify { store.dispatch(AppAction.SelectPocketStoriesCategory(newSelectedCategoryName)) }
    }

    @Test
    fun `WHEN a new recommended story is shown THEN update the State`() {
        val store = spyk(AppStore())
        val controller = DefaultPocketStoriesController(mockk(), store)
        val storyShown: PocketRecommendedStory = mockk()
        val storyGridLocation = 1 to 2

        controller.handleStoryShown(storyShown, storyGridLocation)

        verify { store.dispatch(AppAction.PocketStoriesShown(listOf(storyShown))) }
    }

    @Test
    fun `WHEN a new sponsored story is shown THEN update the State and record telemetry`() {
        val store = spyk(AppStore())
        val controller = DefaultPocketStoriesController(mockk(), store)
        val storyShown: PocketSponsoredStory = mockk {
            every { shim.click } returns "testClickShim"
            every { shim.impression } returns "testImpressionShim"
            every { id } returns 123
        }

        mockkStatic("mozilla.components.service.pocket.ext.PocketStoryKt") {
            // Simulate that the story was already shown 3 times.
            every { storyShown.getCurrentFlightImpressions() } returns listOf(2L, 3L, 7L)

            controller.handleStoryShown(storyShown, 1 to 2)

            verify { store.dispatch(AppAction.PocketStoriesShown(listOf(storyShown))) }
        }
    }

    @Test
    fun `WHEN new stories are shown THEN update the State and record telemetry`() {
        val store = spyk(AppStore())
        val controller = DefaultPocketStoriesController(mockk(), store)
        val storiesShown: List<PocketStory> = mockk()

        controller.handleStoriesShown(storiesShown)

        verify { store.dispatch(AppAction.PocketStoriesShown(storiesShown)) }
    }

    @Test
    fun `WHEN a recommended story is clicked THEN open that story's url using HomeActivity and record telemetry`() {
        val story = PocketRecommendedStory(
            title = "",
            url = "testLink",
            imageUrl = "",
            publisher = "",
            category = "",
            timeToRead = 0,
            timesShown = 123,
        )
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())

        controller.handleStoryClicked(story, 1 to 2)

        verify { homeActivity.openToBrowserAndLoad(story.url, true, BrowserDirection.FromHome) }
    }

    @Test
    fun `WHEN a sponsored story is clicked THEN open that story's url using HomeActivity and record telemetry`() {
        val storyClicked = PocketSponsoredStory(
            id = 7,
            title = "",
            url = "testLink",
            imageUrl = "",
            sponsor = "",
            shim = mockk {
                every { click } returns "testClickShim"
                every { impression } returns "testImpressionShim"
            },
            priority = 3,
            caps = mockk(relaxed = true),
        )
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())

        mockkStatic("mozilla.components.service.pocket.ext.PocketStoryKt") {
            // Simulate that the story was already shown 2 times.
            every { storyClicked.getCurrentFlightImpressions() } returns listOf(2L, 3L)

            controller.handleStoryClicked(storyClicked, 2 to 3)

            verify { homeActivity.openToBrowserAndLoad(storyClicked.url, true, BrowserDirection.FromHome) }
        }
    }

    @Test
    fun `WHEN discover more is clicked then open that using HomeActivity and record telemetry`() {
        val link = "http://getpocket.com/explore"
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())

        controller.handleDiscoverMoreClicked(link)

        verify { homeActivity.openToBrowserAndLoad(link, true, BrowserDirection.FromHome) }
    }

    @Test
    fun `WHEN learn more is clicked then open that using HomeActivity and record telemetry`() {
        val link = "https://www.mozilla.org/en-US/firefox/pocket/"
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())

        controller.handleLearnMoreClicked(link)

        verify { homeActivity.openToBrowserAndLoad(link, true, BrowserDirection.FromHome) }
    }

    @Test
    fun `WHEN a story is clicked THEN its link is opened`() {
        val story = PocketRecommendedStory("", "url", "", "", "", 0, 0)
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())

        controller.handleStoryClicked(story, 1 to 2)

        verifyOrder {
            homeActivity.openToBrowserAndLoad(story.url, true, BrowserDirection.FromHome)
        }
    }

    @Test
    fun `WHEN discover more is clicked THEN its link is opened`() {
        val link = "https://discoverMore.link"
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())

        controller.handleDiscoverMoreClicked(link)

        verifyOrder {
            homeActivity.openToBrowserAndLoad(link, true, BrowserDirection.FromHome)
        }
    }

    @Test
    fun `WHEN learn more link is clicked THEN that link is opened`() {
        val link = "https://learnMore.link"
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())

        controller.handleLearnMoreClicked(link)

        verifyOrder {
            homeActivity.openToBrowserAndLoad(link, true, BrowserDirection.FromHome)
        }
    }
}
