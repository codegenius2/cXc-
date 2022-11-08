package com.armutyus.cameraxproject.ui.video.models

import com.armutyus.cameraxproject.util.Util

sealed class VideoEffect {

    data class ShowMessage(val message: String = Util.GENERAL_ERROR_MESSAGE) : VideoEffect()
    data class RecordVideo(val filePath: String) : VideoEffect()
    data class NavigateTo(val route: String) : VideoEffect()

    object PauseRecording : VideoEffect()
    object ResumeRecording : VideoEffect()
    object StopRecording : VideoEffect()
}
