package com.arwallcanvas.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Seletor de cores radial com paleta de cores pré-definidas.
 * Inspirado na filosofia Augmented Graffiti — cores vibrantes e expressivas.
 */
class ColorPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var onColorSelectedListener: ((Int) -> Unit)? = null

    // Predefined palette — cores vibrantes estilo graffiti
    private val paletteColors = intArrayOf(
        Color.rgb(220, 20, 60),   // Crimson
        Color.rgb(255, 69, 0),    // OrangeRed
        Color.rgb(255, 165, 0),   // Orange
        Color.rgb(255, 215, 0),   // Gold
        Color.rgb(50, 205, 50),   // LimeGreen
        Color.rgb(0, 255, 127),   // SpringGreen
        Color.rgb(0, 191, 255),   // DeepSkyBlue
        Color.rgb(30, 144, 255),  // DodgerBlue
        Color.rgb(138, 43, 226),  // BlueViolet
        Color.rgb(255, 20, 147),  // DeepPink
        Color.rgb(255, 255, 255), // White
        Color.rgb(169, 169, 169), // DarkGray
        Color.rgb(54, 54, 54),    // Dark
        Color.rgb(0, 0, 0)        // Black
    )

    private val columns = 7
    private val rows = 2
    private var cellSize = 0f
    private var startX = 0f
    private var startY = 0f
    private var swatchRadius = 0f

    private val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(200, 255, 255, 255)
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 20, 20, 30)
        setShadowLayer(12f, 0f, 4f, Color.argb(100, 0, 0, 0))
    }
    private val cornerRadius = 24f

    private var selectedIndex = 0
    private var selectedColor = paletteColors[0]

    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        onColorSelectedListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 24f
        val availableW = w - padding * 2
        val availableH = h - padding * 2

        cellSize = minOf(
            availableW / columns,
            availableH / rows
        )
        swatchRadius = cellSize * 0.35f

        val totalW = columns * cellSize
        val totalH = rows * cellSize
        startX = (w - totalW) / 2f
        startY = (h - totalH) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background rounded rect
        val bgRect = RectF(
            startX - 16f, startY - 16f,
            startX + columns * cellSize + 16f,
            startY + rows * cellSize + 16f
        )
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

        // Draw swatches
        for (i in paletteColors.indices) {
            val col = i % columns
            val row = i / columns
            val cx = startX + col * cellSize + cellSize / 2f
            val cy = startY + row * cellSize + cellSize / 2f

            swatchPaint.color = paletteColors[i]
            canvas.drawCircle(cx, cy, swatchRadius, swatchPaint)

            // Border
            borderPaint.color = if (paletteColors[i] == Color.BLACK || paletteColors[i] == Color.rgb(54, 54, 54))
                Color.GRAY else Color.argb(60, 255, 255, 255)
            canvas.drawCircle(cx, cy, swatchRadius, borderPaint)

            // Selected indicator
            if (i == selectedIndex) {
                canvas.drawCircle(cx, cy, swatchRadius + 6f, selectedPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y

            for (i in paletteColors.indices) {
                val col = i % columns
                val row = i / columns
                val cx = startX + col * cellSize + cellSize / 2f
                val cy = startY + row * cellSize + cellSize / 2f

                val dx = x - cx
                val dy = y - cy
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble())

                if (dist <= swatchRadius + 8f) {
                    selectedIndex = i
                    selectedColor = paletteColors[i]
                    onColorSelectedListener?.invoke(selectedColor)
                    invalidate()
                    return true
                }
            }
        }
        return true
    }

    fun getSelectedColor(): Int = selectedColor
}
