package com.armutyus.cameraxproject.ui.gallery.preview.videoplayback

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.STATE_ENDED
import com.armutyus.cameraxproject.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CustomMediaController(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isPlaying: Boolean,
    videoTimer: Float,
    bufferedPercentage: Int,
    playbackState: Int,
    totalDuration: Long,
    isFullScreen: Boolean,
    onPauseToggle: () -> Unit,
    onReplay: () -> Unit,
    onForward: () -> Unit,
    onSeekChanged: (newValue: Float) -> Unit,
    onFullScreenToggle: (isFullScreen: Boolean) -> Unit
) {

    val visible = remember(isVisible) { isVisible }

    val playing = remember(isPlaying) { isPlaying }

    val duration = remember(totalDuration) { totalDuration.coerceAtLeast(0) }

    val timer = remember(videoTimer) { videoTimer }

    val buffer = remember(bufferedPercentage) { bufferedPercentage }

    val playerState = remember(playbackState) { playbackState }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
        ) {

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = if (isFullScreen) {
                    Arrangement.Center
                } else {
                    Arrangement.SpaceEvenly
                }
            ) {

                VideoReplayIcon {
                    onReplay()
                }

                when {
                    playing -> {
                        VideoPauseIcon {
                            onPauseToggle()
                        }
                    }

                    playing.not() && playerState == STATE_ENDED -> {
                        VideoPlayIcon {
                            onPauseToggle()
                        }
                    }

                    else -> {
                        VideoPlayIcon {
                            onPauseToggle()
                        }
                    }
                }

                VideoForwardIcon {
                    onForward()
                }

            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = if (isFullScreen) 32.dp else 16.dp)
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight: Int -> fullHeight }
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight: Int -> fullHeight }
                        )
                    )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = buffer.toFloat(),
                        enabled = false,
                        onValueChange = { /*do nothing*/ },
                        valueRange = 0f..100f,
                        colors =
                        SliderDefaults.colors(
                            disabledThumbColor = Color.Transparent,
                            disabledActiveTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Slider(
                        value = timer,
                        onValueChange = {
                            onSeekChanged.invoke(it)
                        },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onBackground,
                            activeTrackColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .animateEnterExit(
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight: Int -> fullHeight }
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { fullHeight: Int -> fullHeight }
                                )
                            ),
                        text = duration.formatMinSec(),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    FullScreenToggleIcon(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp)
                            .animateEnterExit(
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight: Int -> fullHeight }
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { fullHeight: Int -> fullHeight }
                                )
                            ),
                        isFullScreen = isFullScreen
                    ) {
                        onFullScreenToggle(isFullScreen.not())
                    }
                }
            }
        }
    }

}