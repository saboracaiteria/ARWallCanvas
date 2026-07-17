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
        engine.init(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = drawingEngine?.bitmap
        if (bmp != null) {
            canvas.drawBitmap(bmp, 0f, 0f, null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val engine = drawingEngine ?: return false
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                engine.startStroke(x, y)
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawingEngine?.init(w, h)
        postInvalidate()
    }
}
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            post {
                drawingEngine?.init(w, h)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Iniciar traço
                drawingEngine?.startStroke(x, y)
                lastX = x
                lastY = y
                showFeedback = true
                feedbackX = x
                feedbackY = y
                feedbackRadius = 5f
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                // Mover traço
                drawingEngine?.moveStroke(x, y)
                lastX = x
                lastY = y
                feedbackX = x
                feedbackY = y
                feedbackRadius = (feedbackRadius + 2f).coerceAtMost(40f)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // Finalizar traço
                drawingEngine?.endStroke()
                showFeedback = false
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                drawingEngine?.endStroke()
                showFeedback = false
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Desenhar o bitmap do drawing engine
        val bitmap = drawingEngine?.getBitmap()
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }

        // Feedback visual do toque
        if (showFeedback) {
            // Círculo externo pulsante
            feedbackPaint.alpha = ((1f - feedbackRadius / 40f) * 80).toInt().coerceIn(0, 80)
            canvas.drawCircle(feedbackX, feedbackY, feedbackRadius, feedbackPaint)

            // Preenchimento sutil
            feedbackFillPaint.alpha = ((1f - feedbackRadius / 40f) * 30).toInt().coerceIn(0, 30)
            canvas.drawCircle(feedbackX, feedbackY, feedbackRadius * 0.5f, feedbackFillPaint)
        }
    }
}
