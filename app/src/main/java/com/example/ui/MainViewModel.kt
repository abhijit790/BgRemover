package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.HistoryEntity
import com.example.data.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ImageRepository(application, database.historyDao())

    // UI state observables
    var originalImagePath by mutableStateOf<String?>(null)
        private set

    var removedImagePath by mutableStateOf<String?>(null)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var progressText by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var apiKey by mutableStateOf("i5u7iHxNPJzfwYwNMrMth6Ew")

    var selectedSize by mutableStateOf("preview")

    // Active design parameters for instantaneous on-client editing
    var activeBgType by mutableStateOf(BackgroundType.TRANSPARENT)
    var activeSolidColor by mutableStateOf(Color.White)
    var activeGradientStart by mutableStateOf(Color(0xFFE0C3FC))
    var activeGradientEnd by mutableStateOf(Color(0xFF8EC5FC))
    var customBgImagePath by mutableStateOf<String?>(null)
        private set

    // Slider comparison state: 0.0f to 1.0f (center is 0.5f)
    var sliderPosition by mutableFloatStateOf(0.5f)

    // Historical records from database
    val history: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var isHistoryExpanded by mutableStateOf(false)

    // Temporary bytes when picking file before uploading
    private var pendingOriginalBytes: ByteArray? = null

    /**
     * Handles image selection from gallery.
     */
    fun selectImage(uri: Uri) {
        viewModelScope.launch {
            try {
                isProcessing = true
                progressText = "Loading chosen image..."
                errorMessage = null

                val context = getApplication<Application>()
                val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                }

                if (bytes == null || bytes.isEmpty()) {
                    throw Exception("Could not load image bytes. The image might be corrupted.")
                }

                pendingOriginalBytes = bytes

                // Save selected image to cache/storage immediately for display
                val path = repository.saveImageToFile(bytes, "original", "jpg")

                originalImagePath = path
                removedImagePath = null // Reset previous completion
                activeBgType = BackgroundType.TRANSPARENT // Reset
                sliderPosition = 0.5f // Reset comparison slider
            } catch (e: Exception) {
                errorMessage = "Failed to load image: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Executes the background removal call to remove.bg.
     */
    fun removeBackground() {
        val originalBytes = pendingOriginalBytes ?: return
        val key = apiKey.trim()

        if (key.isBlank()) {
            errorMessage = "Please enter a valid remove.bg API key first."
            return
        }

        viewModelScope.launch {
            try {
                isProcessing = true
                progressText = "Uploading image..."
                errorMessage = null

                // Step 1: Upload and remove background via remove.bg API
                val removedBytes = repository.removeBackground(key, originalBytes, selectedSize)

                progressText = "Saving completed file..."

                // Step 2: Save the transparent PNG
                val savedRemovedPath = repository.saveImageToFile(removedBytes, "removed", "png")
                removedImagePath = savedRemovedPath

                // Step 3: Insert into Room conversion history
                val savedOriginalPath = originalImagePath ?: ""
                repository.saveHistory(savedOriginalPath, savedRemovedPath)

                // Slide the slider automatically to reveal the removed background!
                sliderPosition = 0.8f
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.localizedMessage ?: "Failed to remove background."
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Handles setting a custom background image from the gallery.
     */
    fun selectCustomBackgroundImage(uri: Uri) {
        viewModelScope.launch {
            try {
                isProcessing = true
                progressText = "Loading background..."
                val context = getApplication<Application>()
                val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                }

                if (bytes != null && bytes.isNotEmpty()) {
                    val path = repository.saveImageToFile(bytes, "custom_bg", "jpg")
                    customBgImagePath = path
                    activeBgType = BackgroundType.CUSTOM_IMAGE
                } else {
                    errorMessage = "Failed to load background image bytes."
                }
            } catch (e: Exception) {
                errorMessage = "Error custom background: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Saves the final composite image (foreground removed + chosen styled background) to the device Gallery.
     */
    fun saveResultToDeviceGallery(onComplete: (Boolean) -> Unit) {
        val remPath = removedImagePath ?: return
        viewModelScope.launch {
            try {
                isProcessing = true
                progressText = "Compositing design..."

                val context = getApplication<Application>()
                val foregroundBitmap = BitmapFactory.decodeFile(remPath)

                if (foregroundBitmap == null) {
                    throw Exception("Could not build foreground image bitmap.")
                }

                // Generate composite in IO thread
                val resultBitmap = withContext(Dispatchers.IO) {
                    compositeDesign(foregroundBitmap)
                }

                progressText = "Writing to gallery..."
                val success = com.example.utils.FileHelper.saveBitmapToGallery(context, resultBitmap, "Removed_Bg_${System.currentTimeMillis()}")
                
                // Recycled as memory cleanup
                if (!foregroundBitmap.isRecycled) foregroundBitmap.recycle()
                if (!resultBitmap.isRecycled) resultBitmap.recycle()

                onComplete(success)
            } catch (e: Exception) {
                errorMessage = "Failed to save: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Composites foreground transparent bitmap with background state parameter.
     */
    private fun compositeDesign(foreground: Bitmap): Bitmap {
        val width = foreground.width
        val height = foreground.height
        val composite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(composite)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Draw Background
        when (activeBgType) {
            BackgroundType.TRANSPARENT -> {
                // Keep it completely transparent!
            }
            BackgroundType.SOLID -> {
                canvas.drawColor(activeSolidColor.toArgb())
            }
            BackgroundType.GRADIENT -> {
                val shader = android.graphics.LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    activeGradientStart.toArgb(), activeGradientEnd.toArgb(),
                    android.graphics.Shader.TileMode.CLAMP
                )
                val paintBg = android.graphics.Paint().apply {
                    this.shader = shader
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)
            }
            BackgroundType.CUSTOM_IMAGE -> {
                customBgImagePath?.let { path ->
                    val bgBitmap = BitmapFactory.decodeFile(path)
                    if (bgBitmap != null) {
                        val src = android.graphics.Rect(0, 0, bgBitmap.width, bgBitmap.height)
                        val dst = android.graphics.Rect(0, 0, width, height)
                        canvas.drawBitmap(bgBitmap, src, dst, paint)
                        bgBitmap.recycle()
                    }
                }
            }
        }

        // Draw Foreground
        canvas.drawBitmap(foreground, 0f, 0f, paint)
        return composite
    }

    /**
     * Toggles history list expanded-collapsed state.
     */
    fun toggleHistory() {
        isHistoryExpanded = !isHistoryExpanded
    }

    /**
     * Loads a specific historical record back to active editor.
     */
    fun loadHistoryRecord(record: HistoryEntity) {
        viewModelScope.launch {
            try {
                // Check if physical files still exist in file storage
                val ogFile = java.io.File(record.originalPath)
                val remFile = record.removedPath?.let { java.io.File(it) }

                if (!ogFile.exists() || remFile == null || !remFile.exists()) {
                    errorMessage = "Original source files do not exist anymore. This historical entry is invalid."
                    repository.deleteHistoryRecord(record) // Auto delete stale DB items
                    return@launch
                }

                // Load bytes to memory in dispatcher for network-free offline customization
                isProcessing = true
                progressText = "Loading historical file..."

                originalImagePath = record.originalPath
                removedImagePath = record.removedPath
                pendingOriginalBytes = withContext(Dispatchers.IO) {
                    ogFile.readBytes()
                }

                // Reset editing choices to default
                activeBgType = BackgroundType.TRANSPARENT
                sliderPosition = 0.8f
                isHistoryExpanded = false
            } catch (e: Exception) {
                errorMessage = "Could not load history: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Deletes a specific historical record and clears active editor if matching.
     */
    fun deleteHistoryRecord(record: HistoryEntity) {
        viewModelScope.launch {
            val isCurrentActive = originalImagePath == record.originalPath || removedImagePath == record.removedPath
            repository.deleteHistoryRecord(record)
            if (isCurrentActive) {
                clearActiveEditor()
            }
        }
    }

    /**
     * Clear active images in editor.
     */
    fun clearActiveEditor() {
        originalImagePath = null
        removedImagePath = null
        pendingOriginalBytes = null
        customBgImagePath = null
        activeBgType = BackgroundType.TRANSPARENT
        sliderPosition = 0.5f
    }

    /**
     * Reset/Clear entire history database.
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory(history.value)
            clearActiveEditor()
        }
    }
}
