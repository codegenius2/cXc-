package com.armutyus.cameraxproject.util

import android.content.Context
import com.armutyus.cameraxproject.util.Util.Companion.FILENAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileManager(private val context: Context) {

    fun getPrivateFileDirectory(dir: String): File? {
        val directory = File(context.getExternalFilesDir("cameraXproject"), dir)
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

}