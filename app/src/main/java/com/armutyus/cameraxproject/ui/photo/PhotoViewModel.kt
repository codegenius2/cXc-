package com.armutyus.cameraxproject.ui.photo

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.photo.models.PhotoEvent
import com.armutyus.cameraxproject.ui.photo.models.PhotoState
import com.armutyus.cameraxproject.util.BaseViewModel
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util.Companion.CAPTURE_FAIL
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_EXTENSION
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_10S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_3S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_OFF
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_ROUTE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhotoViewModel constructor(
    private val fileManager: FileManager,
    navController: NavController
) : BaseViewModel(navController) {

    private val _photoState: MutableLiveData<PhotoState> = MutableLiveData(PhotoState())
    val photoState: LiveData<PhotoState> = _photoState

    fun onEvent(photoEvent: PhotoEvent) {
        when (photoEvent) {
            PhotoEvent.DelayTimerTapped -> onDelayTimerTapped()
            PhotoEvent.FlashTapped -> onFlashTapped()
            PhotoEvent.FlipTapped -> onFlipTapped()

            is PhotoEvent.EditIconTapped -> onEditIconTapped(photoEvent.context)
            is PhotoEvent.ThumbnailTapped -> onThumbnailTapped(photoEvent.uri)
            is PhotoEvent.CaptureTapped -> onCaptureTapped(
                photoEvent.timeMillis,
                photoEvent.photoCaptureManager
            )
            is PhotoEvent.CameraInitialized -> onCameraInitialized(photoEvent.cameraLensInfo)
            is PhotoEvent.ImageCaptured -> onImageCaptured(photoEvent.imageResult.savedUri)
            is PhotoEvent.SwitchToVideo -> switchCameraMode()
        }
    }

    private fun onCaptureTapped(timeMillis: Long, photoCaptureManager: PhotoCaptureManager) =
        viewModelScope.launch {
            delay(timeMillis)
            try {
                val filePath = fileManager.createFile(PHOTO_DIR, PHOTO_EXTENSION)
                photoCaptureManager.takePhoto(
                    filePath, _photoState.value!!.lens
                        ?: CameraSelector.LENS_FACING_BACK
                )
            } catch (exception: IllegalArgumentException) {
                Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
            }
        }

    private fun onDelayTimerTapped() = viewModelScope.launch {
        _photoState.value = when (_photoState.value!!.delayTimer) {
            TIMER_OFF -> _photoState.value!!.copy(delayTimer = TIMER_3S)
            TIMER_3S -> _photoState.value!!.copy(delayTimer = TIMER_10S)
            TIMER_10S -> _photoState.value!!.copy(delayTimer = TIMER_OFF)
            else -> _photoState.value!!.copy(delayTimer = TIMER_OFF)
        }
    }

    private fun onEditIconTapped(context: Context) = viewModelScope.launch {
        Toast.makeText(context, R.string.feature_not_available, Toast.LENGTH_SHORT).show()
    }

    private fun onFlashTapped() = viewModelScope.launch {
        _photoState.value = when (_photoState.value!!.flashMode) {
            ImageCapture.FLASH_MODE_OFF -> _photoState.value!!.copy(flashMode = ImageCapture.FLASH_MODE_AUTO)
            ImageCapture.FLASH_MODE_AUTO -> _photoState.value!!.copy(flashMode = ImageCapture.FLASH_MODE_ON)
            ImageCapture.FLASH_MODE_ON -> _photoState.value!!.copy(flashMode = ImageCapture.FLASH_MODE_OFF)
            else -> _photoState.value!!.copy(flashMode = ImageCapture.FLASH_MODE_OFF)
        }
    }

    private fun onFlipTapped() = viewModelScope.launch {
        val lens = if (_photoState.value!!.lens == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        //Check if the lens has flash unit
        val flashMode = if (_photoState.value!!.lensInfo[lens]?.hasFlashUnit() == true) {
            _photoState.value!!.flashMode
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        if (_photoState.value!!.lensInfo[lens] != null) {
            _photoState.value = _photoState.value!!.copy(lens = lens, flashMode = flashMode)
        }
    }

    private fun switchCameraMode() = viewModelScope.launch {
        navigateTo(VIDEO_ROUTE)
    }

    private fun onThumbnailTapped(uri: Uri?) = viewModelScope.launch {
        navigateTo("preview_screen/?filePath=${uri?.toString()}")
    }

    private fun onImageCaptured(uri: Uri?) = viewModelScope.launch {
        if (uri != null && uri.path != null) {
            _photoState.value = _photoState.value!!.copy(latestImageUri = uri)
        } else {
            val mediaDir = fileManager.getPrivateFileDirectory(PHOTO_DIR)
            val latestImageUri = mediaDir?.listFiles()?.lastOrNull()?.toUri() ?: Uri.EMPTY
            _photoState.value = _photoState.value!!.copy(latestImageUri = latestImageUri)
        }
    }

    private fun onCameraInitialized(cameraLensInfo: HashMap<Int, CameraInfo>) =
        viewModelScope.launch {
            if (cameraLensInfo.isNotEmpty()) {
                val defaultLens = if (cameraLensInfo[CameraSelector.LENS_FACING_BACK] != null) {
                    CameraSelector.LENS_FACING_BACK
                } else if (cameraLensInfo[CameraSelector.LENS_FACING_BACK] != null) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    null
                }
                _photoState.value = _photoState.value!!
                    .copy(lens = _photoState.value!!.lens ?: defaultLens, lensInfo = cameraLensInfo)
            }
        }
}