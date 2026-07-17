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
    private var showFeedback = false
    private var feedbackX = 0f
    private var feedbackY = 0f
    private var feedbackRadius = 5f

    private val feedbackPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val feedbackFillPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
    }

    fun setDrawingEngine(engine: DrawingEngine) {
        this.drawingEngine = engine
        if (width > 0 && height > 0) {
            engine.init(width, height)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post {
            drawingEngine?.init(w, h)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
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
                drawingEngine?.moveStroke(x, y)
                lastX = x
                lastY = y
                feedbackX = x
                feedbackY = y
                feedbackRadius = (feedbackRadius + 2f).coerceAtMost(40f)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
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

        val bmp = drawingEngine?.bitmap
        if (bmp != null) {
            canvas.drawBitmap(bmp, 0f, 0f, paint)
        }

        if (showFeedback) {
            feedbackPaint.alpha = ((1f - feedbackRadius / 40f) * 80).toInt().coerceIn(0, 80)
            canvas.drawCircle(feedbackX, feedbackY, feedbackRadius, feedbackPaint)

            feedbackFillPaint.alpha = ((1f - feedbackRadius / 40f) * 30).toInt().coerceIn(0, 30)
            canvas.drawCircle(feedbackX, feedbackY, feedbackRadius * 0.5f, feedbackFillPaint)
        }
    }
}
