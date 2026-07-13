package com.arwallcanvas.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.arwallcanvas.drawing.DrawingEngine

class DrawingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawingEngine: DrawingEngine? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        filterBitmap = true
    }

    fun setDrawingEngine(engine: DrawingEngine) {
        drawingEngine = engine
        if (width > 0 && height > 0) {
            engine.init(width, height)
        }
    }

    fun getDrawingBitmap(): Bitmap? = drawingEngine?.getBitmap()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            post {
                drawingEngine?.init(w, h)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val engine = drawingEngine ?: return false
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                engine.startStroke(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                engine.addPoint(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                engine.endStroke()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                engine.endStroke()
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = drawingEngine?.getBitmap()
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }
    }
}
