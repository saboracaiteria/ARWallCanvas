package com.arwallcanvas.drawing

import android.graphics.*

class DrawingEngine {

    enum class BrushTool {
        BRUSH, SPRAY, MARKER, ERASER
    }

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
            BrushTool.MARKER -> drawMarker(x, y)
            else -> drawBrush(x, y)
        }
        lastX = x
        lastY = y
    }

    fun endStroke() {
        isDrawing = false
    }

    private fun drawBrush(x: Float, y: Float) {
        paint.strokeWidth = brushSize
        canvas.drawLine(lastX, lastY, x, y, paint)
    }

    private fun drawSpray(x: Float, y: Float) {
        val density = brushSize * 2
        for (i in 0 until 15) {
            val dx = (Math.random() * density - density / 2).toFloat()
            val dy = (Math.random() * density - density / 2).toFloat()
            paint.strokeWidth = brushSize * 0.3f
            if (Math.sqrt((dx * dx + dy * dy).toDouble()) < density / 2) {
                canvas.drawPoint(x + dx, y + dy, paint)
            }
        }
    }

    private fun drawMarker(x: Float, y: Float) {
        paint.strokeWidth = brushSize * 0.4f
        paint.alpha = (brushOpacity * 180).toInt()
        canvas.drawLine(lastX, lastY, x, y, paint)
    }

    fun render(c: Canvas) {
        bitmap?.let { c.drawBitmap(it, 0f, 0f, null) }
    }

    fun clear() {
        saveState()
        bitmap?.eraseColor(Color.TRANSPARENT)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(bitmap?.copy(Bitmap.Config.ARGB_8888, true)!!)
            val prev = undoStack.removeAt(undoStack.lastIndex)
            bitmap?.eraseColor(Color.TRANSPARENT)
            canvas.drawBitmap(prev, 0f, 0f, null)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(bitmap?.copy(Bitmap.Config.ARGB_8888, true)!!)
            val next = redoStack.removeAt(redoStack.lastIndex)
            bitmap?.eraseColor(Color.TRANSPARENT)
            canvas.drawBitmap(next, 0f, 0f, null)
        }
    }

    private fun saveState() {
        if (undoStack.size >= maxHistory) undoStack.removeAt(0)
        undoStack.add(bitmap?.copy(Bitmap.Config.ARGB_8888, true)!!)
        redoStack.clear()
    }
}
