package com.armutyus.cameraxproject.ui.photo.models

import android.net.Uri
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode

sealed class PhotoEvent {
    data class CameraInitialized(val cameraLensInfo: HashMap<Int, CameraInfo>) : PhotoEvent()
    data class ExtensionModeChanged(val availableExtensions: List<Int>) : PhotoEvent()
    data class ImageCaptured(val imageResult: ImageCapture.OutputFileResults) : PhotoEvent()
    data class Error(val exception: Exception) : PhotoEvent()
    data class SelectCameraExtension(@ExtensionMode.Mode val extension: Int) : PhotoEvent()
    data class CaptureTapped(val timeMillis: Long = 0L) : PhotoEvent()
    data class ThumbnailTapped(val uri: Uri) : PhotoEvent()

    object DelayTimerTapped : PhotoEvent()
    object FlashTapped : PhotoEvent()
    object FlipTapped : PhotoEvent()
    object SettingsTapped : PhotoEvent()
}
