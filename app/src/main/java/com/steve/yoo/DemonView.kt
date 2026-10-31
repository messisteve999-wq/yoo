package com.steve.yoo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

// Bulle ronde : démon noir aux yeux rouges. Le contour rouge clignote à l'arrivée d'un message.
class DemonView(ctx: Context) : View(ctx) {
    var alert = false
        set(value) {
            field = value
            invalidate()
        }

    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val eye = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.RED
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = w / 2f
        c.drawCircle(r, r, r - 4f, body)
        if (alert) c.drawCircle(r, r, r - 6f, ring)
        c.drawOval(RectF(w * 0.25f, h * 0.38f, w * 0.45f, h * 0.50f), eye)
        c.drawOval(RectF(w * 0.55f, h * 0.38f, w * 0.75f, h * 0.50f), eye)
    }
}
