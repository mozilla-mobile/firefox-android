/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.studies

import androidx.fragment.app.FragmentActivity
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.service.nimbus.NimbusApi
import org.junit.Before
import org.junit.Test
import org.mozilla.experiments.nimbus.internal.EnrolledExperiment
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.ext.openToBrowser
import org.mozilla.fenix.ext.openToBrowserAndLoad

class DefaultStudiesInteractorTest {
    @RelaxedMockK
    private lateinit var activity: HomeActivity

    @RelaxedMockK
    private lateinit var experiments: NimbusApi

    private lateinit var interactor: DefaultStudiesInteractor

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        interactor = spyk(DefaultStudiesInteractor(activity, experiments))
    }

    @Test
    fun `WHEN calling openWebsite THEN delegate to the homeActivity`() {
        val url = ""

        mockkStatic(FragmentActivity::openToBrowser)
        every {
            activity.openToBrowserAndLoad(
                navController = activity.navHost.navController,
                searchTermOrURL = url,
                newTab = true,
                from = BrowserDirection.FromStudiesFragment,
                browsingMode = activity.browsingModeManager.mode,
            )
        } just Runs

        interactor.openWebsite(url)

        verify {
            activity.openToBrowserAndLoad(
                navController = activity.navHost.navController,
                searchTermOrURL = url,
                newTab = true,
                from = BrowserDirection.FromStudiesFragment,
                browsingMode = activity.browsingModeManager.mode,
            )
        }
    }

    @Test
    fun `WHEN calling removeStudy THEN delegate to the NimbusApi`() {
        val experiment = mockk<EnrolledExperiment>(relaxed = true)

        every { experiment.slug } returns "slug"
        every { interactor.killApplication() } just runs

        interactor.removeStudy(experiment)

        verify {
            experiments.optOut("slug")
        }
    }
}
