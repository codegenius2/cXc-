package com.armutyus.cameraxproject.ui.video

import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.camera.core.CameraInfo
import androidx.camera.core.TorchState
import androidx.camera.extensions.ExtensionMode
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.photo.models.CameraModesItem
import com.armutyus.cameraxproject.ui.photo.models.CameraState
import com.armutyus.cameraxproject.ui.video.models.PreviewVideoState
import com.armutyus.cameraxproject.ui.video.models.RecordingStatus
import com.armutyus.cameraxproject.ui.video.models.VideoEffect
import com.armutyus.cameraxproject.ui.video.models.VideoEvent
import com.armutyus.cameraxproject.util.*
import com.armutyus.cameraxproject.util.Util.Companion.DELAY_10S
import com.armutyus.cameraxproject.util.Util.Companion.DELAY_3S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_10S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_3S
import com.armutyus.cameraxproject.util.Util.Companion.TIMER_OFF
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_MODE
import java.io.File

@Composable
fun VideoScreen(
    navController: NavController,
    factory: ViewModelProvider.Factory,
    videoViewModel: VideoViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    val state by videoViewModel.videoState.collectAsState()
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

    DisposableEffect(key1 = "key2") {
        orientationEventListener.enable()
        onDispose {
            orientationEventListener.disable()
        }
    }

    LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val listener = remember(videoViewModel) {
        object : VideoCaptureManager.Listener {
            override fun onInitialised(
                cameraLensInfo: HashMap<Int, CameraInfo>,
                supportedQualities: List<Quality>
            ) {
                videoViewModel.onEvent(
                    VideoEvent.CameraInitialized(
                        cameraLensInfo,
                        supportedQualities
                    )
                )
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
                videoViewModel.onEvent(VideoEvent.Error(throwable))
            }
        }
    }

    val videoCaptureManager = remember(videoViewModel) {
        VideoCaptureManager.Builder(context)
            .registerLifecycleOwner(lifecycleOwner)
            .create()
            .apply { this.listener = listener }
    }

    val mediaDir = context.getExternalFilesDir("cameraXproject")?.let {
        File(it, VIDEO_DIR).apply { mkdirs() }
    }

    val latestCapturedVideo = state.latestVideoUri ?: mediaDir?.listFiles()?.lastOrNull()?.toUri()

    CompositionLocalProvider(LocalVideoCaptureManager provides videoCaptureManager) {
        VideoScreenContent(
            availableExtensions = listOf(ExtensionMode.NONE, VIDEO_MODE),
            extensionMode = state.extensionMode,
            cameraLens = state.lens,
            cameraState = state.cameraState,
            delayTimer = state.delayTimer,
            torchState = state.torchState,
            hasFlashUnit = state.lensInfo[state.lens]?.hasFlashUnit() ?: false,
            hasDualCamera = state.lensInfo.size > 1,
            videoUri = latestCapturedVideo,
            quality = state.quality,
            recordedLength = state.recordedLength,
            recordingStatus = state.recordingStatus,
            rotation = rotation,
            view = view,
            onEvent = videoViewModel::onEvent
        )
    }

    LaunchedEffect(videoViewModel) {
        videoViewModel.videoEffect.collect {
            when (it) {
                is VideoEffect.NavigateTo -> {
                    navController.navigate(it.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                is VideoEffect.ShowMessage -> onShowMessage(it.message)
                is VideoEffect.RecordVideo -> videoCaptureManager.startRecording(it.filePath)
                VideoEffect.PauseRecording -> videoCaptureManager.pauseRecording()
                VideoEffect.ResumeRecording -> videoCaptureManager.resumeRecording()
                VideoEffect.StopRecording -> videoCaptureManager.stopRecording()
            }
        }
    }
}

@Composable
private fun VideoScreenContent(
    availableExtensions: List<Int>,
    extensionMode: Int,
    cameraLens: Int?,
    cameraState: CameraState,
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
                        onFlashTapped = { onEvent(VideoEvent.FlashTapped) },
                        onQualitySelectorTapped = { onEvent(VideoEvent.SetVideoQuality) }
                    ) {
                        onEvent(VideoEvent.SettingsTapped)
                    }
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
                    availableExtensions = availableExtensions,
                    videoUri = videoUri,
                    extensionMode = extensionMode,
                    recordingStatus = recordingStatus,
                    showFlipIcon = hasDualCamera,
                    rotation = rotation,
                    view = view,
                    onThumbnailTapped = { onEvent(VideoEvent.ThumbnailTapped) },
                    onRecordTapped = {
                        when (delayTimer) {
                            TIMER_OFF -> onEvent(VideoEvent.RecordTapped(0L))
                            TIMER_3S -> onEvent(VideoEvent.RecordTapped(DELAY_3S))
                            TIMER_10S -> onEvent(VideoEvent.RecordTapped(DELAY_10S))
                        }
                    },
                    onStopTapped = { onEvent(VideoEvent.StopTapped) },
                    onPauseTapped = { onEvent(VideoEvent.PauseTapped) },
                    onResumeTapped = { onEvent(VideoEvent.ResumeTapped) },
                    onCameraModeTapped = { extension ->
                        onEvent(
                            VideoEvent.SelectCameraExtension(
                                extension
                            )
                        )
                    },
                    onFlipTapped = { onEvent(VideoEvent.FlipTapped) }
                )
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
    onQualitySelectorTapped: () -> Unit,
    onSettingsTapped: () -> Unit
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
            SettingsIcon(rotation = rotation, onTapped = onSettingsTapped)
        }
    }
}


@Composable
internal fun VideoBottomControls(
    modifier: Modifier = Modifier,
    availableExtensions: List<Int>,
    extensionMode: Int,
    recordingStatus: RecordingStatus,
    showFlipIcon: Boolean,
    rotation: Int,
    videoUri: Uri?,
    view: View,
    onCameraModeTapped: (Int) -> Unit,
    onRecordTapped: () -> Unit,
    onStopTapped: () -> Unit,
    onPauseTapped: () -> Unit,
    onResumeTapped: () -> Unit,
    onFlipTapped: () -> Unit,
    onThumbnailTapped: () -> Unit
) {
    val cameraModes = mapOf(
        ExtensionMode.NONE to R.string.camera_mode_none,
        VIDEO_MODE to R.string.camera_mode_video
    )

    val cameraModesList = availableExtensions.map {
        CameraModesItem(
            it,
            stringResource(id = cameraModes[it]!!),
            extensionMode == it
        )
    }

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
                    onCameraModeTapped(it)
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
    onCameraModeTapped: (Int) -> Unit
) {
    TextButton(
        onClick = {
            if (!cameraModesItem.selected) {
                onCameraModeTapped(cameraModesItem.cameraMode)
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