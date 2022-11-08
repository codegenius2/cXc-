package com.armutyus.cameraxproject.ui.video.models

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.video.Quality
import com.armutyus.cameraxproject.ui.photo.models.CameraState
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_MODE

data class PreviewVideoState(
    val cameraState: CameraState = CameraState.READY,
    @ImageCapture.FlashMode val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    @TorchState.State val torchState: Int = TorchState.OFF,
    val quality: Quality = Quality.HIGHEST,
    val extensionMode: Int = VIDEO_MODE,
    val cameraLens: Int = CameraSelector.LENS_FACING_BACK
)
