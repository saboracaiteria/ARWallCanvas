package com.arwallcanvas.ui

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceProvider
import androidx.core.content.ContextCompat

class CameraPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), SurfaceProvider {

    init { isOpaque = false }

    override fun onSurfaceRequested(request: SurfaceRequest) {
        surfaceTexture?.let {
            request.provideSurface(it, ContextCompat.getMainExecutor(context)) { _ -> }
        } ?: request.requestCancelled()
    }
}
