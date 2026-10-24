package com.example.yourfriend.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.roundToInt

fun Context.dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()

fun View.dp(value: Float): Int = context.dp(value)

fun View.springScale(
    target: Float,
    stiffness: Float = SpringForce.STIFFNESS_HIGH,
    damping: Float = SpringForce.DAMPING_RATIO_NO_BOUNCY
) {
    SpringAnimation(this, DynamicAnimation.SCALE_X, target).apply {
        spring.stiffness = stiffness
        spring.dampingRatio = damping
    }.start()
    SpringAnimation(this, DynamicAnimation.SCALE_Y, target).apply {
        spring.stiffness = stiffness
        spring.dampingRatio = damping
    }.start()
}

fun View.installPressFeedback(scale: Float = 0.96f) {
    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> view.springScale(scale)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.springScale(1f)
        }
        false
    }
}

fun View.tapHaptic() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

fun View.softHaptic() {
    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
}

fun View.staggerIn(delay: Long, distanceDp: Float = 16f) {
    alpha = 0f
    translationY = dp(distanceDp).toFloat()
    animate()
        .alpha(1f)
        .translationY(0f)
        .setStartDelay(delay)
        .setDuration(560L)
        .setInterpolator(DecelerateInterpolator(1.8f))
        .start()
}

fun View.shake() {
    ObjectAnimator.ofFloat(
        this,
        View.TRANSLATION_X,
        0f,
        -dp(7f).toFloat(),
        dp(7f).toFloat(),
        -dp(4f).toFloat(),
        dp(4f).toFloat(),
        0f
    ).setDuration(330L).start()
}
