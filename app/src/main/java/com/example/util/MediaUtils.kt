package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object MediaUtils {
    fun copyUriToInternalStorage(context: Context, uri: Uri, folderName: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val folder = File(context.filesDir, folderName)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val extension = context.contentResolver.getType(uri)?.substringAfterLast("/") ?: "jpg"
            val file = File(folder, "${UUID.randomUUID()}.$extension")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
