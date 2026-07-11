package com.arwallcanvas.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BitmapUtils {
    fun combineBitmaps(camera: Bitmap?, drawing: Bitmap?): Bitmap? {
        if (drawing == null) return camera; if (camera == null) return drawing
        val result = Bitmap.createBitmap(drawing.width, drawing.height, Bitmap.Config.ARGB_8888)
        val cv = Canvas(result)
        cv.drawBitmap(Bitmap.createScaledBitmap(camera, drawing.width, drawing.height, true), 0f, 0f, null)
        cv.drawBitmap(drawing, 0f, 0f, null)
        return result
    }

    fun saveToGallery(ctx: Context, bitmap: Bitmap, name: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val v = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ARWallCanvas")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)
            uri?.let { ctx.contentResolver.openOutputStream(it)?.use { o -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, o) } }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ARWallCanvas")
            dir.mkdirs(); File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        true
    } catch (e: Exception) { e.printStackTrace(); false }

    fun generateFileName(): String = "ARWallCanvas_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
}
