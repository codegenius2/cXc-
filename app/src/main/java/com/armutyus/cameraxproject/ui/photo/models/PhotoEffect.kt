package com.armutyus.cameraxproject.ui.photo.models

import com.armutyus.cameraxproject.util.Util

sealed class PhotoEffect {
    data class ShowMessage(val message: String = Util.GENERAL_ERROR_MESSAGE) : PhotoEffect()
    data class CaptureImage(val filePath: String) : PhotoEffect()
    data class NavigateTo(val route: String) : PhotoEffect()
}
