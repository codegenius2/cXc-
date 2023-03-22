package com.armutyus.cameraxproject.ui.gallery.preview.models

import android.content.Context
import java.io.File

sealed class PreviewScreenEvent {

    data class ShareTapped(val context: Context, val file: File) : PreviewScreenEvent()
    data class DeleteTapped(val file: File) : PreviewScreenEvent()
    data class FullScreenToggleTapped(val isFullScreen: Boolean) : PreviewScreenEvent()
    data class ChangeBarState(val zoomState: Boolean) : PreviewScreenEvent()
    data class HideController(val isPlaying: Boolean) : PreviewScreenEvent()
    data class SaveTapped(val context: Context) : PreviewScreenEvent()
    object EditTapped : PreviewScreenEvent()
    object CancelEditTapped : PreviewScreenEvent()
    object PlayerViewTapped : PreviewScreenEvent()
    object NavigateBack : PreviewScreenEvent()

}