package com.example.yourfriend.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.example.yourfriend.data.FriendGender
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class MagicBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Palette(
        val primary: Int,
        val secondary: Int,
        val accent: Int
    )

    private data class Star(
        val x: Float,
        val y: Float,
        val radius: Float,
        val phase: Float,
        val speed: Float,
        val baseAlpha: Int
    )

    private data class Mote(
        val x: Float,
        val y: Float,
        val radius: Float,
        val speed: Float,
        val phase: Float
    )

    private data class Blob(
        val shader: RadialGradient,
        val radius: Float
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(7)

    private var palette = neutralPalette()
    private var baseShader: LinearGradient? = null
    private var vignetteShader: RadialGradient? = null
    private val blobs = mutableListOf<Blob>()
    private val stars = mutableListOf<Star>()
    private val motes = mutableListOf<Mote>()

    private var viewWidth = 0
    private var viewHeight = 0

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setGender(gender: FriendGender?) {
        val next = when (gender) {
            FriendGender.FEMALE -> Palette(
                primary = 0xFFFF6FAE.toInt(),
                secondary = 0xFF9B6CFF.toInt(),
                accent = 0xFF6BE8FF.toInt()
            )
            FriendGender.MALE -> Palette(
                primary = 0xFF5CE1E6.toInt(),
                secondary = 0xFF5B8CFF.toInt(),
                accent = 0xFF9B7CFF.toInt()
            )
            null -> neutralPalette()
        }
        if (next != palette) {
            palette = next
            rebuildBlobs()
            invalidate()
        }
    }

    private fun neutralPalette() = Palette(
        primary = 0xFFFF7AB6.toInt(),
        secondary = 0xFF9B6CFF.toInt(),
        accent = 0xFF6BE8FF.toInt()
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        if (w <= 0 || h <= 0) return

        baseShader = LinearGradient(
            0f,
            0f,
            w * 0.35f,
            h.toFloat(),
            intArrayOf(
                0xFF05030F.toInt(),
                0xFF0B0822.toInt(),
                0xFF151033.toInt(),
                0xFF090617.toInt()
            ),
            floatArrayOf(0f, 0.45f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )

        vignetteShader = RadialGradient(
            w * 0.5f,
            h * 0.42f,
            max(w, h) * 0.82f,
            intArrayOf(Color.TRANSPARENT, 0x33000000.toInt(), 0x99000000.toInt()),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )

        rebuildBlobs()
        rebuildStars()
    }

    private fun rebuildBlobs() {
        if (viewWidth <= 0 || viewHeight <= 0) return
        blobs.clear()
        val radius = max(viewWidth, viewHeight).toFloat()
        blobs += Blob(
            shader = RadialGradient(
                0f,
                0f,
                radius * 0.68f,
                intArrayOf(
                    withAlpha(palette.primary, 0.34f),
                    withAlpha(palette.secondary, 0.15f),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.46f, 1f),
                Shader.TileMode.CLAMP
            ),
            radius = radius * 0.68f
        )
        blobs += Blob(
            shader = RadialGradient(
                0f,
                0f,
                radius * 0.58f,
                intArrayOf(
                    withAlpha(palette.secondary, 0.27f),
                    withAlpha(palette.accent, 0.10f),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.42f, 1f),
                Shader.TileMode.CLAMP
            ),
            radius = radius * 0.58f
        )
        blobs += Blob(
            shader = RadialGradient(
                0f,
                0f,
                radius * 0.66f,
                intArrayOf(
                    withAlpha(palette.accent, 0.16f),
                    withAlpha(palette.primary, 0.08f),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            ),
            radius = radius * 0.66f
        )
    }

    private fun rebuildStars() {
        stars.clear()
        repeat(78) {
            stars += Star(
                x = random.nextFloat() * viewWidth,
                y = random.nextFloat() * viewHeight,
                radius = 0.5f + random.nextFloat() * 1.35f,
                phase = random.nextFloat() * (2f * PI.toFloat()),
                speed = 0.35f + random.nextFloat() * 0.85f,
                baseAlpha = 45 + random.nextInt(100)
            )
        }
        motes.clear()
        repeat(26) {
            motes += Mote(
                x = random.nextFloat() * viewWidth,
                y = random.nextFloat() * viewHeight,
                radius = 0.7f + random.nextFloat() * 1.5f,
                speed = 5f + random.nextFloat() * 13f,
                phase = random.nextFloat() * (2f * PI.toFloat())
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (viewWidth == 0 || viewHeight == 0) return

        val time = SystemClock.uptimeMillis() / 1000f

        basePaint.shader = baseShader
        canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), basePaint)

        drawBlob(
            canvas,
            blob = blobs.getOrNull(0),
            x = viewWidth * (0.20f + 0.08f * sin(time * 0.11f)),
            y = viewHeight * (0.25f + 0.055f * cos(time * 0.13f)),
            scale = 1f
        )
        drawBlob(
            canvas,
            blob = blobs.getOrNull(1),
            x = viewWidth * (0.84f + 0.06f * cos(time * 0.09f)),
            y = viewHeight * (0.58f + 0.06f * sin(time * 0.12f)),
            scale = 1.05f
        )
        drawBlob(
            canvas,
            blob = blobs.getOrNull(2),
            x = viewWidth * (0.48f + 0.12f * sin(time * 0.07f)),
            y = viewHeight * (1.00f + 0.035f * cos(time * 0.10f)),
            scale = 0.95f
        )

        drawStars(canvas, time)
        drawMotes(canvas, time)

        vignettePaint.shader = vignetteShader
        canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), vignettePaint)

        postInvalidateOnAnimation()
    }

    private fun drawBlob(canvas: Canvas, blob: Blob?, x: Float, y: Float, scale: Float) {
        if (blob == null) return
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)
        paint.shader = blob.shader
        canvas.drawCircle(0f, 0f, blob.radius, paint)
        canvas.restore()
    }

    private fun drawStars(canvas: Canvas, time: Float) {
        starPaint.shader = null
        stars.forEach { star ->
            val twinkle = 0.55f + 0.45f * sin(time * star.speed + star.phase)
            val alpha = (star.baseAlpha * twinkle).toInt().coerceIn(8, 220)
            starPaint.color = Color.argb(alpha, 255, 248, 255)
            canvas.drawCircle(star.x, star.y, star.radius, starPaint)
        }
    }

    private fun drawMotes(canvas: Canvas, time: Float) {
        starPaint.shader = null
        motes.forEach { mote ->
            val y = (mote.y - time * mote.speed).mod(viewHeight.toFloat())
            val drift = sin(time * 0.35f + mote.phase) * 8f
            val alpha = (45 + 35 * (0.5f + 0.5f * sin(time * 0.7f + mote.phase))).toInt()
            starPaint.color = withAlpha(palette.accent, alpha / 255f)
            canvas.drawCircle(mote.x + drift, y, mote.radius, starPaint)
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
