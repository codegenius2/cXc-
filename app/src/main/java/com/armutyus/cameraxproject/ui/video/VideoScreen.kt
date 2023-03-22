package com.armutyus.cameraxproject.ui.video

import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.camera.core.CameraInfo
import androidx.camera.core.TorchState
import androidx.camera.video.Quality
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armutyus.cameraxproject.ui.photo.models.CameraModesItem
import com.armutyus.cameraxproject.ui.photo.models.CameraState
import com.armutyus.cameraxproject.ui.video.models.PreviewVideoState
import com.armutyus.cameraxproject.ui.video.models.RecordingStatus
import com.armutyus.cameraxproject.ui.video.models.VideoEvent
import com.armutyus.cameraxproject.util.*
import com.armutyus.cameraxproject.util.Util.Companion.APP_NAME
import com.armutyus.cameraxproject.util.Util.Companion.DELAY_10S
import com.armutyus.cameraxproject.util.Util.Companion.DELAY_3S
import com.armutyus.cameraxproject.util.Util.Companion.GENERAL_ERROR_MESSAGE
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_10S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_3S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_OFF
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_MODE
import java.io.File

@Composable
fun VideoScreen(
    factory: ViewModelProvider.Factory,
    videoViewModel: VideoViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    val state by videoViewModel.videoState.observeAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    var rotation by remember {
        mutableStateOf(Surface.ROTATION_0)
    }

    val orientationEventListener by lazy {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == Util.UNKNOWN_ORIENTATION) {
                    return
                }

                rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
    }

    DisposableEffect(key1 = "orientation") {
        orientationEventListener.enable()
        onDispose {
            orientationEventListener.disable()
        }
    }

    LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val listener = remember(videoViewModel) {
        object : VideoCaptureManager.Listener {
            override fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>) {
                videoViewModel.onEvent(VideoEvent.CameraInitialized(cameraLensInfo))
            }

            override fun onVideoStateChanged(cameraState: CameraState) {
                videoViewModel.onEvent(VideoEvent.StateChanged(cameraState))
            }

            override fun recordingPaused() {
                videoViewModel.onEvent(VideoEvent.RecordingPaused)
            }

            override fun onProgress(progress: Int) {
                videoViewModel.onEvent(VideoEvent.OnProgress(progress))
            }

            override fun recordingCompleted(outputUri: Uri) {
                videoViewModel.onEvent(VideoEvent.RecordingEnded(outputUri))
            }

            override fun onError(throwable: Throwable?) {
                videoViewModel.onEvent(VideoEvent.Error)
                onShowMessage(throwable?.localizedMessage ?: GENERAL_ERROR_MESSAGE)
            }
        }
    }

    val videoCaptureManager = remember(videoViewModel) {
        VideoCaptureManager.Builder(context)
            .registerLifecycleOwner(lifecycleOwner)
            .create()
            .apply { this.listener = listener }
    }

    val mediaDir = context.getExternalFilesDir(APP_NAME)?.let {
        File(it, VIDEO_DIR).apply { mkdirs() }
    }

    val latestCapturedVideo = state!!.latestVideoUri ?: mediaDir?.listFiles()?.lastOrNull()?.toUri()

    CompositionLocalProvider(LocalVideoCaptureManager provides videoCaptureManager) {
        VideoScreenContent(
            cameraLens = state!!.lens,
            cameraState = state!!.cameraState,
            videoCaptureManager = videoCaptureManager,
            delayTimer = state!!.delayTimer,
            torchState = state!!.torchState,
            hasFlashUnit = state!!.lensInfo[state!!.lens]?.hasFlashUnit() ?: false,
            hasDualCamera = state!!.lensInfo.size > 1,
            videoUri = latestCapturedVideo,
            quality = state!!.quality,
            recordedLength = state!!.recordedLength,
            recordingStatus = state!!.recordingStatus,
            rotation = rotation,
            view = view,
            onEvent = videoViewModel::onEvent
        )
    }
}

@Composable
private fun VideoScreenContent(
    cameraLens: Int?,
    cameraState: CameraState,
    videoCaptureManager: VideoCaptureManager,
    delayTimer: Int,
    @TorchState.State torchState: Int,
    hasFlashUnit: Boolean,
    hasDualCamera: Boolean,
    videoUri: Uri?,
    quality: Quality,
    recordedLength: Int,
    recordingStatus: RecordingStatus,
    rotation: Int,
    view: View,
    onEvent: (VideoEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        cameraLens?.let {
            CameraPreview(
                cameraState = cameraState,
                lens = it,
                torchState = torchState,
                quality = quality,
            )
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (recordingStatus == RecordingStatus.Idle) {
                    VideoTopControls(
                        showFlashIcon = hasFlashUnit,
                        delayTimer = delayTimer,
                        torchState = torchState,
                        rotation = rotation,
                        quality = quality,
                        onDelayTimerTapped = { onEvent(VideoEvent.DelayTimerTapped) },
                        onFlashTapped = { onEvent(VideoEvent.FlashTapped) }
                    ) { onEvent(VideoEvent.SetVideoQuality) }
                }
                if (recordedLength > 0) {
                    Timer(
                        seconds = recordedLength
                    )
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.Bottom
            ) {
                VideoBottomControls(
                    recordingStatus = recordingStatus,
                    showFlipIcon = hasDualCamera,
                    rotation = rotation,
                    videoUri = videoUri,
                    view = view,
                    onCameraModeTapped = { onEvent(VideoEvent.SwitchToPhoto) },
                    onRecordTapped = {
                        when (delayTimer) {
                            TIMER_OFF -> onEvent(VideoEvent.RecordTapped(0L, videoCaptureManager))
                            TIMER_3S -> onEvent(
                                VideoEvent.RecordTapped(
                                    DELAY_3S,
                                    videoCaptureManager
                                )
                            )
                            TIMER_10S -> onEvent(
                                VideoEvent.RecordTapped(
                                    DELAY_10S,
                                    videoCaptureManager
                                )
                            )
                        }
                    },
                    onStopTapped = { onEvent(VideoEvent.StopTapped(videoCaptureManager)) },
                    onPauseTapped = { onEvent(VideoEvent.PauseTapped(videoCaptureManager)) },
                    onResumeTapped = { onEvent(VideoEvent.ResumeTapped(videoCaptureManager)) },
                    onFlipTapped = { onEvent(VideoEvent.FlipTapped) }
                ) {
                    onEvent(
                        VideoEvent.ThumbnailTapped(
                            videoUri ?: Uri.EMPTY
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun VideoTopControls(
    showFlashIcon: Boolean,
    delayTimer: Int,
    torchState: Int,
    rotation: Int,
    quality: Quality,
    onDelayTimerTapped: () -> Unit,
    onFlashTapped: () -> Unit,
    onQualitySelectorTapped: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(width = 0.5.dp, shape = CircleShape, color = Color.White),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraDelayIcon(
                delayTimer = delayTimer,
                rotation = rotation,
                onTapped = onDelayTimerTapped
            )
            CameraTorchIcon(
                showFlashIcon = showFlashIcon,
                torchState = torchState,
                rotation = rotation,
                onTapped = onFlashTapped
            )
            QualitySelectorIcon(rotation = rotation, quality = quality) {
                onQualitySelectorTapped()
            }
        }
    }
}


@Composable
internal fun VideoBottomControls(
    modifier: Modifier = Modifier,
    recordingStatus: RecordingStatus,
    showFlipIcon: Boolean,
    rotation: Int,
    videoUri: Uri?,
    view: View,
    onCameraModeTapped: () -> Unit,
    onRecordTapped: () -> Unit,
    onStopTapped: () -> Unit,
    onPauseTapped: () -> Unit,
    onResumeTapped: () -> Unit,
    onFlipTapped: () -> Unit,
    onThumbnailTapped: () -> Unit
) {
    val cameraModesList = listOf(
        CameraModesItem(Util.PHOTO_MODE, "Photo", false),
        CameraModesItem(VIDEO_MODE, "Video", true)
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(contentPadding = PaddingValues(16.dp)) {
            items(cameraModesList) { cameraMode ->
                CaptureModesRow(cameraModesItem = cameraMode) {
                    onCameraModeTapped()
                }
            }
        }

        VideoControlsRow(
            showFlipIcon = showFlipIcon,
            recordingStatus = recordingStatus,
            view = view,
            videoUri = videoUri,
            rotation = rotation,
            onRecordTapped = { onRecordTapped() },
            onStopTapped = { onStopTapped() },
            onPauseTapped = { onPauseTapped() },
            onResumeTapped = { onResumeTapped() },
            onFlipTapped = { onFlipTapped() }
        ) { onThumbnailTapped() }
    }
}

@Composable
fun VideoControlsRow(
    showFlipIcon: Boolean,
    recordingStatus: RecordingStatus,
    view: View,
    videoUri: Uri?,
    rotation: Int,
    onRecordTapped: () -> Unit,
    onStopTapped: () -> Unit,
    onPauseTapped: () -> Unit,
    onResumeTapped: () -> Unit,
    onFlipTapped: () -> Unit,
    onThumbnailTapped: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .absolutePadding(left = 24.dp, right = 24.dp, bottom = 24.dp, top = 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapturedVideoThumbnailIcon(imageUri = videoUri, rotation = rotation) {
                onThumbnailTapped()
            }
            when (recordingStatus) {
                RecordingStatus.Idle -> {
                    CameraRecordIcon(
                        onTapped = onRecordTapped,
                        view = view
                    )
                }
                RecordingStatus.Paused -> {
                    CameraStopIcon(
                        onTapped = onStopTapped,
                        view = view
                    )
                    CameraPlayIconSmall(rotation = rotation, onTapped = onResumeTapped)
                }
                RecordingStatus.InProgress -> {
                    CameraStopIcon(
                        onTapped = onStopTapped,
                        view = view
                    )
                    CameraPauseIconSmall(onTapped = onPauseTapped)
                }
            }

            if (showFlipIcon && recordingStatus == RecordingStatus.Idle) {
                CameraFlipIcon(
                    onTapped = onFlipTapped,
                    rotation = rotation,
                    view = view
                )
            }
        }
    }
}

@Composable
fun CaptureModesRow(
    cameraModesItem: CameraModesItem,
    onCameraModeTapped: () -> Unit
) {
    TextButton(
        onClick = {
            if (!cameraModesItem.selected) {
                onCameraModeTapped()
            }
        }
    ) {
        Text(
            text = cameraModesItem.name,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (cameraModesItem.selected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.White
            }
        )
    }
}

@Composable
private fun CameraPreview(
    cameraState: CameraState,
    lens: Int,
    @TorchState.State torchState: Int,
    quality: Quality,
) {
    val captureManager = LocalVideoCaptureManager.current
    BoxWithConstraints {
        AndroidView(
            factory = {
                captureManager.showPreview(
                    PreviewVideoState(
                        cameraState = cameraState,
                        torchState = torchState,
                        cameraLens = lens
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                when (cameraState) {
                    CameraState.NOT_READY -> {}
                    CameraState.READY -> {
                        captureManager.updatePreview(
                            PreviewVideoState(
                                cameraState = cameraState,
                                torchState = torchState,
                                quality = quality,
                                cameraLens = lens
                            ),
                            it
                        )
                    }
                    CameraState.CHANGED -> {
                        captureManager.videoStateChanged()
                    }
                }
            }
        )
    }
}