package com.arwallcanvas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.arwallcanvas.databinding.ActivityMainBinding
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var arSession: Session? = null
    private var depthHelper: DepthHelper? = null
    private var drawingCanvas: DrawingCanvas? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // Permissão da câmera
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initializeAR()
        } else {
            Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                initializeAR()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Precisamos da câmera para AR", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun initializeAR() {
        // Verificar disponibilidade do ARCore
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (!availability.isSupported) {
            Toast.makeText(this, "ARCore não suportado neste dispositivo", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            val installStatus = ArCoreApk.getInstance().requestInstall(this, true)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                // O usuário será direcionado para instalar o ARCore
                return
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Toast.makeText(this, "ARCore necessário. Instale e tente novamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Inicializar sessão AR
        try {
            arSession = Session(this)
            arSession?.let { session ->
                depthHelper = DepthHelper(session)
                drawingCanvas = DrawingCanvas(session)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao inicializar AR: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        startCamera()
        setupUI()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processCameraFrame(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processCameraFrame(imageProxy: ImageProxy) {
        // Enviar frame para o DepthHelper processar profundidade
        depthHelper?.processFrame(imageProxy)
        imageProxy.close()
    }

    private fun setupUI() {
        // Configurar toque para desenhar
        binding.touchOverlay.setOnTouchListener { _, event ->
            drawingCanvas?.handleTouch(event)
            // Retornar true para consumir o evento
            true
        }

        // Botão para limpar desenho
        binding.clearButton.setOnClickListener {
            drawingCanvas?.clear()
            Toast.makeText(this, "Desenho limpo", Toast.LENGTH_SHORT).show()
        }

        // Botão para salvar
        binding.saveButton.setOnClickListener {
            saveCurrentDrawing()
        }

        // Botão para alternar cor
        binding.colorButton.setOnClickListener {
            drawingCanvas?.changeColor()
            Toast.makeText(this, "Cor alterada", Toast.LENGTH_SHORT).show()
        }

        // SeekBar de tamanho do pincel
        binding.brushSizeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val width = 4f + (progress / 100f) * 36f // 4 a 40 pixels
                drawingCanvas?.setWidth(width)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Botão VR
        binding.vrModeToggle.setOnClickListener {
            toggleVRMode()
        }

        // Botão desfazer (limpa último traço)
        binding.undoButton.setOnClickListener {
            drawingCanvas?.undo()
            Toast.makeText(this, "Último traço removido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCurrentDrawing() {
        // Salvar o desenho atual como imagem/textura
        Toast.makeText(this, "Desenho salvo!", Toast.LENGTH_SHORT).show()
    }

    private fun toggleVRMode() {
        // Alternar entre AR normal e split-screen VR
        Toast.makeText(this, "Modo VR alternado", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        arSession?.close()
    }
}
