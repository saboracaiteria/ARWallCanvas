package com.arwallcanvas.drawing

import android.graphics.*

enum class BrushTool {
    BRUSH, SPRAY, MARKER, ERASER
}

class DrawingEngine {

    private var bitmap: Bitmap? = null
    private val canvas = Canvas()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentTool = BrushTool.BRUSH
    private var brushSize = 20f
    private var brushOpacity = 1.0f
    private var currentColor = Color.BLACK
    private var lastX = 0f
    private var lastY = 0f
    private var isDrawing = false

    private val undoStack = mutableListOf<Bitmap>()
    private val redoStack = mutableListOf<Bitmap>()
    private val maxHistory = 30

    fun init(width: Int, height: Int) {
        bitmap?.recycle()
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
    }

    fun getBitmap(): Bitmap? = bitmap

    fun hasContent(): Boolean {
        bitmap?.let { bmp ->
            val pixels = IntArray(bmp.width * bmp.height)
            bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            return pixels.any { it != 0 }
        }
        return false
    }

    fun setTool(tool: BrushTool) { currentTool = tool }
    fun setColor(color: Int) { currentColor = color }
    fun setSize(size: Float) { brushSize = size.coerceIn(1f, 100f) }
    fun setOpacity(opacity: Float) { brushOpacity = opacity.coerceIn(0f, 1f) }

    fun startStroke(x: Float, y: Float) {
        saveState()
        isDrawing = true
        lastX = x
        lastY = y
    }

    fun addPoint(x: Float, y: Float) {
        if (!isDrawing) return
        paint.color = if (currentTool == BrushTool.ERASER) Color.TRANSPARENT else currentColor
        paint.alpha = (brushOpacity * 255).toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        when (currentTool) {
            BrushTool.SPRAY -> drawSpray(x, y)
            BrushTool.MARKER -> {
                paint.strokeWidth = brushSize
                canvas.drawLine(lastX, lastY, x, y, paint)
            }
            BrushTool.ERASER -> {
                paint.strokeWidth = brushSize * 2f
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                canvas.drawLine(lastX, lastY, x, y, paint)
                paint.xfermode = null
            }
            else -> { // BRUSH
                paint.strokeWidth = brushSize
                canvas.drawLine(lastX, lastY, x, y, paint)
            }
        }
        lastX = x
        lastY = y
    }

    private fun drawSpray(x: Float, y: Float) {
        val density = 15
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        val originalAlpha = paint.alpha
        for (i in 0 until density) {
            val offsetX = (Math.random() * brushSize * 0.8 - brushSize * 0.4).toFloat()
            val offsetY = (Math.random() * brushSize * 0.8 - brushSize * 0.4).toFloat()
            paint.alpha = (originalAlpha * (0.3 + Math.random() * 0.7)).toInt()
            canvas.drawPoint(lastX + offsetX, lastY + offsetY, paint)
        }
        paint.alpha = originalAlpha
        paint.style = Paint.Style.STROKE
    }

    fun endStroke() { isDrawing = false }

    fun render(c: Canvas) {
        bitmap?.let { c.drawBitmap(it, 0f, 0f, null) }
    }

    private fun saveState() {
        bitmap?.let {
            val copy = it.copy(it.config, true)
            undoStack.add(copy)
            if (undoStack.size > maxHistory) undoStack.removeAt(0)
            redoStack.clear()
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        bitmap?.let { redoStack.add(it.copy(it.config, true)) }
        bitmap = undoStack.removeAt(undoStack.lastIndex)
        canvas.setBitmap(bitmap!!)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        bitmap?.let { undoStack.add(it.copy(it.config, true)) }
        bitmap = redoStack.removeAt(redoStack.lastIndex)
        canvas.setBitmap(bitmap!!)
    }

    fun clear() {
        bitmap?.let {
            saveState()
            it.eraseColor(Color.TRANSPARENT)
        }
    }
}
