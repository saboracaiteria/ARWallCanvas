package com.arwallcanvas.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.arwallcanvas.drawing.DrawingEngine

class DrawingOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val drawingEngine: DrawingEngine = DrawingEngine()
    private var lastX = 0f
    private var lastY = 0f

    fun setDrawingEngine(engine: DrawingEngine) {
        // Engine já foi criado internamente.
        // Para usar engine externo, descomente:
        // this.drawingEngine = engine
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawingEngine.render(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = x
                lastY = y
                drawingEngine.startStroke(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                drawingEngine.addPoint(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                drawingEngine.endStroke()
                invalidate()
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawingEngine.init(w, h)
    }
}
