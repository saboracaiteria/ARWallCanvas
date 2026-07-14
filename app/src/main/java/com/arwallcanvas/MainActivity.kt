package com.arwallcanvas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.arwallcanvas.drawing.BrushTool
import com.arwallcanvas.drawing.DrawingEngine
import com.arwallcanvas.ui.DrawingOverlayView
import com.arwallcanvas.utils.ColorPicker
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var drawingOverlay: DrawingOverlayView
    private lateinit var colorPicker: ColorPicker
    private lateinit var drawingEngine: DrawingEngine
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar views do layout
        previewView = findViewById(R.id.preview_view)
        drawingOverlay = findViewById(R.id.drawing_overlay)
        colorPicker = findViewById(R.id.color_picker)

        // Inicializar motor de desenho
        drawingEngine = DrawingEngine()
        drawingOverlay.setDrawingEngine(drawingEngine)

        // Configurar paleta de cores
        colorPicker.setOnColorSelectedListener { color ->
            drawingEngine.setColor(color)
            colorPicker.visibility = android.view.View.GONE
        }

        // Configurar botões da toolbar
        setupToolbar()

        // Configurar sliders
        setupSliders()

        // Iniciar câmera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
        }

        // Estado inicial
        drawingEngine.setColor(android.graphics.Color.RED)
        updateToolSelection()
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btn_spray).setOnClickListener {
            drawingEngine.setTool(BrushTool.SPRAY)
            updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_brush).setOnClickListener {
            drawingEngine.setTool(BrushTool.BRUSH)
            updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_marker).setOnClickListener {
            drawingEngine.setTool(BrushTool.MARKER)
            updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_eraser).setOnClickListener {
            drawingEngine.setTool(BrushTool.ERASER)
            updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_undo).setOnClickListener {
            drawingEngine.undo()
            drawingOverlay.invalidate()
        }
        findViewById<ImageButton>(R.id.btn_redo).setOnClickListener {
            drawingEngine.redo()
            drawingOverlay.invalidate()
        }
        findViewById<ImageButton>(R.id.btn_clear).setOnClickListener {
            drawingEngine.clear()
            drawingOverlay.invalidate()
            Toast.makeText(this, "Tela limpa", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btn_color).setOnClickListener {
            colorPicker.visibility = if (colorPicker.visibility == android.view.View.VISIBLE)
                android.view.View.GONE else android.view.View.VISIBLE
        }
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener {
            salvarArte()
        }
    }

    private fun updateToolSelection() {
        val map = mapOf(
            R.id.btn_spray to BrushTool.SPRAY,
            R.id.btn_brush to BrushTool.BRUSH,
            R.id.btn_marker to BrushTool.MARKER,
            R.id.btn_eraser to BrushTool.ERASER
        )
        map.forEach { (id, tool) ->
            findViewById<ImageButton>(id).alpha = if (drawingEngine.tool == tool) 1.0f else 0.4f
        }
    }

    private fun setupSliders() {
        findViewById<SeekBar>(R.id.size_slider).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    drawingEngine.setSize(progress.toFloat())
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {}
            }
        )

        findViewById<SeekBar>(R.id.opacity_slider).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    drawingEngine.setOpacity(progress / 100f)
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {}
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun salvarArte() {
        val bitmap = drawingEngine.getBitmap()
        if (bitmap == null || !drawingEngine.hasContent()) {
            Toast.makeText(this, "Nada para salvar — desenhe algo primeiro!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "ARWallCanvas_$timestamp.png"
            val dir = getExternalFilesDir(null)
            val file = File(dir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            Toast.makeText(this, "Salvo como: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_LONG).show()
        }
    }
}
