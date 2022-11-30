package com.armutyus.cameraxproject.ui.gallery.models

import com.armutyus.cameraxproject.util.Util

sealed class GalleryEffect {
    data class ShowMessage(val message: String = Util.GENERAL_ERROR_MESSAGE) : GalleryEffect()
    data class NavigateTo(val route: String) : GalleryEffect()
}