package com.armutyus.cameraxproject.ui.video.models

sealed class RecordingStatus {
    object Idle : RecordingStatus()
    object InProgress : RecordingStatus()
    object Paused : RecordingStatus()
}