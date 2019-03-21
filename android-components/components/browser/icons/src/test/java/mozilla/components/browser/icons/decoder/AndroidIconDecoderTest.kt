/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons.decoder

import android.graphics.Bitmap
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.spy
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidIconDecoderTest {
    @Test
    fun `WHEN decoding PNG THEN returns non-null bitmap`() {
        val decoder = AndroidIconDecoder()

        val bitmap = decoder.decode(
            loadImage("png/mozac.png"),
            targetSize = 32,
            maxSize = 256,
            maxScaleFactor = 2.0f
        )

        assertNotNull(bitmap!!)
    }

    @Test
    fun `WHEN out of memory THEN returns null`() {
        val decoder = spy(AndroidIconDecoder())
        doThrow(OutOfMemoryError()).`when`(decoder).decodeBitmap(any())

        val bitmap = decoder.decode(
            ByteArray(0),
            targetSize = 64,
            maxSize = 256,
            maxScaleFactor = 2.0f
        )

        assertNull(bitmap)
    }

    @Test
    fun `WHEN bitmap width equals zero THEN returns null`() {
        val bitmap: Bitmap = mock()
        `when`(bitmap.width).thenReturn(0)
        `when`(bitmap.height).thenReturn(512)

        val decoder = spy(AndroidIconDecoder())
        doReturn(bitmap).`when`(decoder).decodeBitmap(any())

        val decodedBitmap = decoder.decode(
            ByteArray(0),
            targetSize = 64,
            maxSize = 256,
            maxScaleFactor = 2.0f
        )

        assertNull(decodedBitmap)
    }

    @Test
    fun `WHEN bitmap height equals zero THEN returns null`() {
        val bitmap: Bitmap = mock()
        `when`(bitmap.width).thenReturn(512)
        `when`(bitmap.height).thenReturn(0)

        val decoder = spy(AndroidIconDecoder())
        doReturn(bitmap).`when`(decoder).decodeBitmap(any())

        val decodedBitmap = decoder.decode(
            ByteArray(0),
            targetSize = 64,
            maxSize = 256,
            maxScaleFactor = 2.0f
        )

        assertNull(decodedBitmap)
    }

    @Test
    fun `WHEN decoding null bitmap THEN returns null`() {
        val decoder = spy(AndroidIconDecoder())
        doReturn(null).`when`(decoder).decodeBitmap(any())

        val decodedBitmap = decoder.decode(
            ByteArray(0),
            targetSize = 64,
            maxSize = 256,
            maxScaleFactor = 2.0f
        )

        assertNull(decodedBitmap)
    }

    @Test
    fun `WHEN bitmap width too small THEN returns null`() {
        val bitmap: Bitmap = mock()
        `when`(bitmap.width).thenReturn(50)
        `when`(bitmap.height).thenReturn(250)

        val decoder = spy(AndroidIconDecoder())
        doReturn(bitmap).`when`(decoder).decodeBitmap(any())

        val decodedBitmap = decoder.decode(
            ByteArray(0),
            targetSize = 256,
            maxSize = 256,
            maxScaleFactor = 2.0f
        )

        assertNull(decodedBitmap)
    }

    @Test
    fun `WHEN bitmap height too small THEN returns null`() {
        val bitmap: Bitmap = mock()
        `when`(bitmap.width).thenReturn(250)
        `when`(bitmap.height).thenReturn(50)

        val decoder = spy(AndroidIconDecoder())
        doReturn(bitmap).`when`(decoder).decodeBitmap(any())

        val decodedBitmap = decoder.decode(
            ByteArray(0),
            targetSize = 256,
            maxSize = 256,
            maxScaleFactor = 2.0f
        )

        assertNull(decodedBitmap)
    }

    @Test
    fun `WHEN bitmap width too large THEN returns null`() {
        val bitmap: Bitmap = mock()
        `when`(bitmap.width).thenReturn(2000)
        `when`(bitmap.height).thenReturn(250)

        val decoder = spy(AndroidIconDecoder())
        doReturn(bitmap).`when`(decoder).decodeBitmap(any())

        val decodedBitmap = decoder.decode(
            ByteArray(0),
            targetSize = 256,
            maxSize = 256,
            maxScaleFactor = 2.0f
        )

        assertNull(decodedBitmap)
    }

    @Test
    fun `WHEN bitmap height too large THEN returns null`() {
        val bitmap: Bitmap = mock()
        `when`(bitmap.width).thenReturn(250)
        `when`(bitmap.height).thenReturn(2000)

        val decoder = spy(AndroidIconDecoder())
        doReturn(bitmap).`when`(decoder).decodeBitmap(any())

        val decodedBitmap = decoder.decode(
            ByteArray(0),
            targetSize = 256,
            maxSize = 256,
            maxScaleFactor = 2.0f
        )

        assertNull(decodedBitmap)
    }

    private fun loadImage(fileName: String): ByteArray =
        javaClass.getResourceAsStream("/$fileName")!!
            .buffered()
            .readBytes()
}
