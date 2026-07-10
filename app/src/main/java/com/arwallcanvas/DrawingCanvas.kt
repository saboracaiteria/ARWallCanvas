package com.arwallcanvas

import android.graphics.Path
import android.view.MotionEvent
import com.google.ar.core.Session
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Gerencia o desenho virtual na superfície detectada pelo AR.
 * Cada toque é mapeado para um plano 3D e desenhado via OpenGL.
 */
class DrawingCanvas(private val session: Session) {

    data class StrokePoint(val x: Float, val y: Float, val pressure: Float = 1f)

    data class Stroke(
        val points: MutableList<StrokePoint> = mutableListOf(),
        val color: Int = 0xFFFF4444.toInt(),
        val width: Float = 12f,
        var isActive: Boolean = true
    )

    private val strokes = mutableListOf<Stroke>()
    private var currentStroke: Stroke? = null
    private var currentColor: Int = 0xFFFF4444.toInt()
    private var currentWidth: Float = 12f

    private var isDrawing = false

    // Cores para alternar
    private val colorPalette = listOf(
        0xFFFF4444, // Vermelho
        0xFF44FF44, // Verde
        0xFF4444FF, // Azul
        0xFFFFFF44, // Amarelo
        0xFFFF44FF, // Rosa
        0xFF44FFFF, // Ciano
        0xFFFFFFFF, // Branco
        0xFFFF8800  // Laranja
    )
    private var colorIndex = 0

    fun handleTouch(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val pressure = event.pressure.coerceIn(0.1f, 1.0f)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDrawing = true
                currentStroke = Stroke(
                    points = mutableListOf(StrokePoint(x, y, pressure)),
                    color = currentColor,
                    width = currentWidth
                )
                strokes.add(currentStroke!!)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDrawing && currentStroke != null) {
                    currentStroke!!.points.add(StrokePoint(x, y, pressure))
                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP -> {
                isDrawing = false
                currentStroke?.isActive = false
                currentStroke = null
                return true
            }
        }
        return false
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.size - 1)
        }
    }

    fun changeColor() {
        colorIndex = (colorIndex + 1) % colorPalette.size
        currentColor = colorPalette[colorIndex]
    }

    fun setColor(color: Int) {
        currentColor = color
    }

    fun setWidth(width: Float) {
        currentWidth = width.coerceIn(4f, 40f)
    }

    fun getVertexBuffer(): FloatBuffer? {
        if (strokes.isEmpty()) return null

        // Coletar todos os pontos de todos os traços
        val allPoints = mutableListOf<Float>()
        for (stroke in strokes) {
            for (pt in stroke.points) {
                // x, y, r, g, b, a
                val r = ((stroke.color shr 16) and 0xFF) / 255f
                val g = ((stroke.color shr 8) and 0xFF) / 255f
                val b = (stroke.color and 0xFF) / 255f
                val a = 1f
                allPoints.addAll(listOf(pt.x, pt.y, r, g, b, a))
            }
        }

        val buffer = ByteBuffer
            .allocateDirect(allPoints.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(allPoints.toFloatArray())
        buffer.position(0)
        return buffer
    }

    fun clear() {
        strokes.clear()
        currentStroke = null
    }

    fun getStrokes(): List<Stroke> = strokes.toList()

    fun isEmpty(): Boolean = strokes.isEmpty()
}
