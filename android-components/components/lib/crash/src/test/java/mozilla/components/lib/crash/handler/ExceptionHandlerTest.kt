/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.crash.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import mozilla.components.lib.crash.Crash
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.crash.service.CrashReporterService
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ExceptionHandlerTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val scope = TestCoroutineScope(testDispatcher)

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(testDispatcher)

    @Test
    fun `ExceptionHandler forwards crashes to CrashReporter`() {
        var capturedCrash: Crash? = null
        val service = object : CrashReporterService {
            override fun report(crash: Crash.UncaughtExceptionCrash) {
                capturedCrash = crash
            }

            override fun report(crash: Crash.NativeCodeCrash) {
                fail("Did not expect native crash")
            }

            override fun report(throwable: Throwable) {
                fail("Did not expect caught exception")
            }
        }

        val crashReporter = spy(CrashReporter(
            shouldPrompt = CrashReporter.Prompt.NEVER,
            services = listOf(service),
            scope = scope
        ))

        val handler = ExceptionHandler(
            testContext,
            crashReporter)

        val exception = RuntimeException("Hello World")
        handler.uncaughtException(Thread.currentThread(), exception)

        verify(crashReporter).onCrash(any(), any())

        assertNotNull(capturedCrash)

        val crash = capturedCrash as? Crash.UncaughtExceptionCrash
            ?: throw AssertionError("Expected UncaughtExceptionCrash instance")

        assertEquals(exception, crash.throwable)
    }

    @Test
    fun `ExceptionHandler invokes default exception handler`() {
        val defaultExceptionHandler: Thread.UncaughtExceptionHandler = mock()

        val crashReporter = CrashReporter(
                shouldPrompt = CrashReporter.Prompt.NEVER,
                services = listOf(object : CrashReporterService {
                    override fun report(crash: Crash.UncaughtExceptionCrash) {
                    }

                    override fun report(crash: Crash.NativeCodeCrash) {
                    }

                    override fun report(throwable: Throwable) {
                    }
                }),
                scope = scope
        ).install(testContext)

        val handler = ExceptionHandler(
            testContext,
            crashReporter,
            defaultExceptionHandler
        )

        verify(defaultExceptionHandler, never()).uncaughtException(any(), any())

        val exception = RuntimeException()
        handler.uncaughtException(Thread.currentThread(), exception)

        verify(defaultExceptionHandler).uncaughtException(Thread.currentThread(), exception)
    }
}