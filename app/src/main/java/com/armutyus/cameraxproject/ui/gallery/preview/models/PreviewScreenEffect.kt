package com.armutyus.cameraxproject.ui.gallery.preview.models

import com.armutyus.cameraxproject.util.Util

sealed class PreviewScreenEffect {
    data class ShowMessage(val message: String = Util.GENERAL_ERROR_MESSAGE) : PreviewScreenEffect()
    data class NavigateTo(val route: String) : PreviewScreenEffect()
}
