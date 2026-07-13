package com.arwallcanvas.ui

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.TextureView
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

/**
 * Wrapper sobre TextureView que implementa SurfaceProvider
 * para ser usado com CameraX.
 */
class CameraPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), SurfaceProvider {

    init {
        isOpaque = false
    }

    override fun onSurfaceRequested(request: SurfaceRequest) {
        surfaceTexture?.let { texture ->
            request.provideSurface(
                texture,
                ContextCompat.getMainExecutor(context)
            ) { result ->
                // Resultado da requisicao (pode ser OK ou cancelled)
            }
        } ?: run {
            request.requestCancelled()
        }
    }

    override fun getExecutor(): Executor = ContextCompat.getMainExecutor(context)
}
