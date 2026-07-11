package com.arwallcanvas.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorPicker @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var listener: ((Int) -> Unit)? = null

    private val colors = intArrayOf(
        Color.rgb(220,20,60), Color.rgb(255,69,0), Color.rgb(255,165,0), Color.rgb(255,215,0),
        Color.rgb(50,205,50), Color.rgb(0,255,127), Color.rgb(0,191,255), Color.rgb(30,144,255),
        Color.rgb(138,43,226), Color.rgb(255,20,147), Color.WHITE, Color.rgb(169,169,169),
        Color.rgb(54,54,54), Color.BLACK
    )
    private val cols = 7; private val rows = 2
    private var cellSize = 0f; private var sx = 0f; private var sy = 0f
    private var swR = 0f; private var selIdx = 0

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.argb(60,255,255,255) }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.argb(200,255,255,255) }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180,20,20,30)
        setShadowLayer(12f,0f,4f,Color.argb(100,0,0,0))
    }

    fun setOnColorSelectedListener(l: (Int) -> Unit) { listener = l }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        val pad = 24f; val aw = w - pad*2; val ah = h - pad*2
        cellSize = minOf(aw/cols, ah/rows); swR = cellSize * 0.35f
        sx = (w - cellSize*cols)/2f; sy = (h - cellSize*rows)/2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = RectF(sx-16f, sy-16f, sx+cols*cellSize+16f, sy+rows*cellSize+16f)
        canvas.drawRoundRect(r, 24f, 24f, bgPaint)
        for (i in colors.indices) {
            val c = i%cols; val l = i/cols
            val cx = sx + c*cellSize + cellSize/2f; val cy = sy + l*cellSize + cellSize/2f
            fillPaint.color = colors[i]; canvas.drawCircle(cx, cy, swR, fillPaint)
            borderPaint.color = if (colors[i] == Color.BLACK || colors[i] == Color.rgb(54,54,54)) Color.GRAY else Color.argb(60,255,255,255)
            canvas.drawCircle(cx, cy, swR, borderPaint)
            if (i == selIdx) canvas.drawCircle(cx, cy, swR+6f, selectedPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            for (i in colors.indices) {
                val c = i%cols; val l = i/cols
                val cx = sx + c*cellSize + cellSize/2f; val cy = sy + l*cellSize + cellSize/2f
                if (Math.hypot((event.x - cx).toDouble(), (event.y - cy).toDouble()) <= swR + 8f) {
                    selIdx = i; listener?.invoke(colors[i]); invalidate(); return true
                }
            }
        }
        return true
    }
}
