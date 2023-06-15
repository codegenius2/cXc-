package com.armutyus.cameraxproject.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import com.armutyus.cameraxproject.util.Util.Companion.APP_NAME
import com.armutyus.cameraxproject.util.Util.Companion.FILENAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class FileManager(private val context: Context) {

    fun getPrivateFileDirectory(dir: String): File? {
        val directory = File(context.getExternalFilesDir(APP_NAME), dir)
        return if (directory.exists() || directory.mkdirs()) {
            directory
        } else context.filesDir
    }

    suspend fun createFile(directory: String, ext: String): String {
        return withContext(Dispatchers.IO) {
            val timestamp = SimpleDateFormat(
                FILENAME,
                Locale.getDefault()
            ).format(System.currentTimeMillis())
            return@withContext File(
                getPrivateFileDirectory(directory),
                "$timestamp.$ext"
            ).canonicalPath
        }
    }

    suspend fun saveEditedImageToFile(bitmap: Bitmap, directory: String, ext: String): Uri {
        return withContext(Dispatchers.IO) {
            val timestamp = SimpleDateFormat(
                FILENAME,
                Locale.getDefault()
            ).format(System.currentTimeMillis())
            val file = File(getPrivateFileDirectory(directory), "cXc_$timestamp.$ext")
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                it.close()
            }
            return@withContext file.toUri()
        }
    }

}