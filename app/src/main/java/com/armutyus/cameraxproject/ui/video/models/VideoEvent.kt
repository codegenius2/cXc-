package com.armutyus.cameraxproject.ui.video.models

import android.net.Uri
import androidx.camera.core.CameraInfo
import com.armutyus.cameraxproject.ui.photo.models.CameraState
import com.armutyus.cameraxproject.ui.video.VideoCaptureManager

sealed class VideoEvent {
    data class CameraInitialized(val cameraLensInfo: HashMap<Int, CameraInfo>) : VideoEvent()

    data class OnProgress(val progress: Int) : VideoEvent()
    object RecordingPaused : VideoEvent()
    data class RecordingEnded(val outputUri: Uri) : VideoEvent()
    object Error : VideoEvent()
    object SwitchToPhoto : VideoEvent()
    data class StateChanged(val cameraState: CameraState) : VideoEvent()

    object SetVideoQuality : VideoEvent()
    object FlashTapped : VideoEvent()
    object FlipTapped : VideoEvent()
    data class ThumbnailTapped(val uri: Uri) : VideoEvent()
    object DelayTimerTapped : VideoEvent()
    object SettingsTapped : VideoEvent()

    data class RecordTapped(
        val timeMillis: Long = 0L,
        val videoCaptureManager: VideoCaptureManager
    ) : VideoEvent()

    data class PauseTapped(val videoCaptureManager: VideoCaptureManager) : VideoEvent()
    data class ResumeTapped(val videoCaptureManager: VideoCaptureManager) : VideoEvent()
    data class StopTapped(val videoCaptureManager: VideoCaptureManager) : VideoEvent()

}
