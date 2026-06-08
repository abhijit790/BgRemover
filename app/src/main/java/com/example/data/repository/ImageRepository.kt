package com.example.data.repository

import android.content.Context
import com.example.data.api.RemoveBgApiClient
import com.example.data.database.HistoryDao
import com.example.data.database.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageRepository(
    private val context: Context,
    private val historyDao: HistoryDao
) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    /**
     * Uploads the image bytes to remove.bg API and returns the background-removed PNG bytes.
     */
    suspend fun removeBackground(apiKey: String, imageBytes: ByteArray, size: String = "preview"): ByteArray = withContext(Dispatchers.IO) {
        val requestFile = imageBytes.toRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image_file", "upload.jpg", requestFile)
        val sizePart = MultipartBody.Part.createFormData("size", size)

        val response = RemoveBgApiClient.service.removeBackground(apiKey, imagePart, sizePart)

        if (response.isSuccessful) {
            val responseBody = response.body() ?: throw Exception("Empty response body from background removal server.")
            responseBody.bytes()
        } else {
            val errorJson = response.errorBody()?.string()
            val errorMessage = RemoveBgApiClient.parseError(errorJson)
            throw Exception(errorMessage)
        }
    }

    /**
     * Saves byte array to internal storage as a file and returns its absolute path.
     */
    suspend fun saveImageToFile(bytes: ByteArray, prefix: String, extension: String = "png"): String = withContext(Dispatchers.IO) {
        val fileName = "${prefix}_${UUID.randomUUID()}.$extension"
        val directory = File(context.filesDir, "bg_remover_images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(bytes)
        }
        file.absolutePath
    }

    /**
     * Inserts removal history into database.
     */
    suspend fun saveHistory(originalPath: String, removedPath: String) = withContext(Dispatchers.IO) {
        val entity = HistoryEntity(
            originalPath = originalPath,
            removedPath = removedPath,
            timestamp = System.currentTimeMillis()
        )
        historyDao.insertHistory(entity)
    }

    /**
     * Deletes a record from the database and deletes its associated files.
     */
    suspend fun deleteHistoryRecord(record: HistoryEntity) = withContext(Dispatchers.IO) {
        try {
            // Delete associated files
            val originalFile = File(record.originalPath)
            if (originalFile.exists()) originalFile.delete()

            val removedFile = File(record.removedPath)
            if (removedFile.exists()) removedFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Delete from database
        historyDao.deleteHistoryById(record.id)
    }

    /**
     * Clear all records and files.
     */
    suspend fun clearAllHistory(records: List<HistoryEntity>) = withContext(Dispatchers.IO) {
        records.forEach { record ->
            try {
                val originalFile = File(record.originalPath)
                if (originalFile.exists()) originalFile.delete()

                val removedFile = File(record.removedPath)
                if (removedFile.exists()) removedFile.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        historyDao.clearHistory()
    }
}
