/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import mozilla.components.browser.menu.view.MenuButton
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import mozilla.components.concept.base.images.ImageLoadRequest
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.IncompleteRedesignToolbarFeature
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.components.toolbar.navbar.BottomToolbarContainerView
import org.mozilla.fenix.databinding.TabPreviewBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.theme.ThemeManager
import kotlin.math.min

/**
 * A 'dummy' view of a tab used by [ToolbarGestureHandler] to support switching tabs by swiping the address bar.
 *
 * The view is responsible for showing the preview and a dummy toolbar of the inactive tab during swiping.
 */
class TabPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : CoordinatorLayout(context, attrs, defStyle) {

    private val binding = TabPreviewBinding.inflate(LayoutInflater.from(context), this)
    private val thumbnailLoader = ThumbnailLoader(context.components.core.thumbnailStorage)

    init {
        val isToolbarAtTop = context.settings().toolbarPosition == ToolbarPosition.TOP
        if (isToolbarAtTop) {
            binding.fakeToolbar.updateLayoutParams<LayoutParams> {
                gravity = Gravity.TOP
            }

            binding.fakeToolbar.background = AppCompatResources.getDrawable(
                context,
                ThemeManager.resolveAttribute(R.attr.bottomBarBackgroundTop, context),
            )
        }

        val isNavBarEnabled = IncompleteRedesignToolbarFeature(context.settings()).isEnabled
        binding.tabButton.isVisible = !isNavBarEnabled
        binding.menuButton.isVisible = !isNavBarEnabled

        if (isNavBarEnabled) {
            BottomToolbarContainerView(
                context = context,
                parent = this,
                androidToolbarView = if (!isToolbarAtTop) binding.fakeToolbar else null,
                menuButton = MenuButton(context),
            )
            removeView(binding.fakeToolbar)
        }

        // Change view properties to avoid confusing the UI tests
        binding.tabButton.findViewById<View>(R.id.counter_box).id = View.NO_ID
        binding.tabButton.findViewById<View>(R.id.counter_text).id = View.NO_ID
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val store = context.components.core.store
        store.state.selectedTab?.let {
            val count = store.state.getNormalOrPrivateTabs(it.content.private).size
            binding.tabButton.setCount(count)
        }

        binding.previewThumbnail.translationY = if (context.settings().toolbarPosition == ToolbarPosition.TOP) {
            binding.fakeToolbar.height.toFloat()
        } else {
            0f
        }
    }

    /**
     * Load a preview for a thumbnail.
     */
    fun loadPreviewThumbnail(thumbnailId: String, isPrivate: Boolean) {
        doOnNextLayout {
            val previewThumbnail = binding.previewThumbnail
            val thumbnailSize = min(previewThumbnail.height, previewThumbnail.width)
            thumbnailLoader.loadIntoView(
                previewThumbnail,
                ImageLoadRequest(thumbnailId, thumbnailSize, isPrivate),
            )
        }
    }
}
