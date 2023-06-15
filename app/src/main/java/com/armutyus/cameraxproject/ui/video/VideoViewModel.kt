package com.armutyus.cameraxproject.ui.video

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.video.Quality
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.armutyus.cameraxproject.ui.photo.models.CameraState
import com.armutyus.cameraxproject.ui.video.models.RecordingStatus
import com.armutyus.cameraxproject.ui.video.models.VideoEvent
import com.armutyus.cameraxproject.ui.video.models.VideoState
import com.armutyus.cameraxproject.util.BaseViewModel
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util
import com.armutyus.cameraxproject.util.Util.Companion.CAPTURE_FAIL
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_CONTENT
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_EXTENSION
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoViewModel constructor(
    private val fileManager: FileManager,
    navController: NavController
) : BaseViewModel(navController) {

    private val _videoState: MutableLiveData<VideoState> = MutableLiveData(VideoState())
    val videoState: LiveData<VideoState> = _videoState

    fun onEvent(videoEvent: VideoEvent) {
        when (videoEvent) {
            VideoEvent.FlashTapped -> onFlashTapped()
            VideoEvent.FlipTapped -> onFlipTapped()
            VideoEvent.DelayTimerTapped -> onDelayTimerTapped()
            VideoEvent.SetVideoQuality -> onSetVideoQuality()
            VideoEvent.SwitchToPhoto -> switchCameraMode()
            is VideoEvent.ThumbnailTapped -> onThumbnailTapped(videoEvent.uri)

            is VideoEvent.PauseTapped -> onPauseTapped(videoEvent.videoCaptureManager)
            is VideoEvent.ResumeTapped -> onResumeTapped(videoEvent.videoCaptureManager)
            is VideoEvent.StopTapped -> onStopTapped(videoEvent.videoCaptureManager)

            is VideoEvent.RecordTapped -> onRecordTapped(
                videoEvent.timeMillis,
                videoEvent.videoCaptureManager
            )

            is VideoEvent.CameraInitialized -> onCameraInitialized(videoEvent.cameraLensInfo)
            is VideoEvent.StateChanged -> onStateChanged(videoEvent.cameraState)
            is VideoEvent.OnProgress -> onProgress(videoEvent.progress)
            is VideoEvent.RecordingPaused -> onPaused()
            is VideoEvent.RecordingEnded -> onRecordingEnded(videoEvent.outputUri)
            VideoEvent.Error -> onError()
        }
    }

    private fun onFlashTapped() = viewModelScope.launch {
        _videoState.value = when (_videoState.value!!.torchState) {
            TorchState.OFF -> _videoState.value!!.copy(torchState = TorchState.ON)
            TorchState.ON -> _videoState.value!!.copy(torchState = TorchState.OFF)
            else -> _videoState.value!!.copy(torchState = TorchState.OFF)
        }
    }

    private fun onFlipTapped() = viewModelScope.launch {
        val lens = if (_videoState.value!!.lens == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        //Check if the lens has flash unit
        val flashMode = if (_videoState.value!!.lensInfo[lens]?.hasFlashUnit() == true) {
            _videoState.value!!.flashMode
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        if (_videoState.value!!.lensInfo[lens] != null) {
            _videoState.value = _videoState.value!!.copy(
                lens = lens,
                flashMode = flashMode,
                quality = Quality.HIGHEST
            )
        }
    }

    private fun onSetVideoQuality() = viewModelScope.launch {
        _videoState.value = when (_videoState.value!!.quality) {
            Quality.HIGHEST -> _videoState.value!!.copy(
                quality = Quality.SD,
                cameraState = CameraState.CHANGED
            )

            Quality.SD -> _videoState.value!!.copy(
                quality = Quality.HD,
                cameraState = CameraState.CHANGED
            )

            Quality.HD -> _videoState.value!!.copy(
                quality = Quality.FHD,
                cameraState = CameraState.CHANGED
            )

            Quality.FHD -> _videoState.value!!.copy(
                quality = Quality.UHD,
                cameraState = CameraState.CHANGED
            )

            Quality.UHD -> _videoState.value!!.copy(
                quality = Quality.SD,
                cameraState = CameraState.CHANGED
            )

            else -> _videoState.value!!.copy(quality = Quality.HIGHEST)
        }
    }

    private fun onThumbnailTapped(uri: Uri?) = viewModelScope.launch {
        navigateTo("preview_screen/?filePath=${uri?.toString()}/?contentFilter=${VIDEO_CONTENT}")
    }

    private fun onPauseTapped(videoCaptureManager: VideoCaptureManager) = viewModelScope.launch {
        videoCaptureManager.pauseRecording()
    }

    private fun onResumeTapped(videoCaptureManager: VideoCaptureManager) = viewModelScope.launch {
        videoCaptureManager.resumeRecording()
    }

    private fun onStopTapped(videoCaptureManager: VideoCaptureManager) = viewModelScope.launch {
        videoCaptureManager.stopRecording()
    }

    private fun onRecordTapped(timeMillis: Long, videoCaptureManager: VideoCaptureManager) =
        viewModelScope.launch {
            _videoState.value = _videoState.value!!.copy(cameraState = CameraState.NOT_READY)
            delay(timeMillis)
            try {
                val filePath = fileManager.createFile(VIDEO_DIR, VIDEO_EXTENSION)
                videoCaptureManager.startRecording(filePath)
            } catch (exception: IllegalArgumentException) {
                Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
            }
        }

    private fun onRecordingEnded(uri: Uri?) = viewModelScope.launch {
        if (uri != null && uri.path != null) {
            _videoState.value = _videoState.value!!.copy(
                cameraState = CameraState.READY,
                recordingStatus = RecordingStatus.Idle,
                recordedLength = 0,
                latestVideoUri = uri
            )
        } else {
            val mediaDir = fileManager.getPrivateFileDirectory(VIDEO_DIR)
            val latestVideoUri = mediaDir?.listFiles()?.lastOrNull()?.toUri() ?: Uri.EMPTY
            _videoState.value = _videoState.value!!.copy(
                cameraState = CameraState.READY,
                recordingStatus = RecordingStatus.Idle,
                recordedLength = 0,
                latestVideoUri = latestVideoUri
            )
        }
    }

    private fun onError() = viewModelScope.launch {
        _videoState.value =
            _videoState.value!!.copy(recordedLength = 0, recordingStatus = RecordingStatus.Idle)
    }

    private fun onPaused() = viewModelScope.launch {
        _videoState.value = _videoState.value!!.copy(recordingStatus = RecordingStatus.Paused)
    }

    private fun onProgress(progress: Int) = viewModelScope.launch {
        _videoState.value = _videoState.value!!.copy(
            recordedLength = progress,
            recordingStatus = RecordingStatus.InProgress
        )
    }

    private fun switchCameraMode() = viewModelScope.launch {
        navigateTo(PHOTO_ROUTE)
    }

    private fun onStateChanged(cameraState: CameraState) = viewModelScope.launch {
        _videoState.value = _videoState.value!!.copy(cameraState = cameraState)
    }

    private fun onDelayTimerTapped() = viewModelScope.launch {
        _videoState.value = when (_videoState.value!!.delayTimer) {
            Util.TIMER_OFF -> _videoState.value!!.copy(delayTimer = Util.TIMER_3S)
            Util.TIMER_3S -> _videoState.value!!.copy(delayTimer = Util.TIMER_10S)
            Util.TIMER_10S -> _videoState.value!!.copy(delayTimer = Util.TIMER_OFF)
            else -> _videoState.value!!.copy(delayTimer = Util.TIMER_OFF)
        }
    }

    private fun onCameraInitialized(cameraLensInfo: HashMap<Int, CameraInfo>) {
        if (cameraLensInfo.isNotEmpty()) {
            val defaultLens = if (cameraLensInfo[CameraSelector.LENS_FACING_BACK] != null) {
                CameraSelector.LENS_FACING_BACK
            } else if (cameraLensInfo[CameraSelector.LENS_FACING_FRONT] != null) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                null
            }
            _videoState.value = _videoState.value!!.copy(
                cameraState = CameraState.NOT_READY,
                lens = _videoState.value!!.lens ?: defaultLens,
                lensInfo = cameraLensInfo
            )
        }
    }
}