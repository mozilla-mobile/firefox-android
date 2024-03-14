/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar.internal

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import mozilla.components.browser.toolbar.R
import mozilla.components.concept.toolbar.Toolbar

/**
 * A container [View] for displaying [Toolbar.Action] objects.
 */
internal class ActionContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val actions = mutableListOf<ActionWrapper>()
    private var actionSize: Int? = null

    init {
        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL
        visibility = View.GONE

        context.obtainStyledAttributes(
            attrs,
            R.styleable.ActionContainer,
            defStyleAttr,
            0,
        ).run {
            actionSize = attrs?.let {
                getDimensionPixelSize(R.styleable.ActionContainer_actionContainerItemSize, 0)
            }

            recycle()
        }
    }

    fun addAction(action: Toolbar.Action) {
        val wrapper = ActionWrapper(action)

        if (action.visible()) {
            visibility = View.VISIBLE

            action.createView(this).let {
                wrapper.view = it
                if (action.weight() == -1) {
                    addActionView(it)
                } else {
                    val insertionIndex = calculateInsertionIndex(action)
                    addActionViewAt(it, insertionIndex)
                }
            }
        }

        actions.add(wrapper)
        actions.sortBy { it.actual.weight() }
    }

    private fun addActionViewAt(view: View, index: Int) {
        addView(view, index, LayoutParams(actionSize ?: 0, actionSize ?: 0))
    }

    private fun calculateInsertionIndex(newAction: Toolbar.Action): Int {
        // If it's a default-weight action, add it at the beginning
        if (newAction.weight() == -1) {
            return 0
        }
        // Map existing actions to their views' indices and weights, but only if they are visible.
        val visibleActionsWithIndices = actions.filter { it.actual.visible() }
            .mapNotNull { actionWrapper ->
                val index = indexOfChild(actionWrapper.view)
                if (index != -1) index to actionWrapper.actual.weight() else null
            }.sortedBy { it.second } // Ensure they are sorted by weight for consistent order.

        // Find the first action that has a higher weight than the new action, and use its index.
        val insertionIndex = visibleActionsWithIndices.firstOrNull { it.second > newAction.weight() }?.first

        return insertionIndex ?: childCount
    }

    fun removeAction(action: Toolbar.Action) {
        actions.find { it.actual == action }?.let {
            actions.remove(it)
            removeView(it.view)
        }
    }

    @Suppress("NestedBlockDepth")
    fun invalidateActions() {
        TransitionManager.beginDelayedTransition(this)
        actions.sortBy { it.actual.weight() }
        var updatedVisibility = View.GONE

        for (action in actions) {
            val visible = action.actual.visible()

            if (visible) {
                updatedVisibility = View.VISIBLE
            }

            if (!visible && action.view != null) {
                removeView(action.view)
                action.view = null
            } else if (visible && action.view == null) {
                action.actual.createView(this).let {
                    action.view = it
                    if (action.actual.weight() == -1) {
                        addActionView(it)
                    } else {
                        val insertionIndex = calculateInsertionIndex(action.actual)
                        addActionViewAt(it, insertionIndex)
                    }
                }
            }

            action.view?.let { action.actual.bind(it) }
        }

        visibility = updatedVisibility
        reevaluateAndReorderActions()
    }

    private fun reevaluateAndReorderActions() {
        // Remove all views and re-add them based on current visibility states and weights
        removeAllViews()
        actions.filter { it.actual.visible() }
            .sortedBy { it.actual.weight() }
            .forEach { action ->
                addView(action.view)
            }
    }

    fun autoHideAction(isVisible: Boolean) {
        for (action in actions) {
            if (action.actual.autoHide()) {
                action.view?.isVisible = isVisible
            }
        }
    }

    private fun addActionView(view: View) {
        addView(view, LayoutParams(actionSize ?: 0, actionSize ?: 0))
    }
}
