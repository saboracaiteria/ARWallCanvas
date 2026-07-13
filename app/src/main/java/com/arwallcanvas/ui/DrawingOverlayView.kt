package com.arwallcanvas.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.arwallcanvas.drawing.DrawingEngine

class DrawingOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var drawingEngine: DrawingEngine? = null
    private var lastX = 0f
    private var lastY = 0f

    fun setDrawingEngine(engine: DrawingEngine) {
        this.drawingEngine = engine
        if (width > 0 && height > 0) engine.init(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawingEngine?.init(w, h)
        postInvalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val engine = drawingEngine ?: return false
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                engine.startStroke(x, y)
                lastX = x; lastY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                engine.addPoint(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                engine.endStroke()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawingEngine?.render(canvas)
    }
}
