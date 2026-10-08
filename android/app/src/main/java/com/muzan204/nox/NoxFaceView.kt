package com.muzan204.nox

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class NoxFaceView(context: Context) : View(context) {
    var state = "IDLE"
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val color = Color.rgb(34, 211, 238)

    init { setBackgroundColor(Color.rgb(5,7,13)) }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f
        val cy = height / 2f
        val s = min(width, height) * .075f

        glow.color = color
        glow.alpha = when (state) { "LISTENING" -> 70; "THINKING" -> 45; "SPEAKING" -> 85; else -> 28 }
        c.drawCircle(cx, cy, s * 5.8f, glow)

        paint.color = color
        paint.style = Paint.Style.FILL
        val eyeY = cy - s * .35f
        val gap = s * 1.9f
        val r = s * .62f
        c.drawCircle(cx - gap, eyeY, r, paint)
        c.drawCircle(cx + gap, eyeY, r, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * .18f
        val mouth = RectF(cx - s * 1.35f, cy + s * .65f, cx + s * 1.35f, cy + s * 1.55f)
        if (state == "SPEAKING") c.drawOval(mouth, paint)
        else c.drawArc(mouth, 15f, 150f, false, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        (context as? MainActivity)?.onFaceTapped()
        return true
    }
}
