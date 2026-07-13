package com.arwallcanvas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.arwallcanvas.drawing.DrawingEngine
import com.arwallcanvas.ui.DrawingOverlayView
import com.arwallcanvas.utils.BitmapUtils
import com.arwallcanvas.utils.ColorPicker
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var drawingOverlay: DrawingOverlayView
    private lateinit var colorPicker: ColorPicker
    private lateinit var drawingEngine: DrawingEngine
    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private var cameraFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // IDs do layout usam snake_case! NÃO camelCase.
        previewView = findViewById(R.id.preview_view)
        drawingOverlay = findViewById(R.id.drawing_overlay)
        colorPicker = findViewById(R.id.color_picker)

        drawingEngine = DrawingEngine()
        drawingOverlay.setDrawingEngine(drawingEngine)

        colorPicker.setOnColorSelectedListener { color ->
            drawingEngine.setColor(color)
        }

        findViewById<ImageButton>(R.id.btn_brush).setOnClickListener {
            drawingEngine.setTool(DrawingEngine.BrushTool.BRUSH)
        }
        findViewById<ImageButton>(R.id.btn_spray).setOnClickListener {
            drawingEngine.setTool(DrawingEngine.BrushTool.SPRAY)
        }
        findViewById<ImageButton>(R.id.btn_marker).setOnClickListener {
            drawingEngine.setTool(DrawingEngine.BrushTool.MARKER)
        }
        findViewById<ImageButton>(R.id.btn_eraser).setOnClickListener {
            drawingEngine.setTool(DrawingEngine.BrushTool.ERASER)
        }
        findViewById<ImageButton>(R.id.btn_undo).setOnClickListener {
            drawingEngine.undo()
        }
        findViewById<ImageButton>(R.id.btn_redo).setOnClickListener {
            drawingEngine.redo()
        }
        findViewById<ImageButton>(R.id.btn_clear).setOnClickListener {
            drawingEngine.clear()
        }

        findViewById<SeekBar>(R.id.size_slider).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, v: Int, b: Boolean) { drawingEngine.setSize(v / 10f) }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })

        findViewById<SeekBar>(R.id.opacity_slider).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, v: Int, b: Boolean) { drawingEngine.setOpacity(v / 255f) }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })

        findViewById<ImageButton>(R.id.btn_save).setOnClickListener {
            saveDrawing()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        } else {
            startCamera()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraFacing).build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun saveDrawing() {
        val bitmap = drawingEngine.getBitmap()
        if (bitmap == null || !drawingEngine.hasContent()) {
            Toast.makeText(this, "Nada para salvar", Toast.LENGTH_SHORT).show()
            return
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(dir, "ARCanvas_$timeStamp.png")
        BitmapUtils.saveBitmap(bitmap, file)
        Toast.makeText(this, "Salvo: ${file.name}", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
