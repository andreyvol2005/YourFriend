package com.example.yourfriend.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class TypingDotsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var accent = 0xFFFF9ECB.toInt()

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setAccent(color: Int) {
        accent = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val time = SystemClock.uptimeMillis() / 1000f
        val radius = dp(2.5f)
        val gap = width / 4f
        repeat(3) { index ->
            val wave = sin(time * 5.2f - index * 0.8f)
            val offsetY = wave * dp(2.2f)
            val alpha = 0.40f + 0.60f * ((wave + 1f) / 2f)
            paint.color = withAlpha(accent, alpha)
            canvas.drawCircle(
                gap * (index + 1),
                height / 2f + offsetY,
                radius * (0.85f + 0.15f * alpha),
                paint
            )
        }
        postInvalidateOnAnimation()
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        return Color.argb(
            (alpha.coerceIn(0f, 1f) * 255).toInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}
