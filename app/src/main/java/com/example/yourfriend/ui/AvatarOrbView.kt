package com.example.yourfriend.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import com.example.yourfriend.data.FriendGender
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AvatarOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        color = Color.WHITE
    }
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var initial: String = "?"
    private var gender: FriendGender? = null
    private var primary = 0xFFFF7AB6.toInt()
    private var secondary = 0xFF9B6CFF.toInt()
    private var accent = 0xFF6BE8FF.toInt()

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var compact = false

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun setProfile(name: String, gender: FriendGender?) {
        this.initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        this.gender = gender
        applyPalette()
        buildShaders()
        invalidate()
    }

    fun setInitial(initial: String) {
        this.initial = initial.take(1).uppercase()
        invalidate()
    }

    fun setPalette(gender: FriendGender?) {
        this.gender = gender
        applyPalette()
        buildShaders()
        invalidate()
    }

    private fun applyPalette() {
        when (gender) {
            FriendGender.FEMALE -> {
                primary = 0xFFFF6FAE.toInt()
                secondary = 0xFF9B6CFF.toInt()
                accent = 0xFF6BE8FF.toInt()
            }
            FriendGender.MALE -> {
                primary = 0xFF5CE1E6.toInt()
                secondary = 0xFF5B8CFF.toInt()
                accent = 0xFF9B7CFF.toInt()
            }
            null -> {
                primary = 0xFFFF7AB6.toInt()
                secondary = 0xFF9B6CFF.toInt()
                accent = 0xFF6BE8FF.toInt()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        compact = w < dp(38f)
        radius = min(w, h) / 2f * if (compact) 0.84f else 0.72f
        buildShaders()
    }

    private fun buildShaders() {
        if (radius <= 0f) return

        glowPaint.shader = RadialGradient(
            centerX,
            centerY,
            radius * if (compact) 1.18f else 1.36f,
            intArrayOf(
                withAlpha(primary, 0.34f),
                withAlpha(secondary, 0.10f),
                withAlpha(primary, 0f)
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )

        orbPaint.shader = RadialGradient(
            centerX - radius * 0.32f,
            centerY - radius * 0.38f,
            radius * 1.55f,
            intArrayOf(
                ColorUtils.blendARGB(primary, Color.WHITE, 0.48f),
                primary,
                secondary,
                ColorUtils.blendARGB(secondary, Color.BLACK, 0.35f)
            ),
            floatArrayOf(0f, 0.32f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )

        highlightPaint.color = withAlpha(Color.WHITE, 0.22f)
        textPaint.textSize = radius * if (compact) 0.92f else 0.78f
        ringPaint.strokeWidth = dp(if (compact) 0.9f else 1.25f).toFloat()
        sparklePaint.color = withAlpha(accent, 0.9f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (radius <= 0f) return

        val time = SystemClock.uptimeMillis() / 1000f
        val pulse = if (compact) 0f else sin(time * 1.35f) * radius * 0.035f

        canvas.drawCircle(centerX, centerY, radius * if (compact) 1.36f else 1.8f, glowPaint)

        canvas.save()
        canvas.rotate(time * 11f, centerX, centerY)
        ringPaint.color = withAlpha(accent, if (compact) 0.42f else 0.55f)
        val ringRadius = radius * 1.08f + pulse
        canvas.drawArc(
            RectF(
                centerX - ringRadius,
                centerY - ringRadius,
                centerX + ringRadius,
                centerY + ringRadius
            ),
            205f,
            118f,
            false,
            ringPaint
        )
        canvas.restore()

        canvas.save()
        canvas.rotate(-time * 7f, centerX, centerY)
        ringPaint.color = withAlpha(primary, if (compact) 0.25f else 0.38f)
        val outerRadius = radius * 1.22f + pulse
        canvas.drawArc(
            RectF(
                centerX - outerRadius,
                centerY - outerRadius,
                centerX + outerRadius,
                centerY + outerRadius
            ),
            20f,
            70f,
            false,
            ringPaint
        )
        canvas.restore()

        canvas.drawCircle(centerX, centerY, radius, orbPaint)
        canvas.drawCircle(
            centerX - radius * 0.28f,
            centerY - radius * 0.32f,
            radius * 0.34f,
            highlightPaint
        )

        val fontMetrics = textPaint.fontMetrics
        val textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(initial, centerX, textY, textPaint)

        if (!compact) {
            drawSparkles(canvas, time)
            postInvalidateOnAnimation()
        }
    }

    private fun drawSparkles(canvas: Canvas, time: Float) {
        val orbit = radius * 1.38f
        repeat(4) { index ->
            val angle = time * (0.22f + index * 0.03f) + index * (PI / 2).toFloat()
            val x = centerX + cos(angle) * orbit
            val y = centerY + sin(angle) * orbit
            val twinkle = 0.45f + 0.55f * (0.5f + 0.5f * sin(time * 1.8f + index))
            sparklePaint.color = withAlpha(accent, twinkle)
            val size = dp(if (index % 2 == 0) 1.8f else 1.1f).toFloat()
            canvas.drawCircle(x, y, size, sparklePaint)
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
