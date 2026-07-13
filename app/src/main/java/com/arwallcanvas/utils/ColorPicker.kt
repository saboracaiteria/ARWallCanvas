package com.arwallcanvas.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorPicker(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val colors = intArrayOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.MAGENTA, Color.WHITE, Color.BLACK,
        Color.GRAY, Color.DKGRAY, Color.rgb(255, 165, 0),  // Orange
        Color.rgb(128, 0, 128),  // Purple
        Color.rgb(0, 128, 128),  // Teal
        Color.rgb(255, 192, 203) // Pink
    )
    private var selectedIndex = 0
    private var listener: ((Int) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setOnColorSelectedListener(l: (Int) -> Unit) {
        listener = l
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val cols = 4
        val rows = (colors.size + cols - 1) / cols
        val cellW = width / cols
        val cellH = height / rows
        val radius = minOf(cellW, cellH) * 0.35f

        for (i in colors.indices) {
            val col = i % cols
            val row = i / cols
            val cx = col * cellW + cellW / 2
            val cy = row * cellH + cellH / 2

            paint.color = colors[i]
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, radius, paint)

            if (i == selectedIndex) {
                paint.color = Color.WHITE
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawCircle(cx, cy, radius + 4f, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            val cols = 4
            val cellW = width.toFloat() / cols
            val cellH = height.toFloat() / ((colors.size + cols - 1) / cols)
            val col = (x / cellW).toInt()
            val row = (y / cellH).toInt()
            val idx = row * cols + col
            if (idx in colors.indices) {
                selectedIndex = idx
                listener?.invoke(colors[idx])
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
