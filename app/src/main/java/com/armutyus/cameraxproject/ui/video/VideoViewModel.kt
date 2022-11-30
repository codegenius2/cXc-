package com.armutyus.cameraxproject.ui.video

import android.net.Uri
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.ui.photo.models.CameraState
import com.armutyus.cameraxproject.ui.video.models.RecordingStatus
import com.armutyus.cameraxproject.ui.video.models.VideoEffect
import com.armutyus.cameraxproject.ui.video.models.VideoEvent
import com.armutyus.cameraxproject.ui.video.models.VideoState
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_EXTENSION
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VideoViewModel constructor(
    private val fileManager: FileManager
) : ViewModel() {

    private val _videoState = MutableStateFlow(VideoState())
    val videoState: StateFlow<VideoState> = _videoState

    private val _videoEffect = MutableSharedFlow<VideoEffect>()
    val videoEffect: SharedFlow<VideoEffect> = _videoEffect

    fun onEvent(videoEvent: VideoEvent) {
        when (videoEvent) {
            VideoEvent.FlashTapped -> onFlashTapped()
            VideoEvent.FlipTapped -> onFlipTapped()
            VideoEvent.DelayTimerTapped -> onDelayTimerTapped()
            VideoEvent.SettingsTapped -> onSettingsTapped()
            VideoEvent.SetVideoQuality -> onSetVideoQuality()
            is VideoEvent.ThumbnailTapped -> onThumbnailTapped(videoEvent.uri)

            VideoEvent.PauseTapped -> onPauseTapped()
            VideoEvent.ResumeTapped -> onResumeTapped()
            VideoEvent.StopTapped -> onStopTapped()

            is VideoEvent.RecordTapped -> onRecordTapped(videoEvent.timeMillis)
            is VideoEvent.CameraInitialized -> onCameraInitialized(
                videoEvent.cameraLensInfo,
                videoEvent.qualities
            )
            is VideoEvent.StateChanged -> onStateChanged(videoEvent.cameraState)
            is VideoEvent.SelectCameraExtension -> setExtensionMode(videoEvent.extension)
            is VideoEvent.OnProgress -> onProgress(videoEvent.progress)
            is VideoEvent.RecordingPaused -> onPaused()
            is VideoEvent.RecordingEnded -> onRecordingFinished(videoEvent.outputUri)
            is VideoEvent.Error -> onError()
        }
    }

    private fun onFlashTapped() {
        _videoState.update {
            when (_videoState.value.torchState) {
                TorchState.OFF -> it.copy(torchState = TorchState.ON)
                TorchState.ON -> it.copy(torchState = TorchState.OFF)
                else -> it.copy(torchState = TorchState.OFF)
            }
        }
    }

    private fun onFlipTapped() {
        val lens = if (_videoState.value.lens == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        //Check if the lens has flash unit
        val flashMode = if (_videoState.value.lensInfo[lens]?.hasFlashUnit() == true) {
            _videoState.value.flashMode
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        if (_videoState.value.lensInfo[lens] != null) {
            _videoState.update { it.copy(lens = lens, flashMode = flashMode) }
        }
    }

    private fun onSetVideoQuality() {
        _videoState.update {
            when (_videoState.value.quality) {
                Quality.HIGHEST -> it.copy(quality = Quality.SD, cameraState = CameraState.CHANGED)
                Quality.SD -> it.copy(quality = Quality.HD, cameraState = CameraState.CHANGED)
                Quality.HD -> it.copy(quality = Quality.FHD, cameraState = CameraState.CHANGED)
                Quality.FHD -> it.copy(quality = Quality.UHD, cameraState = CameraState.CHANGED)
                Quality.UHD -> it.copy(quality = Quality.SD, cameraState = CameraState.CHANGED)
                else -> it.copy(quality = Quality.HIGHEST)
            }
        }
    }

    private fun onThumbnailTapped(uri: Uri?) {
        val type = "video"
        viewModelScope.launch {
            _videoEffect.emit(
                VideoEffect.NavigateTo("preview_screen/?filePath=${uri?.toString()}/?itemType=${type}")
            )
        }
    }

    private fun onPauseTapped() {
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.PauseRecording)
        }
    }

    private fun onResumeTapped() {
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.ResumeRecording)
        }
    }

    private fun onStopTapped() {
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.StopRecording)
        }
    }

    private fun onRecordTapped(timeMillis: Long) {
        _videoState.update {
            it.copy(
                cameraState = CameraState.NOT_READY
            )
        }
        viewModelScope.launch {
            delay(timeMillis)
            try {
                val filePath = fileManager.createFile(VIDEO_DIR, VIDEO_EXTENSION)
                _videoEffect.emit(VideoEffect.RecordVideo(filePath))
            } catch (exception: IllegalArgumentException) {
                _videoEffect.emit(VideoEffect.ShowMessage())
            }
        }
    }

    private fun onRecordingFinished(uri: Uri?) {
        if (uri != null && uri.path != null) {
            _videoState.update {
                it.copy(
                    cameraState = CameraState.READY,
                    recordingStatus = RecordingStatus.Idle,
                    recordedLength = 0,
                    latestVideoUri = uri
                )
            }
        } else {
            val mediaDir = fileManager.getPrivateFileDirectory(VIDEO_DIR)
            val latestVideoUri = mediaDir?.listFiles()?.lastOrNull()?.toUri() ?: Uri.EMPTY
            _videoState.update {
                it.copy(
                    cameraState = CameraState.READY,
                    recordingStatus = RecordingStatus.Idle,
                    recordedLength = 0,
                    latestVideoUri = latestVideoUri
                )
            }
        }
    }

    private fun onError() {
        _videoState.update { it.copy(recordedLength = 0, recordingStatus = RecordingStatus.Idle) }
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.ShowMessage())
        }
    }

    private fun onPaused() {
        _videoState.update { it.copy(recordingStatus = RecordingStatus.Paused) }
    }

    private fun onProgress(progress: Int) {
        _videoState.update {
            it.copy(
                recordedLength = progress,
                recordingStatus = RecordingStatus.InProgress
            )
        }
    }

    private fun setExtensionMode(extension: Int) {
        if (extension == ExtensionMode.NONE) {
            viewModelScope.launch {
                _videoEffect.emit(VideoEffect.NavigateTo(PHOTO_ROUTE))
            }
        }
    }

    private fun onStateChanged(cameraState: CameraState) {
        _videoState.update {
            it.copy(cameraState = cameraState)
        }
    }

    private fun onDelayTimerTapped() {
        _videoState.update {
            when (_videoState.value.delayTimer) {
                Util.TIMER_OFF -> it.copy(delayTimer = Util.TIMER_3S)
                Util.TIMER_3S -> it.copy(delayTimer = Util.TIMER_10S)
                Util.TIMER_10S -> it.copy(delayTimer = Util.TIMER_OFF)
                else -> it.copy(delayTimer = Util.TIMER_OFF)
            }
        }
    }

    private fun onSettingsTapped() {
        viewModelScope.launch {
            _videoEffect.emit(VideoEffect.NavigateTo(Util.SETTINGS_ROUTE))
        }
    }

    private fun onCameraInitialized(
        cameraLensInfo: HashMap<Int, CameraInfo>,
        qualities: List<Quality>
    ) {
        if (cameraLensInfo.isNotEmpty()) {
            val defaultLens = if (cameraLensInfo[CameraSelector.LENS_FACING_BACK] != null) {
                CameraSelector.LENS_FACING_BACK
            } else if (cameraLensInfo[CameraSelector.LENS_FACING_BACK] != null) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                null
            }
            _videoState.update {
                it.copy(
                    cameraState = CameraState.NOT_READY,
                    lens = it.lens ?: defaultLens,
                    lensInfo = cameraLensInfo,
                    supportedQualities = qualities
                )
            }
        }
    }
}