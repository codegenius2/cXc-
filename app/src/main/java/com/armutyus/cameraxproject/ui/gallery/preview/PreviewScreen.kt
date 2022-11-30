package com.armutyus.cameraxproject.ui.gallery.preview

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.GalleryViewModel
import com.armutyus.cameraxproject.ui.gallery.models.BottomNavItem
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEffect
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEvent
import com.armutyus.cameraxproject.util.Util.Companion.GENERAL_ERROR_MESSAGE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_FORWARD_5
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_REPLAY_5
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun PreviewScreen(
    filePath: String,
    navController: NavController,
    factory: ViewModelProvider.Factory,
    previewViewModel: PreviewViewModel = viewModel(factory = factory),
    galleryViewModel: GalleryViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {
    val context = LocalContext.current
    val media by galleryViewModel.mediaItems.collectAsState()
    val state by previewViewModel.previewScreenState.collectAsState()
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var rotationState by remember { mutableStateOf(0f) }
    var zoomState by remember { mutableStateOf(false) }

    val currentFile = Uri.parse(filePath).toFile()
    val fileName = currentFile.nameWithoutExtension
    val takenDate = fileName.substring(0, 10).replace("-", "/")
    val takenTime = fileName.substring(11, 16).replace("-", ":")

    val bottomNavItems = listOf(
        BottomNavItem.Share,
        BottomNavItem.EditItem,
        BottomNavItem.Delete
    )

    LaunchedEffect(previewViewModel) {
        previewViewModel.previewEffect.collect {
            when (it) {
                is PreviewScreenEffect.NavigateTo -> {
                    navController.navigate(it.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
                is PreviewScreenEffect.ShowMessage -> onShowMessage(it.message)
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                modifier = Modifier,
                visible = state.showBars,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Sharp.ArrowBack,
                                contentDescription = stringResource(id = R.string.cancel)
                            )
                        }
                    },
                    title = {
                        Column(
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(text = takenDate, fontSize = 18.sp)
                            Text(text = takenTime, fontSize = 14.sp)
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                modifier = Modifier,
                visible = state.showBars,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NavigationBar {
                    bottomNavItems.forEach { bottomNavItem ->
                        NavigationBarItem(
                            selected = false,
                            icon = {
                                Icon(
                                    imageVector = bottomNavItem.icon,
                                    contentDescription = stringResource(id = bottomNavItem.label)
                                )
                            },
                            label = {
                                Text(text = stringResource(id = bottomNavItem.label))
                            },
                            alwaysShowLabel = true,
                            onClick = {
                                when (bottomNavItem) {
                                    BottomNavItem.Share -> {
                                        previewViewModel.onEvent(
                                            PreviewScreenEvent.ShareTapped(
                                                context,
                                                currentFile
                                            )
                                        )
                                    }
                                    BottomNavItem.EditItem -> {
                                        previewViewModel.onEvent(PreviewScreenEvent.EditTapped)
                                    }
                                    BottomNavItem.Delete -> {
                                        previewViewModel.onEvent(
                                            PreviewScreenEvent.DeleteTapped(
                                                currentFile
                                            )
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val currentList = media.values.flatten()
            val count = currentList.size
            val initialItem =
                currentList.firstOrNull { mediaItem -> mediaItem.name == currentFile.name }
            val initialItemIndex by remember { mutableStateOf(currentList.indexOf(initialItem)) }
            val pagerState = rememberPagerState(initialItemIndex)

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                count = count,
                state = pagerState,
                userScrollEnabled = !zoomState,
                itemSpacing = 16.dp
            ) { page ->
                Box(
                    modifier = Modifier
                        .clip(RectangleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    offset.getDistance()
                                    if (scale >= 2f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 3f
                                        offsetX -= offset.x
                                        offsetY -= offset.y
                                        rotationState = 0f
                                        zoomState = false
                                        println("x: ${offset.x}")
                                        println("y: ${offset.y}")
                                    }
                                },
                                onTap = {
                                    if (!zoomState) previewViewModel.onEvent(
                                        PreviewScreenEvent.ChangeBarState(zoomState)
                                    )
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                do {
                                    val event = awaitPointerEvent()
                                    scale *= event.calculateZoom()
                                    if (scale > 1) {
                                        val offset = event.calculatePan()
                                        offsetX += offset.x
                                        offsetY += offset.y
                                        rotationState += event.calculateRotation()
                                        zoomState = true
                                        previewViewModel.onEvent(
                                            PreviewScreenEvent.ChangeBarState(
                                                zoomState
                                            )
                                        )
                                    } else {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                        rotationState = 0f
                                        zoomState = false
                                    }
                                } while (event.changes.any { pointerInputChange -> pointerInputChange.pressed })
                            }
                        }
                ) {
                    when (currentList[page].type) {
                        MediaItem.Type.PHOTO -> {
                            SubcomposeAsyncImage(
                                model = currentList[page].uri,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .graphicsLayer(
                                        scaleX = maxOf(1f, minOf(3f, scale)),
                                        scaleY = maxOf(1f, minOf(3f, scale)),
                                        rotationZ = rotationState,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    ),
                                filterQuality = FilterQuality.High,
                                contentDescription = ""
                            ) {
                                val painterState = painter.state
                                if (painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Error) {
                                    LinearProgressIndicator()
                                } else {
                                    SubcomposeAsyncImageContent()
                                }
                            }
                        }
                        MediaItem.Type.VIDEO -> {
                            VideoPlaybackContent(
                                currentList[page].uri,
                                state.isFullScreen,
                                state.showMediaController,
                                {
                                    previewViewModel.onEvent(
                                        PreviewScreenEvent.FullScreenToggleTapped(
                                            state.isFullScreen
                                        )
                                    )
                                },
                                { previewViewModel.onEvent(PreviewScreenEvent.HideController(it)) },
                                { previewViewModel.onEvent(PreviewScreenEvent.PlayerViewTapped) },
                                { navController.popBackStack() }
                            )
                        }
                        else -> onShowMessage(GENERAL_ERROR_MESSAGE)
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun VideoPlaybackContent(
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
            .setSeekBackIncrementMs(VIDEO_REPLAY_5)
            .setSeekForwardIncrementMs(VIDEO_FORWARD_5)
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