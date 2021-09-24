package com.udacity.project4.base

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.udacity.project4.R
import kotlin.math.max
import kotlin.math.min

class SwipeController<T>(
    private val swipeRefreshLayout: SwipeRefreshLayout? = null,
    private val callback: ((item: T, direction: Int) -> Unit)? = null
) :
    ItemTouchHelper.Callback() {
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        swipeRefreshLayout?.isEnabled = actionState != ItemTouchHelper.ACTION_STATE_SWIPE
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    @Suppress("UNCHECKED_CAST")
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        (viewHolder.bindingAdapter as? BaseRecyclerViewAdapter<T>)?.let {
            callback?.apply {
                invoke(it.getItem(viewHolder.absoluteAdapterPosition), direction)
            }
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val itemView: View = viewHolder.itemView
            if (dX > itemView.left.toFloat()) {
                c.clipRect(
                    itemView.left.toFloat(),
                    itemView.top.toFloat(),
                    min(dX, itemView.right.toFloat()),
                    itemView.bottom.toFloat()
                )
                c.drawColor(ContextCompat.getColor(recyclerView.context, R.color.colorPrimary))
                AppCompatResources.getDrawable(
                    recyclerView.context,
                    R.drawable.ic_edit_white
                )?.apply {
                    val margin = (itemView.height - intrinsicHeight) / 2
                    bounds = Rect(
                        itemView.left + margin,
                        itemView.top + margin,
                        itemView.left + margin + intrinsicWidth,
                        itemView.top + intrinsicHeight + margin
                    )
                    draw(c)
                }
            } else if (dX < 0) {
                c.clipRect(
                    max(itemView.right.toFloat() + dX, itemView.left.toFloat()),
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                c.drawColor(ContextCompat.getColor(recyclerView.context, R.color.colorAccent))
                AppCompatResources.getDrawable(
                    recyclerView.context,
                    R.drawable.ic_delete_white
                )?.apply {
                    val hmargin = (itemView.height - intrinsicHeight) / 2
                    bounds = Rect(
                        itemView.right - hmargin - intrinsicWidth,
                        itemView.top + hmargin,
                        itemView.right - hmargin,
                        itemView.top + intrinsicHeight + hmargin
                    )
                    draw(c)
                }
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}