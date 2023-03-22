package com.armutyus.cameraxproject.ui.gallery.preview.videoplayback

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.armutyus.cameraxproject.util.Util
import com.google.accompanist.systemuicontroller.rememberSystemUiController


@UnstableApi
@Composable
fun VideoPlaybackContent(
    filePath: Uri?,
    isFullScreen: Boolean,
    shouldShowController: Boolean,
    onFullScreenToggle: (isFullScreen: Boolean) -> Unit,
    hideController: (isPlaying: Boolean) -> Unit,
    onPlayerClick: () -> Unit,
    navigateBack: () -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(isFullScreen) {
        systemUiController.isSystemBarsVisible = !isFullScreen
    }
    val context = LocalContext.current
    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(Util.VIDEO_REPLAY_5)
            .setSeekForwardIncrementMs(Util.VIDEO_FORWARD_5)
            .build()
    }

    CustomPlayerView(
        filePath = filePath,
        videoPlayer = exoPlayer,
        isFullScreen = isFullScreen,
        shouldShowController = shouldShowController,
        onFullScreenToggle = onFullScreenToggle,
        hideController = hideController,
        onPlayerClick = onPlayerClick,
        navigateBack = navigateBack,
    )
}