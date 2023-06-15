package com.armutyus.cameraxproject.ui.gallery.preview

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.GalleryViewModel
import com.armutyus.cameraxproject.ui.gallery.models.BottomNavItem
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.EditImageContent
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEvent
import com.armutyus.cameraxproject.ui.gallery.preview.videoplayback.VideoPlaybackContent
import com.armutyus.cameraxproject.util.LockScreenOrientation
import com.armutyus.cameraxproject.util.Util.Companion.ALL_CONTENT
import com.armutyus.cameraxproject.util.Util.Companion.EDIT_CONTENT
import com.armutyus.cameraxproject.util.Util.Companion.FILTER_NAME
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_CONTENT
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_CONTENT
import com.armutyus.cameraxproject.util.toBitmap
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import jp.co.cyberagent.android.gpuimage.GPUImage

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun PreviewScreen(
    contentFilter: String,
    filePath: String,
    factory: ViewModelProvider.Factory,
    previewViewModel: PreviewViewModel = viewModel(factory = factory),
    galleryViewModel: GalleryViewModel = viewModel(factory = factory)
) {
    DisposableEffect(Unit) {
        galleryViewModel.loadMedia()
        onDispose {
            galleryViewModel.loadMedia().cancel()
        }
    }

    LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val context = LocalContext.current
    val media by galleryViewModel.mediaItems.observeAsState(mapOf())
    val state by previewViewModel.previewScreenState.observeAsState()
    var isDeleteTapped by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationState by remember { mutableFloatStateOf(0f) }
    var zoomState by remember { mutableStateOf(false) }


    val initialFile = Uri.parse(filePath).toFile()
    var currentFile by remember { mutableStateOf(initialFile) }
    val fileName = currentFile.nameWithoutExtension
    val takenDate = if (fileName.startsWith("cXc")) {
        fileName.substring(4, 14).replace("-", "/")
    } else {
        fileName.substring(0, 10).replace("-", "/")
    }
    val takenTime = if (fileName.startsWith("cXc")) {
        fileName.substring(15, 20).replace("-", ":")
    } else {
        fileName.substring(11, 16).replace("-", ":")
    }

    val bottomNavItems = listOf(
        BottomNavItem.Share,
        BottomNavItem.EditItem,
        BottomNavItem.Delete
    )

    Scaffold(
        topBar = {
            AnimatedVisibility(
                modifier = Modifier,
                visible = state!!.showBars,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { previewViewModel.onEvent(PreviewScreenEvent.NavigateBack) }) {
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
                visible = state!!.showBars,
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
                                        if (currentFile.extension == "mp4") {
                                            Toast.makeText(
                                                context,
                                                R.string.feature_not_available,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            previewViewModel.onEvent(PreviewScreenEvent.EditTapped)
                                        }
                                    }

                                    BottomNavItem.Delete -> {
                                        isDeleteTapped = true
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
            val groupedMedia = media.values.flatten().filterNot { it.name.startsWith("cXc") }
            val groupedPhotos = media.values.flatten()
                .filter { it.type == MediaItem.Type.PHOTO && it.name.startsWith("2") }
            val groupedVideos = media.values.flatten().filter { it.type == MediaItem.Type.VIDEO }
            val editedMedia = media.values.flatten().filter { it.name.startsWith("cXc") }
            val currentList = when (contentFilter) {
                ALL_CONTENT -> groupedMedia
                PHOTO_CONTENT -> groupedPhotos
                VIDEO_CONTENT -> groupedVideos
                EDIT_CONTENT -> editedMedia
                else -> groupedMedia
            }
            val count = currentList.size
            val initialItem =
                currentList.firstOrNull { mediaItem -> mediaItem.name == initialFile.name }
            val initialItemIndex by remember { mutableIntStateOf(currentList.indexOf(initialItem)) }
            val pagerState = rememberPagerState(initialItemIndex)

            currentFile = currentList[pagerState.currentPage].uri!!.toFile()

            if (isDeleteTapped) {
                AlertDialog(onDismissRequest = { /* */ },
                    title = { Text(text = stringResource(id = R.string.delete)) },
                    text = { Text(text = stringResource(id = R.string.delete_item)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                previewViewModel.onEvent(PreviewScreenEvent.DeleteTapped(currentFile))
                            }
                        ) {
                            Text(text = stringResource(id = R.string.delete))
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            isDeleteTapped = false
                        }
                        ) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                    }
                )
            }

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
                                    if (scale >= 4.5f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else if (scale >= 2f) {
                                        scale = 5f
                                        /*offsetX -= offset.x
                                        offsetY -= offset.y*/
                                        rotationState = 0f
                                        zoomState = false
                                    } else {
                                        scale = 2.5f
                                        /*offsetX -= offset.x
                                        offsetY -= offset.y*/
                                        rotationState = 0f
                                        zoomState = false
                                    }
                                },
                                onTap = {
                                    if (!zoomState && state?.isInEditMode == false) previewViewModel.onEvent(
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
                            if (state?.isInEditMode == true) {
                                zoomState = true
                                val gpuImage = GPUImage(context)

                                var originalImageBitmap by remember(page) {
                                    mutableStateOf<Bitmap?>(
                                        null
                                    )
                                }

                                val hasFilteredImage by previewViewModel.imageHasFilter.observeAsState()
                                val isImageCropped by previewViewModel.isImageCropped.observeAsState()
                                val imageFilters by previewViewModel.imageFilterList.observeAsState()
                                val editedImageBitmap by previewViewModel.editedBitmap.observeAsState()
                                val croppedImageBitmap by previewViewModel.croppedBitmap.observeAsState()

                                LaunchedEffect(page, croppedImageBitmap) {
                                    originalImageBitmap =
                                        currentList[page].uri!!.toBitmap(context)
                                    if (isImageCropped == true) {
                                        previewViewModel.loadImageFilters(croppedImageBitmap)
                                    } else {
                                        previewViewModel.loadImageFilters(originalImageBitmap)
                                    }
                                }

                                originalImageBitmap?.let { bitmap ->
                                    EditImageContent(
                                        originalImageBitmap = bitmap,
                                        croppedImageBitmap = croppedImageBitmap ?: bitmap,
                                        editedImageBitmap = editedImageBitmap ?: bitmap,
                                        editModeName = state?.switchEditMode ?: FILTER_NAME,
                                        imageFilters = imageFilters ?: emptyList(),
                                        gpuImage = gpuImage,
                                        onCropCancelClicked = {
                                            previewViewModel.switchEditMode(
                                                FILTER_NAME
                                            )
                                        },
                                        setCroppedImage = { previewViewModel.setCroppedImage(it) },
                                        onEditModeTapped = { previewViewModel.switchEditMode(it) },
                                        setEditedBitmap = { previewViewModel.setEditedBitmap(it) },
                                        selectedFilter = { previewViewModel.selectedFilter(it) },
                                        isImageCropped = isImageCropped ?: false,
                                        hasFilteredImage = hasFilteredImage ?: false,
                                        cancelEditMode = {
                                            previewViewModel.onEvent(
                                                PreviewScreenEvent.CancelEditTapped
                                            )
                                        },
                                        onSaveTapped = {
                                            previewViewModel.onEvent(
                                                PreviewScreenEvent.SaveTapped(
                                                    context
                                                )
                                            )
                                        }
                                    )
                                }
                            } else {
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
                        }

                        MediaItem.Type.VIDEO -> {
                            VideoPlaybackContent(
                                currentList[page].uri,
                                state!!.isFullScreen,
                                state!!.showMediaController,
                                {
                                    previewViewModel.onEvent(
                                        PreviewScreenEvent.FullScreenToggleTapped(
                                            state!!.isFullScreen
                                        )
                                    )
                                },
                                { previewViewModel.onEvent(PreviewScreenEvent.HideController(it)) },
                                { previewViewModel.onEvent(PreviewScreenEvent.PlayerViewTapped) },
                                { previewViewModel.onEvent(PreviewScreenEvent.NavigateBack) }
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}