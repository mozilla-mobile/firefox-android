package org.mozilla.focus.autocomplete

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

object SimpleCallback : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition

            (recyclerView.adapter as AutocompleteListFragment.DomainListAdapter).move(from, to)

            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            if (viewHolder is AutocompleteListFragment.AddActionViewHolder) {
                return ItemTouchHelper.Callback.makeMovementFlags(0, 0)
            }

            return super.getMovementFlags(recyclerView, viewHolder)
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)

            if (viewHolder is AutocompleteListFragment.DomainViewHolder) {
                viewHolder.onSelected()
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)

            if (viewHolder is AutocompleteListFragment.DomainViewHolder) {
                viewHolder.onCleared()
            }
        }

        override fun canDropOver(
            recyclerView: RecyclerView,
            current: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            if (target is AutocompleteListFragment.AddActionViewHolder) {
                return false
            }

            return super.canDropOver(recyclerView, current, target)
        }
    }
