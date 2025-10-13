package com.example.fridgetracker.view.screens.edit_product

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Copies URI content to internal app storage
 */
suspend fun copyUriToInternalFile(context: Context, sourceUri: Uri): File? {
    return withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext null

            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val outFile = File(context.filesDir, fileName)

            FileOutputStream(outFile).use { output ->
                input.use { inp ->
                    inp.copyTo(output)
                }
            }

            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Saves Bitmap to internal app storage
 */
suspend fun saveBitmapToInternalFile(context: Context, bmp: Bitmap): File? {
    return withContext(Dispatchers.IO) {
        try {
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val outFile = File(context.filesDir, fileName)

            FileOutputStream(outFile).use { fos: OutputStream ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }

            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}