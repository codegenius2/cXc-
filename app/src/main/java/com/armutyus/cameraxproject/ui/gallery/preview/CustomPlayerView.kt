package com.armutyus.cameraxproject.ui.gallery.preview

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_CONTROLS_VISIBILITY
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CustomPlayerView(
    filePath: Uri?,
    modifier: Modifier = Modifier,
    videoPlayer: ExoPlayer,
    isFullScreen: Boolean,
    shouldShowController: Boolean,
    onFullScreenToggle: (isFullScreen: Boolean) -> Unit,
    hideController: (isPlaying: Boolean) -> Unit,
    onPlayerClick: () -> Unit,
    navigateBack: (() -> Unit)? = null
) {
    var isPlaying by rememberSaveable(filePath) { mutableStateOf(videoPlayer.isPlaying) }
    var playbackState by rememberSaveable(filePath) { mutableStateOf(videoPlayer.playbackState) }
    var videoTimer by rememberSaveable(filePath) { mutableStateOf(0f) }
    var totalDuration by rememberSaveable(filePath) { mutableStateOf(0L) }
    var bufferedPercentage by rememberSaveable(filePath) { mutableStateOf(0) }

    BackHandler {
        if (isFullScreen) {
            onFullScreenToggle.invoke(true)
        } else {
            navigateBack?.invoke()
        }
    }

    Box(modifier = modifier) {
        DisposableEffect(videoPlayer) {
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    super.onEvents(player, events)
                    isPlaying = player.isPlaying
                    totalDuration = player.duration
                    videoTimer = player.contentPosition.toFloat()
                    bufferedPercentage = player.bufferedPercentage
                    playbackState = player.playbackState
                }
            }

            videoPlayer.addListener(listener)

            onDispose {
                videoPlayer.removeListener(listener)
            }
        }

        LaunchedEffect(true) {
            while (isActive) {
                videoTimer = videoPlayer.contentPosition.toFloat()
                delay(50)
            }
        }

        LaunchedEffect(shouldShowController) {
            delay(VIDEO_CONTROLS_VISIBILITY)
            if (shouldShowController) hideController(true)
        }

        VideoPlayer(
            filePath = filePath,
            modifier = Modifier.fillMaxSize(),
            videoPlayer = videoPlayer
        ) {
            onPlayerClick()
        }

        CustomMediaController(
            modifier = Modifier.fillMaxSize(),
            isVisible = { shouldShowController },
            isPlaying = { isPlaying },
            playbackState = { playbackState },
            totalDuration = { totalDuration },
            bufferedPercentage = { bufferedPercentage },
            isFullScreen = isFullScreen,
            onReplay = { videoPlayer.seekBack() },
            onForward = { videoPlayer.seekForward() },
            onPauseToggle = {
                when {
                    videoPlayer.isPlaying -> {
                        videoPlayer.pause()
                    }
                    videoPlayer.isPlaying.not() && playbackState == STATE_ENDED -> {
                        videoPlayer.seekTo(0, 0)
                        videoPlayer.playWhenReady = true
                    }
                    else -> {
                        videoPlayer.play()
                    }
                }
                isPlaying = isPlaying.not()
            },
            onSeekChanged = { position -> videoPlayer.seekTo(position.toLong()) },
            videoTimer = { videoTimer },
            onFullScreenToggle = onFullScreenToggle
        )
    }
}

@Composable
private fun VideoPlayer(
    filePath: Uri?,
    modifier: Modifier = Modifier,
    videoPlayer: ExoPlayer,
    onPlayerClick: () -> Unit
) {
    var lifecycle by remember {
        mutableStateOf(Lifecycle.Event.ON_CREATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .clickable { onPlayerClick() }
    ) {
        DisposableEffect(
            AndroidView(
                modifier = modifier,
                factory = {
                    PlayerView(it).apply {
                        player = videoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player?.setMediaItem(androidx.media3.common.MediaItem.fromUri(filePath!!))
                        player?.prepare()

                        when (lifecycle) {
                            Lifecycle.Event.ON_PAUSE -> {
                                onPause()
                                player?.pause()
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                onResume()
                            }
                            else -> Unit
                        }
                    }
                }
            )
        ) {
            onDispose {
                videoPlayer.release()
            }
        }
    }
}