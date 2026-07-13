package com.arwallcanvas.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitários para manipulação de bitmaps e salvamento de arte.
 */
object BitmapUtils {

    /**
     * Combina dois bitmaps: camera + desenho.
     */
    fun combineBitmaps(cameraBitmap: Bitmap?, drawingBitmap: Bitmap?): Bitmap? {
        if (drawingBitmap == null) return cameraBitmap
        if (cameraBitmap == null) return drawingBitmap

        val combined = Bitmap.createBitmap(
            drawingBitmap.width, drawingBitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(combined)

        // Draw camera background
        val scaledCamera = Bitmap.createScaledBitmap(
            cameraBitmap, drawingBitmap.width, drawingBitmap.height, true
        )
        canvas.drawBitmap(scaledCamera, 0f, 0f, null)

        // Draw drawing overlay
        canvas.drawBitmap(drawingBitmap, 0f, 0f, null)

        return combined
    }

    /**
     * Salva bitmap na galeria.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/ARWallCanvas"
                    )
                }
                val uri: Uri? = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES + "/ARWallCanvas"
                )
                dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Gera nome de arquivo único com timestamp.
     */
    fun generateFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "ARWallCanvas_$timestamp.png"
    }
}
