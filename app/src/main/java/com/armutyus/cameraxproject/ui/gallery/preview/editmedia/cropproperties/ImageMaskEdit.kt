package com.armutyus.cameraxproject.ui.gallery.preview.editmedia.cropproperties

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.smarttoolfactory.cropper.model.ImageMaskOutline

@Composable
internal fun ImageMaskEdit(
    imageMaskOutline: ImageMaskOutline,
    onChange: (ImageMaskOutline) -> Unit
) {

    var newTitle by remember {
        mutableStateOf(imageMaskOutline.title)
    }

    Column {

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(bitmap = imageMaskOutline.image, contentDescription = "ImageMask")

        }
        CropTextField(
            value = newTitle,
            onValueChange = {
                newTitle = it
                onChange(
                    imageMaskOutline.copy(
                        title = newTitle
                    )
                )

            }
        )
    }
}
