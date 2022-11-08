package com.armutyus.cameraxproject.ui.photo.models

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.extensions.ExtensionMode

data class PreviewPhotoState(
    val cameraState: CameraState = CameraState.NOT_READY,
    @ImageCapture.FlashMode val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    @TorchState.State val torchState: Int = TorchState.OFF,
    @ExtensionMode.Mode val extensionMode: Int = ExtensionMode.NONE,
    val cameraLens: Int = CameraSelector.LENS_FACING_BACK
)
