/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mozilla.appservices.places.BookmarkRoot
import mozilla.appservices.places.PlacesException
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class PlacesBookmarksStorageTest {
    private lateinit var bookmarks: PlacesBookmarksStorage

    @Before
    fun setup() = runBlocking {
        bookmarks = PlacesBookmarksStorage(testContext)
        // There's a database on disk which needs to be cleaned up between tests.
        bookmarks.writer.deleteEverything()
    }

    @After
    fun cleanup() = runBlocking {
        bookmarks.cleanup()
    }

    @Test
    fun `get bookmarks tree by root, recursive or not`() = runBlocking {
        val tree = bookmarks.getTree(BookmarkRoot.Root.id)!!
        assertEquals(BookmarkRoot.Root.id, tree.guid)
        assertNotNull(tree.children)
        assertEquals(4, tree.children!!.size)

        var children = tree.children!!.map { it.guid }
        assertTrue(BookmarkRoot.Mobile.id in children)
        assertTrue(BookmarkRoot.Unfiled.id in children)
        assertTrue(BookmarkRoot.Toolbar.id in children)
        assertTrue(BookmarkRoot.Menu.id in children)

        // Non-recursive means children of children aren't fetched.
        for (child in tree.children!!) {
            assertNull(child.children)
            assertEquals(BookmarkRoot.Root.id, child.parentGuid)
            assertEquals(BookmarkNodeType.FOLDER, child.type)
        }

        val deepTree = bookmarks.getTree(BookmarkRoot.Root.id, true)!!
        assertEquals(BookmarkRoot.Root.id, deepTree.guid)
        assertNotNull(deepTree.children)
        assertEquals(4, deepTree.children!!.size)

        children = deepTree.children!!.map { it.guid }
        assertTrue(BookmarkRoot.Mobile.id in children)
        assertTrue(BookmarkRoot.Unfiled.id in children)
        assertTrue(BookmarkRoot.Toolbar.id in children)
        assertTrue(BookmarkRoot.Menu.id in children)

        // Recursive means children of children are fetched.
        for (child in deepTree.children!!) {
            // For an empty tree, we expect to see empty lists.
            assertEquals(emptyList<BookmarkNode>(), child.children)
            assertEquals(BookmarkRoot.Root.id, child.parentGuid)
            assertEquals(BookmarkNodeType.FOLDER, child.type)
        }
    }

    @Test
    fun `bookmarks APIs smoke testing - basic operations`() = runBlocking {
        val url = "http://www.mozilla.org"

        assertEquals(emptyList<BookmarkNode>(), bookmarks.getBookmarksWithUrl(url))
        assertEquals(emptyList<BookmarkNode>(), bookmarks.searchBookmarks("mozilla"))

        val insertedItem = bookmarks.addItem(BookmarkRoot.Mobile.id, url, "Mozilla", 5)

        with (bookmarks.getBookmarksWithUrl(url)) {
            assertEquals(1, this.size)
            with(this[0]) {
                assertEquals(insertedItem, this.guid)
                assertEquals(BookmarkNodeType.ITEM, this.type)
                assertEquals("Mozilla", this.title)
                assertEquals(BookmarkRoot.Mobile.id, this.parentGuid)
                // Clamped to actual range. 'Mobile' was empty, so we get 0 back.
                assertEquals(0, this.position)
                assertEquals("http://www.mozilla.org/", this.url)
            }
        }

        val folderGuid = bookmarks.addFolder(BookmarkRoot.Mobile.id, "Test Folder", null)
        bookmarks.updateNode(insertedItem, BookmarkInfo(
            parentGuid = folderGuid, title = null, position = -3, url = null
        ))
        with (bookmarks.getBookmarksWithUrl(url)) {
            assertEquals(1, this.size)
            with(this[0]) {
                assertEquals(insertedItem, this.guid)
                assertEquals(BookmarkNodeType.ITEM, this.type)
                assertEquals("Mozilla", this.title)
                assertEquals(folderGuid, this.parentGuid)
                assertEquals(0, this.position)
                assertEquals("http://www.mozilla.org/", this.url)
            }
        }

        val separatorGuid = bookmarks.addSeparator(folderGuid, 1)
        with (bookmarks.getTree(folderGuid)!!) {
            assertEquals(2, this.children!!.size)
            assertEquals(BookmarkNodeType.SEPARATOR, this.children!![1].type)
        }

        assertTrue(bookmarks.deleteNode(separatorGuid))
        with (bookmarks.getTree(folderGuid)!!) {
            assertEquals(1, this.children!!.size)
            assertEquals(BookmarkNodeType.ITEM, this.children!![0].type)
        }

        with (bookmarks.searchBookmarks("mozilla")) {
            assertEquals(1, this.size)
            assertEquals("http://www.mozilla.org/", this[0].url)
        }

        with (bookmarks.getBookmark(folderGuid)!!) {
            assertEquals(folderGuid, this.guid)
            assertEquals("Test Folder", this.title)
            assertEquals(BookmarkRoot.Mobile.id, this.parentGuid)
        }

        assertTrue(bookmarks.deleteNode(folderGuid))

        for (root in listOf(
            BookmarkRoot.Mobile, BookmarkRoot.Root, BookmarkRoot.Menu, BookmarkRoot.Toolbar, BookmarkRoot.Unfiled)
        ) {
            try {
                bookmarks.deleteNode(root.id)
                fail("Expected root deletion for ${root.id} to fail")
            } catch (e: PlacesException) {}
        }

        with (bookmarks.searchBookmarks("mozilla")) {
            assertTrue(this.isEmpty())
        }
    }
}
