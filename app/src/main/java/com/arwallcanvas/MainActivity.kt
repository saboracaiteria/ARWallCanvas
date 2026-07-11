package com.arwallcanvas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.arwallcanvas.drawing.BrushTool
import com.arwallcanvas.drawing.DrawingEngine
import com.arwallcanvas.ui.DrawingOverlayView
import com.arwallcanvas.utils.ColorPicker
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var drawingOverlay: DrawingOverlayView
    private lateinit var colorPicker: ColorPicker
    private lateinit var drawingEngine: DrawingEngine
    private var imageCapture: ImageCapture? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var currentTool = BrushTool.SPRAY
    private var currentColor = Color.RED
    private var currentSize = 20f
    private var currentOpacity = 0.8f

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.preview_view)
        drawingOverlay = findViewById(R.id.drawing_overlay)
        colorPicker = findViewById(R.id.color_picker)

        drawingEngine = DrawingEngine(this)
        drawingOverlay.setDrawingEngine(drawingEngine)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        setupToolbar()
        setupSliders()
        setupColorPicker()

        drawingEngine.setTool(currentTool)
        drawingEngine.setColor(currentColor)
        drawingEngine.setSize(currentSize)
        drawingEngine.setOpacity(currentOpacity)
        updateToolSelection()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Erro câmera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btn_spray)?.setOnClickListener {
            currentTool = BrushTool.SPRAY; drawingEngine.setTool(currentTool); updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_brush)?.setOnClickListener {
            currentTool = BrushTool.BRUSH; drawingEngine.setTool(currentTool); updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_marker)?.setOnClickListener {
            currentTool = BrushTool.MARKER; drawingEngine.setTool(currentTool); updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_eraser)?.setOnClickListener {
            currentTool = BrushTool.ERASER; drawingEngine.setTool(currentTool); updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_undo)?.setOnClickListener {
            drawingEngine.undo(); drawingOverlay.invalidate()
        }
        findViewById<ImageButton>(R.id.btn_redo)?.setOnClickListener {
            drawingEngine.redo(); drawingOverlay.invalidate()
        }
        findViewById<ImageButton>(R.id.btn_clear)?.setOnClickListener {
            drawingEngine.clear(); drawingOverlay.invalidate()
            Toast.makeText(this, "Tela limpa", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btn_color)?.setOnClickListener {
            colorPicker.visibility = if (colorPicker.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        findViewById<ImageButton>(R.id.btn_save)?.setOnClickListener { salvarArte() }
    }

    private fun updateToolSelection() {
        mapOf(
            R.id.btn_spray to BrushTool.SPRAY, R.id.btn_brush to BrushTool.BRUSH,
            R.id.btn_marker to BrushTool.MARKER, R.id.btn_eraser to BrushTool.ERASER
        ).forEach { (id, tool) ->
            findViewById<ImageButton>(id)?.alpha = if (tool == currentTool) 1f else 0.4f
        }
    }

    private fun setupColorPicker() {
        colorPicker.setOnColorSelectedListener { color ->
            currentColor = color; drawingEngine.setColor(color); colorPicker.visibility = View.GONE
        }
    }

    private fun setupSliders() {
        findViewById<SeekBar>(R.id.size_slider)?.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, u: Boolean) {
                    currentSize = (p + 5).toFloat(); drawingEngine.setSize(currentSize)
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            }
        )
        findViewById<SeekBar>(R.id.opacity_slider)?.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, u: Boolean) {
                    currentOpacity = p / 100f; drawingEngine.setOpacity(currentOpacity)
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            }
        )
    }

    private fun salvarArte() {
        val bitmap = drawingEngine.getBitmap()
        if (bitmap == null || !drawingEngine.hasContent()) {
            Toast.makeText(this, "Desenhe algo primeiro!", Toast.LENGTH_SHORT).show(); return
        }
        try {
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(getExternalFilesDir(null), "ARWallCanvas_$ts.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(this, "Salvo: ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy(); cameraExecutor.shutdown()
    }
}
