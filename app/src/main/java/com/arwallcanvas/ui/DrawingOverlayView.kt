package com.arwallcanvas.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.arwallcanvas.drawing.DrawingEngine

class DrawingOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawingEngine: DrawingEngine? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true; filterBitmap = true }

    private val fbStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.argb(80, 255, 255, 255); strokeWidth = 2f
    }
    private val fbFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.argb(20, 255, 255, 255)
    }
    private var showFb = false; private var fbx = 0f; private var fby = 0f; private var fbR = 0f

    fun setDrawingEngine(e: DrawingEngine) {
        drawingEngine = e
        if (width > 0 && height > 0) e.init(width, height)
    }
    fun getDrawingBitmap(): Bitmap? = drawingEngine?.getBitmap()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) post { drawingEngine?.init(w, h) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                drawingEngine?.startStroke(x, y); showFb = true; fbx = x; fby = y; fbR = 5f; invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                drawingEngine?.moveStroke(x, y)
                fbx = x; fby = y; fbR = (fbR + 2f).coerceAtMost(40f); invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                drawingEngine?.endStroke(); showFb = false; invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawingEngine?.getBitmap()?.let { canvas.drawBitmap(it, 0f, 0f, paint) }
        if (showFb) {
            fbStroke.alpha = ((1f - fbR / 40f) * 80).toInt().coerceIn(0, 80)
            canvas.drawCircle(fbx, fby, fbR, fbStroke)
            fbFill.alpha = ((1f - fbR / 40f) * 30).toInt().coerceIn(0, 30)
            canvas.drawCircle(fbx, fby, fbR * 0.5f, fbFill)
        }
    }
}
