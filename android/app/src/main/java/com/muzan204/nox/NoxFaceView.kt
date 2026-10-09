package com.muzan204.nox

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min
import kotlin.random.Random

class NoxFaceView(context: Context) : View(context) {

    var state = "IDLE"
        set(value) {
            if (field == value) return
            field = value
            animateColorTo(stateColor())
            invalidate()
        }

    var onLongPress: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private var downAt = 0L
    private val cyan = Color.rgb(34, 211, 238)
    private var animatedColor = cyan

    // animações contínuas que dão vida à carinha
    private var breathe = 0f
    private var blinkAmt = 0f
    private var ringProgress = 0f
    private var talk = 0f

    private var breatheAnim: ValueAnimator? = null
    private var ringAnim: ValueAnimator? = null
    private var talkAnim: ValueAnimator? = null
    private var colorAnim: ValueAnimator? = null
    private var blinkRunnable: Runnable? = null

    private val labels = mapOf(
        "LISTENING" to "Ouvindo…", "THINKING" to "Pensando…", "SPEAKING" to "Falando…",
        "ERROR" to "Erro", "SLEEPING" to "Dormindo", "CONFUSED" to "Confuso",
        "SURPRISED" to "Surpreso", "HAPPY" to "Feliz"
    )

    init {
        setBackgroundColor(Color.rgb(5, 7, 13))

        breatheAnim = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 3200; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { breathe = it.animatedValue as Float; if (state == "IDLE") invalidate() }
            start()
        }
        ringAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100; repeatCount = ValueAnimator.INFINITE
            addUpdateListener { ringProgress = it.animatedValue as Float; if (state == "LISTENING") invalidate() }
            start()
        }
        talkAnim = ValueAnimator.ofFloat(.2f, 1f, .2f).apply {
            duration = 340; repeatCount = ValueAnimator.INFINITE
            addUpdateListener { talk = it.animatedValue as Float; if (state == "SPEAKING") invalidate() }
            start()
        }
        scheduleBlink()
    }

    private fun scheduleBlink() {
        val r = Runnable {
            ValueAnimator.ofFloat(0f, 1f, 0f).apply {
                duration = 140
                addUpdateListener { blinkAmt = it.animatedValue as Float; invalidate() }
                start()
            }
            scheduleBlink()
        }
        blinkRunnable = r
        postDelayed(r, 2200L + Random.nextLong(4200))
    }

    private fun animateColorTo(target: Int) {
        colorAnim?.cancel()
        colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), animatedColor, target).apply {
            duration = 420
            addUpdateListener { animatedColor = it.animatedValue as Int; invalidate() }
            start()
        }
    }

    private fun stateColor(): Int = when (state) {
        "LISTENING" -> Color.rgb(96, 165, 250)
        "THINKING" -> Color.rgb(251, 191, 36)
        "SPEAKING" -> Color.rgb(52, 211, 153)
        "HAPPY" -> Color.rgb(249, 168, 212)
        "SURPRISED" -> Color.rgb(250, 204, 21)
        "CONFUSED" -> Color.rgb(192, 132, 252)
        "ERROR" -> Color.rgb(251, 113, 133)
        "SLEEPING" -> Color.rgb(100, 116, 139)
        else -> cyan
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f
        val cy = height / 2f
        val s = min(width, height) * .075f
        val color = animatedColor

        val baseAlpha = when (state) {
            "LISTENING" -> 90
            "THINKING" -> 55
            "SPEAKING" -> 100
            "HAPPY" -> 75
            "SURPRISED" -> 110
            else -> 32
        }
        val breatheBoost = if (state == "IDLE") (breathe * 16).toInt() else 0
        glow.color = color
        glow.alpha = (baseAlpha + breatheBoost).coerceIn(0, 255)
        c.drawCircle(cx, cy, s * 5.8f, glow)

        if (state == "LISTENING") {
            ringPaint.color = color
            ringPaint.strokeWidth = s * .1f
            ringPaint.alpha = (220 * (1f - ringProgress)).toInt().coerceIn(0, 255)
            c.drawCircle(cx, cy, s * (4.6f + ringProgress * 2.2f), ringPaint)
        }

        paint.color = color
        paint.style = Paint.Style.FILL

        val eyeY = cy - s * .55f
        val gap = s * 2.15f
        val eyeW = s * 1.25f
        val eyeH = when (state) {
            "HAPPY" -> s * .48f
            "SLEEPING" -> s * .16f
            "LISTENING" -> s * 1.45f
            "CONFUSED" -> s * .95f
            "ERROR" -> s * .9f
            else -> s * 1.12f
        }
        val openness = 1f - blinkAmt * .9f

        c.save()
        c.scale(1f, openness, cx - gap, eyeY)
        c.drawRoundRect(cx - gap - eyeW, eyeY - eyeH, cx - gap + eyeW, eyeY + eyeH, s, s, paint)
        c.restore()

        c.save()
        c.scale(1f, openness, cx + gap, eyeY)
        c.drawRoundRect(cx + gap - eyeW, eyeY - eyeH, cx + gap + eyeW, eyeY + eyeH, s, s, paint)
        c.restore()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * .18f

        val mouth = RectF(cx - s * 1.45f, cy + s * .55f, cx + s * 1.45f, cy + s * 1.65f)
        when (state) {
            "SPEAKING" -> {
                paint.style = Paint.Style.FILL
                val h = mouth.height() * talk
                val dy = (mouth.height() - h) / 2f
                c.drawOval(RectF(mouth.left, mouth.top + dy, mouth.right, mouth.bottom - dy), paint)
            }
            "SURPRISED" -> c.drawOval(
                RectF(cx - s * .65f, cy + s * .72f, cx + s * .65f, cy + s * 1.72f),
                paint
            )
            "ERROR" -> c.drawArc(mouth, 200f, 140f, false, paint)
            "THINKING" -> c.drawLine(cx - s * .7f, cy + s * 1.05f, cx + s * .7f, cy + s * 1.05f, paint)
            else -> c.drawArc(mouth, 20f, 140f, false, paint)
        }

        paint.style = Paint.Style.FILL
        labelPaint.textSize = s * .45f
        labelPaint.color = Color.argb(150, 220, 240, 255)
        c.drawText("NOX", cx, cy + s * 3.1f, labelPaint)

        labels[state]?.let {
            labelPaint.textSize = s * .3f
            labelPaint.color = Color.argb(
                160,
                (Color.red(color) + 220) / 2,
                (Color.green(color) + 240) / 2,
                (Color.blue(color) + 255) / 2
            )
            c.drawText(it, cx, cy + s * 3.65f, labelPaint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downAt = System.currentTimeMillis()
                postDelayed({
                    if (downAt != 0L) onLongPress?.invoke()
                }, 900)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - downAt
                downAt = 0L
                if (duration < 900) performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                downAt = 0L
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        (context as? MainActivity)?.onFaceTapped()
        return true
    }

    override fun onDetachedFromWindow() {
        breatheAnim?.cancel()
        ringAnim?.cancel()
        talkAnim?.cancel()
        colorAnim?.cancel()
        blinkRunnable?.let { removeCallbacks(it) }
        super.onDetachedFromWindow()
    }
}
