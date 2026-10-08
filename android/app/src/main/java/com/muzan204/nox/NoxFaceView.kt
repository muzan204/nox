package com.muzan204.nox

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class NoxFaceView(context: Context) : View(context) {
    var state = "IDLE"
        set(value) { field = value; invalidate() }

    var onLongPress: (() -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private var downAt = 0L
    private val cyan = Color.rgb(34, 211, 238)

    init { setBackgroundColor(Color.rgb(5, 7, 13)) }

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
        val color = stateColor()

        glow.color = color
        glow.alpha = when (state) {
            "LISTENING" -> 90
            "THINKING" -> 55
            "SPEAKING" -> 100
            "HAPPY" -> 75
            "SURPRISED" -> 110
            else -> 32
        }
        c.drawCircle(cx, cy, s * 5.8f, glow)

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

        c.drawRoundRect(cx - gap - eyeW, eyeY - eyeH, cx - gap + eyeW, eyeY + eyeH, s, s, paint)
        c.drawRoundRect(cx + gap - eyeW, eyeY - eyeH, cx + gap + eyeW, eyeY + eyeH, s, s, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * .18f

        val mouth = RectF(cx - s * 1.45f, cy + s * .55f, cx + s * 1.45f, cy + s * 1.65f)
        when (state) {
            "SPEAKING" -> c.drawOval(mouth, paint)
            "SURPRISED" -> c.drawOval(
                RectF(cx - s * .65f, cy + s * .72f, cx + s * .65f, cy + s * 1.72f),
                paint
            )
            "ERROR" -> c.drawArc(mouth, 200f, 140f, false, paint)
            "THINKING" -> c.drawLine(cx - s * .7f, cy + s * 1.05f, cx + s * .7f, cy + s * 1.05f, paint)
            else -> c.drawArc(mouth, 20f, 140f, false, paint)
        }

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = s * .45f
        paint.color = Color.argb(150, 220, 240, 255)
        c.drawText("NOX", cx, cy + s * 3.1f, paint)
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
}