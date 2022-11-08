package com.armutyus.cameraxproject.ui.photo.models

import android.net.Uri
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import com.armutyus.cameraxproject.util.Util

/**
 * Defines the current UI state of the camera during pre-capture.
 * The state encapsulates the available camera extensions, the available camera lenses to toggle,
 * the current camera lens, the current extension mode, and the state of the camera.
 */
data class PhotoState(
    val cameraState: CameraState = CameraState.NOT_READY,
    val availableExtensions: List<Int> = emptyList(),
    val captureWithDelay: Int = 0,
    val delayTimer: Int = Util.TIMER_OFF,
    @ExtensionMode.Mode val extensionMode: Int = ExtensionMode.NONE,
    @ImageCapture.FlashMode val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val latestImageUri: Uri? = null,
    val lens: Int? = null,
    val lensInfo: MutableMap<Int, CameraInfo> = mutableMapOf(),
    val timeText: String? = null
)

/**
 * Defines the current state of the camera.
 */
enum class CameraState {
    /**
     * Camera hasn't been initialized.
     */
    NOT_READY,

    /**
     * Camera is open and presenting a preview stream.
     */
    READY,

    /**
     * Some values changed on camera state.
     */
    CHANGED
}
