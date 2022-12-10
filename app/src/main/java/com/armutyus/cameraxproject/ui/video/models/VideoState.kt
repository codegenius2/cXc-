package com.armutyus.cameraxproject.ui.video.models

import android.net.Uri
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.video.Quality
import com.armutyus.cameraxproject.ui.photo.models.CameraState
import com.armutyus.cameraxproject.util.Util

data class VideoState(
    val cameraState: CameraState = CameraState.READY,
    val lens: Int? = null,
    val delayTimer: Int = Util.TIMER_OFF,
    @TorchState.State val torchState: Int = TorchState.OFF,
    @ImageCapture.FlashMode val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val supportedQualities: List<Quality> = mutableListOf(),
    val quality: Quality = Quality.HIGHEST,
    val latestVideoUri: Uri? = null,
    val lensInfo: MutableMap<Int, CameraInfo> = mutableMapOf(),
    val recordedLength: Int = 0,
    val recordingStatus: RecordingStatus = RecordingStatus.Idle
)
