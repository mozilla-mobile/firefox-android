/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu2.ext

import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import androidx.core.widget.PopupWindowCompat
import mozilla.components.browser.menu2.R
import mozilla.components.concept.menu.MenuStyle
import mozilla.components.concept.menu.Orientation
import mozilla.components.support.ktx.android.view.isRTL

internal fun PopupWindow.displayPopup(
    containerView: View,
    anchor: View,
    preferredOrientation: Orientation? = null,
    forceOrientation: Boolean = false,
    style: MenuStyle? = null,
) {
    val menuPositioningData = inferMenuPositioningData(
        containerView,
        anchor,
        style
    )

    // Popup window does not need a input method. This avoids keyboard flicker when menu is opened.
    inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED

    // Try to use the preferred orientation, if doesn't fit fallback to the best fit.
    when {
        preferredOrientation == Orientation.DOWN && (menuPositioningData.fitsDown || forceOrientation) ->
            showPopupWithDownOrientation(anchor, menuPositioningData)
        preferredOrientation == Orientation.UP && (menuPositioningData.fitsUp || forceOrientation) ->
            showPopupWithUpOrientation(anchor, menuPositioningData)
        else -> showPopupWhereBestFits(anchor, menuPositioningData)
    }
}

@Suppress("LongParameterList")
private fun PopupWindow.showPopupWhereBestFits(
    anchor: View,
    menuPositioningData: MenuPositioningData,
) {
    with(menuPositioningData) {
        when {
            !fitsUp && !fitsDown -> showAtAnchorLocation(anchor, this)
            fitsDown -> showPopupWithDownOrientation(anchor, this)
            else -> showPopupWithUpOrientation(anchor, this)
        }
    }
}

private fun PopupWindow.showPopupWithUpOrientation(
    anchor: View,
    menuPositioningData: MenuPositioningData,
) {
    animationStyle = if (menuPositioningData.reversed) {
        R.style.Mozac_Browser_Menu2_Animation_OverflowMenuLeftBottom
    } else {
        R.style.Mozac_Browser_Menu2_Animation_OverflowMenuRightBottom
    }

    val yOffset = -menuPositioningData.containerHeight - menuPositioningData.verticalOffset
    showAsDropDown(anchor, menuPositioningData.horizontalOffset, yOffset)
}

private fun PopupWindow.showPopupWithDownOrientation(
    anchor: View,
    menuPositioningData: MenuPositioningData
) {
    // Apply the best fit animation style based on positioning
    animationStyle = if (menuPositioningData.reversed) {
        R.style.Mozac_Browser_Menu2_Animation_OverflowMenuLeftTop
    } else {
        R.style.Mozac_Browser_Menu2_Animation_OverflowMenuRightTop
    }

    PopupWindowCompat.setOverlapAnchor(this, true)
    showAsDropDown(anchor, menuPositioningData.horizontalOffset, menuPositioningData.verticalOffset)
}

private fun PopupWindow.showAtAnchorLocation(
    anchor: View,
    menuPositioningData: MenuPositioningData
) {
    val anchorPosition = IntArray(2)

    // Apply the best fit animation style based on positioning
    animationStyle = if (menuPositioningData.reversed) {
        R.style.Mozac_Browser_Menu2_Animation_OverflowMenuLeft
    } else {
        R.style.Mozac_Browser_Menu2_Animation_OverflowMenuRight
    }

    anchor.getLocationOnScreen(anchorPosition)
    val (x, y) = anchorPosition

    PopupWindowCompat.setOverlapAnchor(this, true)
    showAtLocation(anchor, Gravity.START or Gravity.TOP, x + menuPositioningData.horizontalOffset, y + menuPositioningData.verticalOffset)
}
