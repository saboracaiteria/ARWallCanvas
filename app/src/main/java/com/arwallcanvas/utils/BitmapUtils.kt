package com.arwallcanvas.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

object BitmapUtils {

    fun saveBitmap(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun createCanvas(width: Int, height: Int): Canvas {
        return Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888))
    }

    fun combineBitmaps(background: Bitmap, overlay: Bitmap): Bitmap {
        val result = background.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        canvas.drawBitmap(overlay, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        return result
    }
}
