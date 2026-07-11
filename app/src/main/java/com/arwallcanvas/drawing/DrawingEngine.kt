package com.arwallcanvas.drawing

import android.content.Context
import android.graphics.*
import java.util.Stack
import kotlin.math.sqrt

enum class BrushTool {
    SPRAY,
    BRUSH,
    MARKER,
    ERASER
}

class DrawingEngine(private val context: Context) {

    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null

    // Histórico undo/redo
    private val undoStack = Stack<Bitmap>()
    private val redoStack = Stack<Bitmap>()

    // Estado da ferramenta
    private var currentTool = BrushTool.SPRAY
    private var currentColor = Color.RED
    private var currentSize = 20f
    private var currentOpacity = 1f

    // Rastreamento do traço
    private var lastPoint: PointF? = null
    private var lastTime = 0L
    private var currentPath: Path? = null
    private var currentPaint: Paint? = null
    private var isDrawing = false

    var width: Int = 0; private set
    var height: Int = 0; private set

    fun init(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        width = w; height = h
        val newBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val newCvs = Canvas(newBmp)
        newCvs.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        if (drawingBitmap != null) newCvs.drawBitmap(drawingBitmap!!, 0f, 0f, null)
        drawingBitmap = newBmp
        drawingCanvas = newCvs
        undoStack.clear(); redoStack.clear()
    }

    fun getBitmap(): Bitmap? = drawingBitmap

    fun setTool(t: BrushTool) { currentTool = t }
    fun setColor(c: Int) { currentColor = c }
    fun setSize(s: Float) { currentSize = s.coerceIn(3f, 100f) }
    fun setOpacity(o: Float) { currentOpacity = o.coerceIn(0f, 1f) }

    fun startStroke(x: Float, y: Float) {
        saveState()
        isDrawing = true
        lastPoint = PointF(x, y)
        lastTime = System.currentTimeMillis()
        currentPath = Path().apply { moveTo(x, y) }
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
            Paint(currentPaint!!).apply {
                strokeWidth = currentSize * 2.5f
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }.let { drawingCanvas?.drawPoint(x, y, it) }
        } else if (currentTool == BrushTool.SPRAY) {
            sprayDot(x, y, currentSize, currentPaint!!)
        } else {
            drawingCanvas?.drawPoint(x, y, currentPaint!!)
        }
    }

    fun moveStroke(x: Float, y: Float) {
        if (!isDrawing || drawingCanvas == null || lastPoint == null) return
        val now = System.currentTimeMillis()
        val dt = (now - lastTime).coerceAtLeast(1)
        val dx = x - lastPoint!!.x; val dy = y - lastPoint!!.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 1f) return
        val vel = dist / dt * 1000f

        when (currentTool) {
            BrushTool.SPRAY -> {
                val steps = (dist / 3f).toInt().coerceIn(1, 30)
                val p = Paint(currentPaint!!).apply { alpha = ((currentOpacity * 0.5f) * 255).toInt() }
                for (i in 0 until steps) {
                    val t = i.toFloat() / steps
                    val sz = currentSize * (0.6f + (vel / 8000f).coerceIn(0f, 0.4f))
                    sprayDot(lastPoint!!.x + dx * t, lastPoint!!.y + dy * t, sz, p)
                }
            }
            BrushTool.BRUSH -> {
                val sz = currentSize * (0.5f + (vel / 5000f).coerceIn(0f, 0.6f))
                val p = Paint(currentPaint!!).apply {
                    strokeWidth = sz; alpha = (currentOpacity * 255).toInt()
                }
                currentPath!!.quadTo(lastPoint!!.x, lastPoint!!.y, (lastPoint!!.x + x) / 2f, (lastPoint!!.y + y) / 2f)
                drawingCanvas!!.drawPath(currentPath!!, p)
            }
            BrushTool.MARKER -> {
                val p = Paint(currentPaint!!).apply { alpha = ((currentOpacity * 0.85f) * 255).toInt() }
                drawingCanvas!!.drawLine(lastPoint!!.x, lastPoint!!.y, x, y, p)
            }
            BrushTool.ERASER -> {
                val p = Paint().apply {
                    strokeWidth = currentSize * 2.5f; style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); isAntiAlias = true
                }
                drawingCanvas!!.drawLine(lastPoint!!.x, lastPoint!!.y, x, y, p)
            }
        }
        lastPoint = PointF(x, y); lastTime = now
    }

    fun endStroke() {
        isDrawing = false; currentPath = null; currentPaint = null; lastPoint = null
    }

    private fun sprayDot(cx: Float, cy: Float, size: Float, paint: Paint) {
        val density = (size / 2f).toInt().coerceIn(1, 20)
        val fp = Paint(paint).apply { style = Paint.Style.FILL; strokeWidth = 0f }
        for (i in 0 until density) {
            val ang = Math.random() * 2.0 * Math.PI
            val r = Math.random() * size * 0.6f
            val a = ((0.3 + Math.random() * 0.7) * fp.alpha).toInt()
            fp.alpha = a.coerceIn(0, 255)
            drawingCanvas?.drawPoint(cx + (Math.cos(ang) * r).toFloat(), cy + (Math.sin(ang) * r).toFloat(), fp)
        }
    }

    private fun saveState() {
        drawingBitmap?.let { bmp ->
            undoStack.push(bmp.copy(bmp.config, true))
            redoStack.clear()
            if (undoStack.size > 30) undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        drawingBitmap?.let { redoStack.push(it.copy(it.config, true)) }
        drawingBitmap = undoStack.pop()
        drawingCanvas = Canvas(drawingBitmap!!)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        drawingBitmap?.let { undoStack.push(it.copy(it.config, true)) }
        drawingBitmap = redoStack.pop()
        drawingCanvas = Canvas(drawingBitmap!!)
    }

    fun clear() {
        if (drawingBitmap == null) return
        saveState()
        drawingCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    fun hasContent(): Boolean {
        drawingBitmap?.let { bmp ->
            for (x in 0 until bmp.width step 10)
                for (y in 0 until bmp.height step 10)
                    if (bmp.getPixel(x, y) != Color.TRANSPARENT) return true
        }
        return false
    }
}
