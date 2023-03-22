package com.armutyus.cameraxproject.ui.gallery.preview.editmedia

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.cropproperties.CropStyleSelectionMenu
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.cropproperties.PropertySelectionSheet
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.models.EditModesItem
import com.armutyus.cameraxproject.util.Util.Companion.CROP_MODE
import com.armutyus.cameraxproject.util.Util.Companion.CROP_NAME
import com.armutyus.cameraxproject.util.Util.Companion.FILTER_MODE
import com.armutyus.cameraxproject.util.Util.Companion.FILTER_NAME
import com.smarttoolfactory.colorpicker.widget.drawChecker
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.*
import kotlinx.coroutines.launch

internal enum class SelectionPage {
    Properties, Style
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImageCropMode(
    editedImageBitmap: ImageBitmap,
    isImageCropped: Boolean,
    editModeName: String,
    onEditModeTapped: (String) -> Unit,
    setCroppedImage: (Bitmap?) -> Unit,
    setEditedBitmap: (Bitmap) -> Unit,
    cancelEditMode: () -> Unit,
    onSaveTapped: () -> Unit,
    onCropCancelClicked: () -> Unit
) {

    var isBackTapped by remember { mutableStateOf(false) }

    BackHandler {
        if (isImageCropped) {
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

    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val handleSize: Float = LocalDensity.current.run { 20.dp.toPx() }

    val defaultImage1 = ImageBitmap.imageResource(id = R.drawable.squircle)
    val defaultImage2 = ImageBitmap.imageResource(id = R.drawable.cloud)
    val defaultImage3 = ImageBitmap.imageResource(id = R.drawable.sun)
    val cropFrameFactory = remember {
        CropFrameFactory(
            listOf(
                defaultImage1,
                defaultImage2,
                defaultImage3
            )
        )
    }
    var cropProperties by remember(CropDefaults) {
        mutableStateOf(
            CropDefaults.properties(
                cropOutlineProperty = CropOutlineProperty(
                    OutlineType.Rect,
                    RectCropShape(0, "Rect")
                ),
                handleSize = handleSize
            )
        )
    }
    var cropStyle by remember { mutableStateOf(CropDefaults.style()) }
    var selectionPage by remember { mutableStateOf(SelectionPage.Properties) }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetElevation = 16.dp,
        sheetShape = RoundedCornerShape(
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
            topStart = 28.dp,
            topEnd = 28.dp
        ),
        sheetContent = {
            if (selectionPage == SelectionPage.Properties) {
                PropertySelectionSheet(
                    cropFrameFactory = cropFrameFactory,
                    cropProperties = cropProperties,
                    onCropPropertiesChange = {
                        cropProperties = it
                    }
                )
            } else {
                CropStyleSelectionMenu(
                    cropType = cropProperties.cropType,
                    cropStyle = cropStyle,
                    onCropStyleChange = {
                        cropStyle = it
                    }
                )
            }
        },
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.TopCenter)
            ) {
                EditMediaTopContent(
                    navigateBack = {
                        if (isImageCropped) {
                            isBackTapped = true
                        } else {
                            cancelEditMode()
                        }
                    },
                    onSaveTapped = onSaveTapped
                )
                MainContent(
                    cropProperties = cropProperties,
                    cropStyle = cropStyle,
                    originalImageBitmap = editedImageBitmap,
                    editModeName = editModeName,
                    onEditModeTapped = { onEditModeTapped(it) },
                    setEditedBitmap = { setEditedBitmap(it) },
                    setCroppedImage = { setCroppedImage(it) },
                    onCropCancelClicked = onCropCancelClicked,
                ) {
                    selectionPage = it

                    coroutineScope.launch {
                        if (bottomSheetState.isVisible) {
                            bottomSheetState.hide()
                        } else {
                            bottomSheetState.show()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    cropProperties: CropProperties,
    cropStyle: CropStyle,
    originalImageBitmap: ImageBitmap,
    editModeName: String,
    onEditModeTapped: (String) -> Unit,
    setEditedBitmap: (Bitmap) -> Unit,
    setCroppedImage: (Bitmap?) -> Unit,
    onCropCancelClicked: () -> Unit,
    onSelectionPageMenuClicked: (SelectionPage) -> Unit
) {

    val imageBitmap by remember { mutableStateOf(originalImageBitmap) }
    var croppedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    setCroppedImage(croppedImage?.asAndroidBitmap())

    var crop by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var isCropping by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            ImageCropper(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.85f),
                imageBitmap = croppedImage ?: imageBitmap,
                contentDescription = "Image Cropper",
                cropProperties = cropProperties,
                cropStyle = cropStyle,
                crop = crop,
                onCropStart = {
                    isCropping = true
                }
            ) {
                croppedImage = it
                isCropping = false
                crop = false
                showDialog = true
            }

            val editModesList = listOf(
                EditModesItem(FILTER_MODE, FILTER_NAME, editModeName == FILTER_NAME),
                EditModesItem(CROP_MODE, CROP_NAME, editModeName == CROP_NAME)
            )
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .weight(0.15f),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = {
                            setCroppedImage(null)
                            onCropCancelClicked()
                        }
                    ) {
                        Icon(
                            Icons.Sharp.Close,
                            contentDescription = "Cancel Crop"
                        )
                    }
                    IconButton(
                        onClick = {
                            onSelectionPageMenuClicked(SelectionPage.Properties)
                        }
                    ) {
                        Icon(
                            Icons.Sharp.Settings,
                            contentDescription = "Settings",
                        )
                    }
                    IconButton(
                        onClick = {
                            onSelectionPageMenuClicked(SelectionPage.Style)
                        }
                    ) {
                        Icon(Icons.Sharp.Brush, contentDescription = "Style")
                    }
                    IconButton(
                        onClick = {
                            crop = true
                        }
                    ) {
                        Icon(
                            Icons.Sharp.Done,
                            contentDescription = "Crop Image"
                        )
                    }
                }
            }
        }

        if (isCropping) {
            CircularProgressIndicator()
        }
    }

    if (showDialog) {
        croppedImage?.let { croppedBitmap ->
            ShowCroppedImageDialog(
                imageBitmap = croppedBitmap,
                setEditedBitmap = {
                    setEditedBitmap(croppedBitmap.asAndroidBitmap())
                    showDialog = !showDialog
                }
            ) {
                showDialog = !showDialog
                croppedImage = null
            }
        }
    }
}

@Composable
private fun ShowCroppedImageDialog(
    imageBitmap: ImageBitmap,
    setEditedBitmap: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Image(
                modifier = Modifier
                    .drawChecker(RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit,
                bitmap = imageBitmap,
                contentDescription = "result"
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    setEditedBitmap()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}