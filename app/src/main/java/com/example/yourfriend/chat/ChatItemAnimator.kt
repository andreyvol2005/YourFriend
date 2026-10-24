package com.example.yourfriend.chat

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.yourfriend.ui.dp

class ChatItemAnimator : SimpleItemAnimator() {

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        val view = holder.itemView
        ViewCompat.animate(view)
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .translationY(-view.dp(8f).toFloat())
            .setDuration(180L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { dispatchRemoveFinished(holder) }
            .start()
        return true
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        val view = holder.itemView
        view.alpha = 0f
        view.translationY = view.dp(18f).toFloat()
        view.scaleX = 0.94f
        view.scaleY = 0.94f
        ViewCompat.animate(view)
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(360L)
            .setInterpolator(DecelerateInterpolator(1.8f))
            .withEndAction { dispatchAddFinished(holder) }
            .start()
        return true
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        val view = holder.itemView
        val deltaX = fromX - view.left
        val deltaY = fromY - view.top
        view.translationX = deltaX.toFloat()
        view.translationY = deltaY.toFloat()
        ViewCompat.animate(view)
            .translationX(0f)
            .translationY(0f)
            .setDuration(260L)
            .setInterpolator(DecelerateInterpolator(1.6f))
            .withEndAction { dispatchMoveFinished(holder) }
            .start()
        return true
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        oldHolder.itemView.alpha = 1f
        newHolder.itemView.alpha = 0f
        ViewCompat.animate(oldHolder.itemView)
            .alpha(0f)
            .setDuration(150L)
            .withEndAction { dispatchChangeFinished(oldHolder, true) }
            .start()
        ViewCompat.animate(newHolder.itemView)
            .alpha(1f)
            .setDuration(220L)
            .withEndAction { dispatchChangeFinished(newHolder, false) }
            .start()
        return true
    }

    override fun endAnimation(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        ViewCompat.animate(view).cancel()
        view.alpha = 1f
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    override fun runPendingAnimations() = Unit

    override fun endAnimations() = Unit

    override fun isRunning(): Boolean = false
}
