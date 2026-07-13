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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * MainActivity do ARWallCanvas.
 *
 * Abre a câmera, sobrepõe uma camada de desenho e fornece
 * ferramentas de pintura (spray, pincel, marcador, borracha)
 * com suporte a desfazer/refazer, paleta de cores e salvamento.
 *
 * Inspirado na filosofia Augmented Graffiti — cada traço importa.
 */
class MainActivity : AppCompatActivity() {

    // Componentes de UI
    private lateinit var previewView: PreviewView
    private lateinit var drawingOverlay: DrawingOverlayView
    private lateinit var colorPicker: ColorPicker
    private lateinit var toolbar: View

    // Motor de desenho
    private lateinit var drawingEngine: DrawingEngine

    // Câmera
    private lateinit var imageCapture: ImageCapture
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Estado atual
    private var currentTool = BrushTool.SPRAY
    private var currentColor = Color.RED
    private var currentSize = 20f
    private var currentOpacity = 0.8f

    // Launcher de permissão
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar views
        initViews()

        // Inicializar motor de desenho
        drawingEngine = DrawingEngine(this)
        drawingOverlay.setDrawingEngine(drawingEngine)

        // Configurar câmera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Configurar controles da UI
        setupToolbar()
        setupSliders()
        setupColorPicker()

        // Estado inicial
        drawingEngine.setTool(currentTool)
        drawingEngine.setColor(currentColor)
        drawingEngine.setSize(currentSize)
        drawingEngine.setOpacity(currentOpacity)
        updateToolSelection()
    }

    private fun initViews() {
        previewView = findViewById(R.id.preview_view)
        drawingOverlay = findViewById(R.id.drawing_overlay)
        colorPicker = findViewById(R.id.color_picker)
        toolbar = findViewById(R.id.tool_bar)
    }

    // ==================== CÂMERA ====================

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Erro na câmera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ==================== BARRA DE FERRAMENTAS ====================

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btn_spray)?.setOnClickListener {
            currentTool = BrushTool.SPRAY
            drawingEngine.setTool(currentTool)
            updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_brush)?.setOnClickListener {
            currentTool = BrushTool.BRUSH
            drawingEngine.setTool(currentTool)
            updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_marker)?.setOnClickListener {
            currentTool = BrushTool.MARKER
            drawingEngine.setTool(currentTool)
            updateToolSelection()
        }
        findViewById<ImageButton>(R.id.btn_eraser)?.setOnClickListener {
            currentTool = BrushTool.ERASER
            drawingEngine.setTool(currentTool)
            updateToolSelection()
        }

        findViewById<ImageButton>(R.id.btn_undo)?.setOnClickListener {
            drawingEngine.undo()
            drawingOverlay.invalidate()
        }
        findViewById<ImageButton>(R.id.btn_redo)?.setOnClickListener {
            drawingEngine.redo()
            drawingOverlay.invalidate()
        }
        findViewById<ImageButton>(R.id.btn_clear)?.setOnClickListener {
            drawingEngine.clear()
            drawingOverlay.invalidate()
            Toast.makeText(this, "Tela limpa", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btn_color)?.setOnClickListener {
            colorPicker.visibility = if (colorPicker.visibility == View.VISIBLE)
                View.GONE else View.VISIBLE
        }
        findViewById<ImageButton>(R.id.btn_save)?.setOnClickListener {
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
            findViewById<ImageButton>(id)?.alpha = if (tool == currentTool) 1.0f else 0.4f
        }
    }

    // ==================== PALETA DE CORES ====================

    private fun setupColorPicker() {
        colorPicker.setOnColorSelectedListener { color ->
            currentColor = color
            drawingEngine.setColor(color)
            colorPicker.visibility = View.GONE
        }
    }

    // ==================== SLIDERS ====================

    private fun setupSliders() {
        findViewById<SeekBar>(R.id.size_slider)?.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    currentSize = (progress + 5).toFloat()
                    drawingEngine.setSize(currentSize)
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {}
            }
        )

        findViewById<SeekBar>(R.id.opacity_slider)?.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    currentOpacity = progress / 100f
                    drawingEngine.setOpacity(currentOpacity)
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {}
            }
        )
    }

    // ==================== SALVAR ====================

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
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            Toast.makeText(this, "Salvo como: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
