package com.armutyus.cameraxproject.ui.gallery.preview.editmedia.cropproperties

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.BuildCompat
import com.google.modernstorage.photopicker.PhotoPicker
import com.smarttoolfactory.cropper.model.*
import com.smarttoolfactory.cropper.util.buildOutline
import com.smarttoolfactory.cropper.util.scaleAndTranslatePath

/**
 * Dialog for displaying selectable crop frames with edit, delete and new frame options
 */
@Composable
fun CropFrameListDialog(
    aspectRatio: AspectRatio,
    cropFrame: CropFrame,
    onDismiss: () -> Unit,
    onConfirm: (CropFrame) -> Unit
) {
    var updatedCropFrame by remember {
        mutableStateOf(cropFrame)
    }

    var selectedIndex by remember {
        mutableStateOf(updatedCropFrame.selectedIndex)
    }

    val outlineType = cropFrame.outlineType

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }


    if (showEditDialog) {
        CropFrameEditDialog(
            aspectRatio = aspectRatio,
            index = selectedIndex,
            cropFrame = updatedCropFrame,
            onConfirm = {
                updatedCropFrame = it
                selectedIndex = updatedCropFrame.selectedIndex
                showEditDialog = false
            },
            onDismiss = {
                showEditDialog = false
            }
        )
    }

    if (showAddDialog) {

        val outline = updatedCropFrame.cropOutlineContainer.selectedItem
        if (outline is CropShape) {
            CropShapeAddDialog(
                aspectRatio = aspectRatio,
                cropFrame = updatedCropFrame.copy(),
                onConfirm = {
                    updatedCropFrame = it
                    selectedIndex = updatedCropFrame.selectedIndex
                    showAddDialog = false
                },
                onDismiss = {
                    showAddDialog = false
                }
            )
        } else if (outline is CropImageMask) {

            PickImageMask {

                val id = updatedCropFrame.outlineCount
                val newOutline =
                    ImageMaskOutline(id = updatedCropFrame.outlineCount, "ImageMask $id", it)

                val newOutlines: List<CropOutline> = cropFrame.outlines
                    .toMutableList()
                    .apply {
                        add(newOutline)
                    }
                    .toList()

                updatedCropFrame = cropFrame.copy(
                    cropOutlineContainer = getOutlineContainer(
                        outlineType = outlineType,
                        index = newOutlines.size - 1,
                        outlines = newOutlines
                    )
                )

                selectedIndex = updatedCropFrame.selectedIndex
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frames",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))

                val enabled = selectedIndex != 0

                Icon(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (enabled) MaterialTheme.colorScheme.error
                            else Color.LightGray
                        )
                        .size(28.dp)
                        .clickable(
                            enabled = enabled,
                            onClick = {
                                val outlines = updatedCropFrame.outlines
                                    .toMutableList()
                                    .apply {
                                        removeAt(selectedIndex)
                                    }
                                updatedCropFrame = updatedCropFrame.copy(
                                    cropOutlineContainer = getOutlineContainer(
                                        updatedCropFrame.outlineType,
                                        outlines.size - 1,
                                        outlines
                                    )
                                )

                                selectedIndex = outlines.size - 1
                            }
                        )
                        .padding(6.dp),

                    imageVector = Icons.Default.Delete,
                    tint = if (enabled) Color.White else Color.Gray,
                    contentDescription = "Delete"
                )

                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .size(28.dp)
                        .clickable {
                            showEditDialog = true
                        }
                        .padding(6.dp),
                    imageVector = Icons.Default.Edit,
                    tint = Color.White,
                    contentDescription = "Edit"
                )
            }
        },
        text = {
            CropOutlineGridList(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
                selectedIndex = selectedIndex,
                outlineType = updatedCropFrame.outlineType,
                outlines = updatedCropFrame.outlines,
                aspectRatio = aspectRatio,
                onItemClick = {
                    selectedIndex = it
                    updatedCropFrame.selectedIndex = selectedIndex
                },
                onAddItemClick = {
                    showAddDialog = true
                }
            )
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {

                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(updatedCropFrame)
                }
            ) {
                Text("Accept")
            }
        }
    )
}

@Composable
private fun CropOutlineGridList(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    outlineType: OutlineType,
    outlines: List<CropOutline>,
    aspectRatio: AspectRatio,
    onItemClick: (Int) -> Unit,
    onAddItemClick: () -> Unit
) {

    LazyVerticalGrid(
        modifier = modifier,
        contentPadding = PaddingValues(2.dp),
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        itemsIndexed(
            items = outlines
        ) { index, cropOutline ->

            val selected = index == selectedIndex
            CropOutlineGridItem(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                cropOutline = cropOutline,
                selected = selected,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                aspectRatio = aspectRatio
            ) {
                onItemClick(index)
            }
        }

        if (outlineType != OutlineType.Custom) {
            item {
                AddItemButton(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    onAddItemClick()
                }
            }
        }
    }
}

@Composable
private fun AddItemButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable {
                onClick()
            }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {

        Icon(
            modifier = Modifier
                .fillMaxWidth(.7f)
                .aspectRatio(1f),
            imageVector = Icons.Default.Add,
            contentDescription = "Add Outline",
            tint = Color.Gray
        )

    }
}

@Composable
private fun CropOutlineGridItem(
    modifier: Modifier = Modifier,
    cropOutline: CropOutline,
    selected: Boolean,
    color: Color,
    aspectRatio: AspectRatio,
    onClick: () -> Unit,
) {

    Box(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(20))
            .border(2.dp, if (selected) color else Color.Unspecified, RoundedCornerShape(20))
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.TopEnd
    ) {

        CropOutlineDisplay(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cropOutline = cropOutline,
            aspectRatio,
            color
        )


        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .7f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cropOutline.title,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 12.sp,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CropOutlineDisplay(
    modifier: Modifier,
    cropOutline: CropOutline,
    aspectRatio: AspectRatio,
    color: Color
) {

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {

        when (cropOutline) {

            is CropShape -> {

                Box(
                    Modifier
                        .matchParentSize()
                        .drawWithCache {
                            val coefficient = .8f

                            val (offset, outline) = buildOutline(
                                aspectRatio,
                                coefficient,
                                cropOutline.shape,
                                size,
                                layoutDirection,
                                density
                            )

                            onDrawWithContent {
                                translate(
                                    left = offset.x,
                                    top = offset.y
                                ) {
                                    drawOutline(
                                        outline = outline,
                                        color = color
                                    )
                                }
                                drawContent()
                            }
                        }
                )
            }

            is CropPath -> {
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(12.dp)
                        .drawWithCache {

                            val path = Path().apply {
                                addPath(cropOutline.path)
                                scaleAndTranslatePath(size.width, size.height)
                            }

                            onDrawWithContent {
                                drawPath(path, color)
                            }
                        }
                )
            }

            is CropImageMask -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(bitmap = cropOutline.image, contentDescription = "ImageMask")
                }
            }
        }
    }
}

@OptIn(BuildCompat.PrereleaseSdkCheck::class)
@Suppress("DEPRECATION")
@Composable
private fun PickImageMask(
    onImageSelected: (ImageBitmap) -> Unit

) {
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(PhotoPicker()) { uris ->
        val uri = uris.firstOrNull() ?: return@rememberLauncherForActivityResult

        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(context.contentResolver, uri)
            ) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        onImageSelected(bitmap.asImageBitmap())
    }

    LaunchedEffect(key1 = Unit) {
        photoPicker.launch(PhotoPicker.Args(PhotoPicker.Type.IMAGES_ONLY, 1))
    }
}
