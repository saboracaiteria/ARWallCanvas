package com.arwallcanvas.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.arwallcanvas.drawing.DrawingEngine

/**
 * View transparente sobre a câmera que captura toques e renderiza o desenho.
 * Feedbacks visuais sutis mostram onde o usuário está tocando.
 */
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

    // Feedback visual de toque
    private val feedbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(80, 255, 255, 255)
        strokeWidth = 2f
    }
    private val feedbackFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(20, 255, 255, 255)
    }

    private var showFeedback = false
    private var feedbackX = 0f
    private var feedbackY = 0f
    private var feedbackRadius = 0f

    private var lastX = 0f
    private var lastY = 0f

    fun setDrawingEngine(engine: DrawingEngine) {
        drawingEngine = engine
        // Se já tiver tamanho, inicializar o engine
        if (width > 0 && height > 0) {
            engine.init(width, height)
        }
    }

    /**
     * Retorna o bitmap do desenho atual.
     */
    fun getDrawingBitmap(): Bitmap? = drawingEngine?.getBitmap()

    /**
     * Inicializa o DrawingEngine quando a view muda de tamanho.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            post {
                drawingEngine?.init(w, h)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Iniciar traço
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
                // Mover traço
                drawingEngine?.moveStroke(x, y)
                lastX = x
                lastY = y
                feedbackX = x
                feedbackY = y
                feedbackRadius = (feedbackRadius + 2f).coerceAtMost(40f)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // Finalizar traço
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

        // Desenhar o bitmap do drawing engine
        val bitmap = drawingEngine?.getBitmap()
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }

        // Feedback visual do toque
        if (showFeedback) {
            // Círculo externo pulsante
            feedbackPaint.alpha = ((1f - feedbackRadius / 40f) * 80).toInt().coerceIn(0, 80)
            canvas.drawCircle(feedbackX, feedbackY, feedbackRadius, feedbackPaint)

            // Preenchimento sutil
            feedbackFillPaint.alpha = ((1f - feedbackRadius / 40f) * 30).toInt().coerceIn(0, 30)
            canvas.drawCircle(feedbackX, feedbackY, feedbackRadius * 0.5f, feedbackFillPaint)
        }
    }
}
