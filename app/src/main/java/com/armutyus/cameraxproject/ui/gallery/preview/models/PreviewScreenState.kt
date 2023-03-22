package com.armutyus.cameraxproject.ui.gallery.preview.models

import com.armutyus.cameraxproject.util.Util.Companion.FILTER_NAME

data class PreviewScreenState(
    val isFullScreen: Boolean = false,
    val isInEditMode: Boolean = false,
    val switchEditMode: String = FILTER_NAME,
    val showBars: Boolean = false,
    val showMediaController: Boolean = false
)