package com.arwallcanvas.drawing

import android.content.Context
import android.graphics.*
import java.util.Stack
import kotlin.math.sqrt

class DrawingEngine(private val context: Context) {

    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null

    private val undoStack = Stack<Bitmap>()
    private val redoStack = Stack<Bitmap>()

    private var currentTool = BrushTool.SPRAY
    private var currentColor = Color.RED
    private var currentSize = 20f
    private var currentOpacity = 1f
    private var lastPoint: PointF? = null
    private var lastTime = 0L
    private var currentPath: Path? = null
    private var currentPaint: Paint? = null
    private var isDrawing = false

    var width: Int = 0
        private set
    var height: Int = 0
        private set

    // Alias para getBitmap (compatibilidade)
        get() = drawingBitmap

    fun init(w: Int, h: Int) {
        if (w <= 0 || h <= 0 || w == width && h == height && drawingBitmap != null) return
        width = w
        height = h

        val newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val newCanvas = Canvas(newBitmap)
        newCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (drawingBitmap != null) {
            newCanvas.drawBitmap(drawingBitmap!!, 0f, 0f, null)
        }

        drawingBitmap = newBitmap
        drawingCanvas = newCanvas

        undoStack.clear()
        redoStack.clear()
    }

    fun getBitmap(): Bitmap? = drawingBitmap

    fun setTool(tool: BrushTool) { currentTool = tool }
    fun setColor(color: Int) { currentColor = color }
    fun setSize(size: Float) { currentSize = size.coerceIn(3f, 100f) }
    fun setOpacity(opacity: Float) { currentOpacity = opacity.coerceIn(0f, 1f) }

    // Alias para compatibilidade com DrawingOverlayView
    fun addPoint(x: Float, y: Float) = moveStroke(x, y)

    fun startStroke(x: Float, y: Float) {
        saveStateForUndo()
        isDrawing = true
        lastPoint = PointF(x, y)
        lastTime = System.currentTimeMillis()

        currentPath = Path()
        currentPath!!.moveTo(x, y)

        currentPaint = Paint().apply {
            color = currentColor
            strokeWidth = currentSize
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            isDither = true
            alpha = (currentOpacity * 255).toInt()
        }

        if (currentTool == BrushTool.ERASER) {
            val eraserPaint = Paint(currentPaint!!).apply {
                strokeWidth = currentSize * 2f
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            drawingCanvas?.drawPoint(x, y, eraserPaint)
        } else if (currentTool == BrushTool.SPRAY) {
            drawSprayDot(x, y, currentSize, currentPaint!!)
        } else {
            drawingCanvas?.drawPoint(x, y, currentPaint!!)
        }
    }

    fun moveStroke(x: Float, y: Float) {
        if (!isDrawing || drawingCanvas == null || lastPoint == null) return

        val now = System.currentTimeMillis()
        val dt = (now - lastTime).coerceAtLeast(1)
        val dx = x - lastPoint!!.x
        val dy = y - lastPoint!!.y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance < 1f) return

        val velocity = distance / dt * 1000f

        when (currentTool) {
            BrushTool.SPRAY -> {
                val steps = (distance / 3f).toInt().coerceIn(1, 30)
                val paint = Paint(currentPaint!!).apply {
                    alpha = ((currentOpacity * 0.5f) * 255).toInt()
                }
                for (i in 0 until steps) {
                    val t = i.toFloat() / steps
                    val px = lastPoint!!.x + dx * t
                    val py = lastPoint!!.y + dy * t
                    val sizeMod = currentSize * (0.6f + (velocity / 8000f).coerceIn(0f, 0.4f))
                    drawSprayDot(px, py, sizeMod, paint)
                }
            }
            BrushTool.BRUSH -> {
                val sizeMod = currentSize * (0.5f + (velocity / 5000f).coerceIn(0f, 0.6f))
                val paint = Paint(currentPaint!!).apply {
                    strokeWidth = sizeMod
                    alpha = (currentOpacity * 255).toInt()
                }
                currentPath!!.quadTo(
                    lastPoint!!.x, lastPoint!!.y,
                    (lastPoint!!.x + x) / 2f, (lastPoint!!.y + y) / 2f
                )
                drawingCanvas!!.drawPath(currentPath!!, paint)
            }
            BrushTool.MARKER -> {
                val paint = Paint(currentPaint!!).apply {
                    alpha = ((currentOpacity * 0.85f) * 255).toInt()
                }
                drawingCanvas!!.drawLine(lastPoint!!.x, lastPoint!!.y, x, y, paint)
            }
            BrushTool.ERASER -> {
                val paint = Paint().apply {
                    strokeWidth = currentSize * 2.5f
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    isAntiAlias = true
                }
                drawingCanvas!!.drawLine(lastPoint!!.x, lastPoint!!.y, x, y, paint)
            }
        }

        lastPoint = PointF(x, y)
        lastTime = now
    }

    fun endStroke() {
        isDrawing = false
        currentPath = null
        currentPaint = null
        lastPoint = null
    }

    private fun drawSprayDot(cx: Float, cy: Float, size: Float, paint: Paint) {
        val density = (size / 2f).toInt().coerceIn(1, 20)
        val fillPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            strokeWidth = 0f
        }

        for (i in 0 until density) {
            val angle = Math.random() * 2.0 * Math.PI
            val radius = Math.random() * size * 0.6f
            val dx = (Math.cos(angle) * radius).toFloat()
            val dy = (Math.sin(angle) * radius).toFloat()
            val alpha = ((0.3 + Math.random() * 0.7) * paint.alpha).toInt()
            fillPaint.alpha = alpha.coerceIn(0, 255)
            drawingCanvas?.drawPoint(cx + dx, cy + dy, fillPaint)
        }
    }

    private fun saveStateForUndo() {
        drawingBitmap?.let { bmp ->
            val copy = bmp.copy(bmp.config, true)
            undoStack.push(copy)
            redoStack.clear()
            if (undoStack.size > 30) {
                undoStack.removeAt(0)
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        drawingBitmap?.let { bmp ->
            redoStack.push(bmp.copy(bmp.config, true))
        }
        drawingBitmap = undoStack.pop()
        drawingCanvas = Canvas(drawingBitmap!!)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        drawingBitmap?.let { bmp ->
            undoStack.push(bmp.copy(bmp.config, true))
        }
        drawingBitmap = redoStack.pop()
        drawingCanvas = Canvas(drawingBitmap!!)
    }

    fun clear() {
        if (drawingBitmap == null) return
        saveStateForUndo()
        drawingCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    fun hasContent(): Boolean {
        drawingBitmap?.let { bmp ->
            for (x in 0 until bmp.width step 10) {
                for (y in 0 until bmp.height step 10) {
                    if (bmp.getPixel(x, y) != Color.TRANSPARENT) return true
                }
            }
        }
        return false
    }
}

enum class BrushTool {
    SPRAY,
    BRUSH,
    MARKER,
    ERASER
}
