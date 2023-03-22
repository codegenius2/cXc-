package com.armutyus.cameraxproject.ui.photo.models

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import com.armutyus.cameraxproject.ui.photo.PhotoCaptureManager

sealed class PhotoEvent {
    data class CameraInitialized(val cameraLensInfo: HashMap<Int, CameraInfo>) : PhotoEvent()
    data class ImageCaptured(val imageResult: ImageCapture.OutputFileResults) : PhotoEvent()
    data class CaptureTapped(
        val timeMillis: Long = 0L,
        val photoCaptureManager: PhotoCaptureManager
    ) : PhotoEvent()

    data class EditIconTapped(val context: Context) : PhotoEvent()

    data class ThumbnailTapped(val uri: Uri) : PhotoEvent()

    object SwitchToVideo : PhotoEvent()
    object DelayTimerTapped : PhotoEvent()
    object FlashTapped : PhotoEvent()
    object FlipTapped : PhotoEvent()
}
