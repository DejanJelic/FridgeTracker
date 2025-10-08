package com.example.fridgetracker.utilities

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

object ImageFileUtils {
    private const val TAG = "ImageFileUtils"

    fun isInternalAppFile(uriString: String?, context: Context): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme != "file") return false
            val path = uri.path ?: return false
            val filesDirPath = context.filesDir.absolutePath
            path.startsWith(filesDirPath)
        } catch (t: Throwable) {
            false
        }
    }

    fun deleteInternalFileIfExists(uriString: String?, context: Context): Boolean {
        if (!isInternalAppFile(uriString, context)) return false
        return try {
            val path = Uri.parse(uriString).path ?: return false
            val f = File(path)
            val deleted = f.exists() && f.delete()
            if (deleted) Log.d(TAG, "Deleted internal image file: $path")
            else Log.d(TAG, "Internal image file did not exist or couldn't be deleted: $path")
            deleted
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to delete internal file", t)
            false
        }
    }
}
