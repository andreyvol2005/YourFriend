package com.example.yourfriend.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SparkleBurstView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Particle(
        val angle: Float,
        val distance: Float,
        val size: Float,
        val speed: Float,
        val color: Int
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(System.nanoTime())
    private val particles = mutableListOf<Particle>()
    private var progress = 1f
    private var originX = 0f
    private var originY = 0f
    private var burstColor = 0xFFFF7AB6.toInt()
    private var animator: ValueAnimator? = null

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun burst(x: Float, y: Float, color: Int) {
        originX = x
        originY = y
        burstColor = color
        particles.clear()
        repeat(26) { index ->
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val distance = dp(26f) + random.nextFloat() * dp(58f)
            particles += Particle(
                angle = angle,
                distance = distance,
                size = dp(1.4f) + random.nextFloat() * dp(2.8f),
                speed = 0.65f + random.nextFloat() * 0.7f,
                color = if (index % 4 == 0) Color.WHITE else color
            )
        }
        animator?.cancel()
        progress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 780L
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress >= 1f || particles.isEmpty()) return

        val p = progress.coerceIn(0f, 1f)
        val fade = (1f - p) * (1f - p)
        val ringRadius = dp(8f) + dp(52f) * p
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1.2f).toFloat()
        paint.color = withAlpha(burstColor, 0.35f * (1f - p))
        canvas.drawCircle(originX, originY, ringRadius, paint)

        paint.style = Paint.Style.FILL
        particles.forEach { particle ->
            val distance = particle.distance * p * particle.speed
            val x = originX + cos(particle.angle) * distance
            val y = originY + sin(particle.angle) * distance + dp(16f) * p * p
            val alpha = (fade * 255).toInt().coerceIn(0, 255)
            paint.color = withAlpha(particle.color, alpha / 255f)
            canvas.drawCircle(x, y, particle.size * (1f - p * 0.55f), paint)

            if (particle.size > dp(2.4f)) {
                paint.strokeWidth = dp(0.7f).toFloat()
                paint.style = Paint.Style.STROKE
                val arm = particle.size * (1f - p * 0.4f) * 2.2f
                canvas.drawLine(x - arm, y, x + arm, y, paint)
                canvas.drawLine(x, y - arm, x, y + arm, paint)
                paint.style = Paint.Style.FILL
            }
        }
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
