package com.arwallcanvas

import androidx.camera.core.ImageProxy
import com.google.ar.core.Frame
import com.google.ar.core.Session

/**
 * Helper para processar dados de profundidade do ARCore.
 * Utiliza Depth API para mapear superfícies e paredes.
 */
class DepthHelper(private val session: Session) {

    private var depthMap: FloatArray? = null
    private var depthWidth: Int = 0
    private var depthHeight: Int = 0

    fun processFrame(imageProxy: ImageProxy) {
        // Processa o frame da câmera para extrair profundidade
        try {
            val frame = session.update()
            updateDepthData(frame)
        } catch (e: Exception) {
            // ARCore pode não estar pronto ainda
        }
    }

    private fun updateDepthData(frame: Frame) {
        // Obtém a imagem de profundidade, se disponível
        val depthImage = frame.acquireDepthImage() ?: return

        depthImage.use { image ->
            depthWidth = image.width
            depthHeight = image.height

            val planes = image.planes
            if (planes.isNotEmpty()) {
                val plane = planes[0]
                val buffer = plane.buffer
                depthMap = FloatArray(depthWidth * depthHeight)
                buffer.asFloatBuffer()?.get(depthMap)
            }
        }
    }

    fun getDepthAt(normalizedX: Float, normalizedY: Float): Float? {
        val map = depthMap ?: return null
        val x = (normalizedX * depthWidth).toInt().coerceIn(0, depthWidth - 1)
        val y = (normalizedY * depthHeight).toInt().coerceIn(0, depthHeight - 1)
        return map[y * depthWidth + x]
    }

    fun hasDepthData(): Boolean = depthMap != null
}
