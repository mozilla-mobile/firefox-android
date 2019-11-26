/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu.item

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.R

@Suppress("Deprecation")
private val defaultHighlight = BrowserMenuHighlightableItem.Highlight(0, 0, 0, 0)

/**
 * A menu item for displaying text with an image icon and a highlight state which sets the
 * background of the menu item and a second image icon to the right of the text.
 *
 * @param label The default visible label of this menu item.
 * @param startImageResource ID of a drawable resource to be shown as a leftmost icon.
 * @param iconTintColorResource Optional ID of color resource to tint the icon.
 * @param textColorResource Optional ID of color resource to tint the text.
 * @param highlight Highlight object representing how the menu item will be displayed when highlighted.
 * @param isHighlighted Whether or not to display the highlight
 * @param listener Callback to be invoked when this menu item is clicked.
 */
class BrowserMenuHighlightableItem(
    private val label: String,
    @DrawableRes private val startImageResource: Int,
    @ColorRes private val iconTintColorResource: Int = NO_ID,
    @ColorRes private val textColorResource: Int = NO_ID,
    val highlight: BrowserMenuHighlight,
    val isHighlighted: () -> Boolean = { true },
    private val listener: () -> Unit = {}
) : BrowserMenuImageText(
    label,
    startImageResource,
    iconTintColorResource,
    textColorResource,
    listener
) {

    @Deprecated("Use the new constructor")
    @Suppress("Deprecation") // Constructor uses old highlight type
    constructor(
        label: String,
        @DrawableRes
        imageResource: Int,
        @ColorRes
        iconTintColorResource: Int = NO_ID,
        @ColorRes
        textColorResource: Int = NO_ID,
        highlight: Highlight? = null,
        listener: () -> Unit = {}
    ) : this(
        label,
        imageResource,
        iconTintColorResource,
        textColorResource,
        highlight ?: defaultHighlight,
        { highlight != null },
        listener
    )

    private var wasHighlighted = false

    override fun getLayoutResource() = R.layout.mozac_browser_menu_highlightable_item

    override fun bind(menu: BrowserMenu, view: View) {
        super.bind(menu, view)

        val endImageView = view.findViewById<AppCompatImageView>(R.id.end_image)
        endImageView.setTintResource(iconTintColorResource)

        val highlightedTextView = view.findViewById<TextView>(R.id.highlight_text)
        highlightedTextView.text = highlight.label ?: label

        wasHighlighted = isHighlighted()
        updateHighlight(view, wasHighlighted)
    }

    override fun invalidate(view: View) {
        val isNowHighlighted = isHighlighted()
        if (isNowHighlighted != wasHighlighted) {
            wasHighlighted = isNowHighlighted
            updateHighlight(view, isNowHighlighted)
        }
    }

    private fun updateHighlight(view: View, isHighlighted: Boolean) {
        val startImageView = view.findViewById<AppCompatImageView>(R.id.image)
        val endImageView = view.findViewById<AppCompatImageView>(R.id.end_image)
        val notificationDotView = view.findViewById<AppCompatImageView>(R.id.notification_dot)
        val textView = view.findViewById<TextView>(R.id.text)
        val highlightedTextView = view.findViewById<TextView>(R.id.highlight_text)

        if (isHighlighted) {
            @Suppress("Deprecation")
            when (highlight) {
                is BrowserMenuHighlight.HighPriority -> {
                    textView.visibility = View.INVISIBLE
                    highlightedTextView.visibility = View.VISIBLE
                    view.setBackgroundColor(highlight.backgroundTint)
                    if (highlight.endImageResource != NO_ID) {
                        endImageView.setImageResource(highlight.endImageResource)
                    }
                    endImageView.visibility = View.VISIBLE
                }
                is BrowserMenuHighlight.LowPriority -> {
                    textView.visibility = View.INVISIBLE
                    highlightedTextView.visibility = View.VISIBLE
                    notificationDotView.imageTintList = ColorStateList.valueOf(highlight.notificationTint)
                    notificationDotView.visibility = View.VISIBLE
                }
                is BrowserMenuHighlight.ClassicHighlight -> {
                    view.setBackgroundResource(highlight.backgroundResource)
                    if (highlight.startImageResource != NO_ID) {
                        startImageView.setImageResource(highlight.startImageResource)
                    }
                    if (highlight.endImageResource != NO_ID) {
                        endImageView.setImageResource(highlight.endImageResource)
                    }
                    endImageView.visibility = View.VISIBLE
                }
            }
        } else {
            textView.visibility = View.VISIBLE
            highlightedTextView.visibility = View.INVISIBLE
            view.background = null
            endImageView.setImageDrawable(null)
            endImageView.visibility = View.GONE
            notificationDotView.visibility = View.GONE
        }
    }

    /**
     * Described how to display a [BrowserMenuHighlightableItem] when it is highlighted.
     * Replaced by [BrowserMenuHighlight] which lets a priority be specified.
     */
    @Deprecated("Replace with BrowserMenuHighlight.LowPriority or BrowserMenuHighlight.HighPriority")
    @Suppress("Deprecation")
    class Highlight(
        @DrawableRes startImageResource: Int = NO_ID,
        @DrawableRes endImageResource: Int = NO_ID,
        @DrawableRes backgroundResource: Int,
        @ColorRes colorResource: Int
    ) : BrowserMenuHighlight.ClassicHighlight(
        startImageResource,
        endImageResource,
        backgroundResource,
        colorResource
    )
}
