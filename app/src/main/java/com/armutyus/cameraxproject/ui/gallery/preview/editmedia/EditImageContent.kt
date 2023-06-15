package com.armutyus.cameraxproject.ui.gallery.preview.editmedia

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.models.EditModesItem
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.models.ImageFilter
import com.armutyus.cameraxproject.util.Util.Companion.CROP_MODE
import com.armutyus.cameraxproject.util.Util.Companion.CROP_NAME
import com.armutyus.cameraxproject.util.Util.Companion.FILTER_MODE
import com.armutyus.cameraxproject.util.Util.Companion.FILTER_NAME
import jp.co.cyberagent.android.gpuimage.GPUImage

@Composable
fun EditImageContent(
    originalImageBitmap: Bitmap,
    croppedImageBitmap: Bitmap,
    editedImageBitmap: Bitmap,
    editModeName: String,
    imageFilters: List<ImageFilter>,
    gpuImage: GPUImage,
    onCropCancelClicked: () -> Unit,
    setCroppedImage: (Bitmap?) -> Unit,
    onEditModeTapped: (String) -> Unit,
    setEditedBitmap: (Bitmap) -> Unit,
    selectedFilter: (String) -> Unit,
    isImageCropped: Boolean,
    hasFilteredImage: Boolean,
    cancelEditMode: () -> Unit,
    onSaveTapped: () -> Unit
) {

    if (isImageCropped) {
        gpuImage.setImage(croppedImageBitmap)
    } else {
        gpuImage.setImage(originalImageBitmap)
    }

    var isBackTapped by remember { mutableStateOf(false) }

    BackHandler {
        if (hasFilteredImage) {
            isBackTapped = true
        } else {
            cancelEditMode()
        }
    }

    if (isBackTapped) {
        AlertDialog(
            onDismissRequest = { /* */ },
            text = { Text(text = stringResource(id = R.string.confirm_changes)) },
            confirmButton = {
                Button(onClick = {
                    onSaveTapped()
                    cancelEditMode()
                    isBackTapped = false
                }) {
                    Text(text = stringResource(id = R.string.save_changes))
                }
            },
            dismissButton = {
                Button(onClick = {
                    cancelEditMode()
                    isBackTapped = false
                }
                ) {
                    Text(text = stringResource(id = R.string.deny))
                }
            }
        )
    }

    when (editModeName) {
        FILTER_NAME -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                EditMediaMidContent(
                    imageBitmap = if (isImageCropped && !hasFilteredImage) croppedImageBitmap else editedImageBitmap
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    EditMediaTopContent(
                        navigateBack = {
                            if (hasFilteredImage) {
                                isBackTapped = true
                            } else {
                                cancelEditMode()
                            }
                        },
                        onSaveTapped = onSaveTapped
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    EditMediaBottomContent(
                        imageFilters = imageFilters,
                        editModeName = editModeName,
                        gpuImage = gpuImage,
                        onEditModeTapped = { onEditModeTapped(it) },
                        setFilteredBitmap = { setEditedBitmap(it) }
                    ) { selectedFilter(it) }
                }
            }
        }

        CROP_NAME -> {
            ImageCropMode(
                editedImageBitmap = editedImageBitmap.asImageBitmap(),
                isImageCropped = isImageCropped,
                editModeName = editModeName,
                onEditModeTapped = { onEditModeTapped(it) },
                setCroppedImage = { setCroppedImage(it) },
                setEditedBitmap = { setEditedBitmap(it) },
                cancelEditMode = cancelEditMode,
                onSaveTapped = onSaveTapped,
                onCropCancelClicked = onCropCancelClicked
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMediaTopContent(
    navigateBack: () -> Unit,
    onSaveTapped: () -> Unit
) {
    AnimatedVisibility(
        modifier = Modifier,
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            navigationIcon = {
                IconButton(onClick = { navigateBack() }) {
                    Icon(
                        imageVector = Icons.Sharp.ArrowBack,
                        contentDescription = stringResource(id = R.string.cancel)
                    )
                }
            },
            title = { Text(text = stringResource(id = R.string.edit), fontSize = 18.sp) },
            actions = {
                TextButton(onClick = { onSaveTapped() }) {
                    Text(text = stringResource(id = R.string.save))
                }
            }
        )
    }
}

@Composable
private fun EditMediaMidContent(
    imageBitmap: Bitmap
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clip(RectangleShape)
    ) {
        SubcomposeAsyncImage(
            model = imageBitmap,
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            contentDescription = ""
        ) {
            val painterState = painter.state
            if (painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Error) {
                CircularProgressIndicator(Modifier.size(32.dp))
            } else {
                SubcomposeAsyncImageContent()
            }
        }
    }
}

@Composable
private fun EditMediaBottomContent(
    imageFilters: List<ImageFilter>,
    editModeName: String,
    gpuImage: GPUImage,
    onEditModeTapped: (String) -> Unit,
    setFilteredBitmap: (Bitmap) -> Unit,
    selectedFilter: (String) -> Unit
) {

    val editModesList = listOf(
        EditModesItem(FILTER_MODE, FILTER_NAME, editModeName == FILTER_NAME),
        EditModesItem(CROP_MODE, CROP_NAME, editModeName == CROP_NAME)
    )
    val scrollState = rememberScrollState()
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(contentPadding = PaddingValues(8.dp)) {
            items(editModesList) { editMode ->
                EditModesRow(editModesItem = editMode) {
                    onEditModeTapped(it)
                }
            }
        }

        LazyRow(state = listState) {
            items(imageFilters) { imageFilter ->
                ImageFilterMode(
                    image = imageFilter.filterPreview,
                    filterName = imageFilter.name
                ) {
                    with(imageFilter) {
                        gpuImage.setFilter(filter)
                        setFilteredBitmap(gpuImage.bitmapWithFilterApplied)
                        selectedFilter(imageFilter.name)
                    }
                }
            }
        }
    }
}

@Composable
fun EditModesRow(
    editModesItem: EditModesItem,
    onEditModeTapped: (String) -> Unit
) {
    TextButton(
        onClick = {
            if (!editModesItem.selected) {
                onEditModeTapped(editModesItem.name)
            }
        }
    ) {
        Text(
            text = editModesItem.name,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (editModesItem.selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            }
        )
    }
}

@Composable
private fun ImageFilterMode(
    image: Bitmap,
    filterName: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(90.dp)
            .height(90.dp)
            .background(Color.Transparent)
            .padding(6.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = true) {
                onClick.invoke()
            }
    ) {

        SubcomposeAsyncImage(
            model = image,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
            contentDescription = ""
        ) {
            val painterState = painter.state
            if (painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Error) {
                CircularProgressIndicator(Modifier.requiredSize(8.dp))
            } else {
                SubcomposeAsyncImageContent()
            }
        }
        Text(
            text = filterName,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .align(Alignment.BottomCenter),
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}