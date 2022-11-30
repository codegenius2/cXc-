package com.armutyus.cameraxproject.ui.gallery.preview.models

import android.content.Context
import java.io.File

sealed class PreviewScreenEvent {

    data class ShareTapped(val context: Context, val file: File) : PreviewScreenEvent()
    data class DeleteTapped(val file: File) : PreviewScreenEvent()
    data class FullScreenToggleTapped(val isFullScreen: Boolean) : PreviewScreenEvent()
    data class ChangeBarState(val zoomState: Boolean) : PreviewScreenEvent()
    data class HideController(val isPlaying: Boolean) : PreviewScreenEvent()
    object EditTapped : PreviewScreenEvent()
    object PlayerViewTapped : PreviewScreenEvent()

}