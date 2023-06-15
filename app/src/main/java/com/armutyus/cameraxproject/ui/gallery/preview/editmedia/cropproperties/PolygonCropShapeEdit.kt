package com.armutyus.cameraxproject.ui.gallery.preview.editmedia.cropproperties

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.model.PolygonCropShape
import com.smarttoolfactory.cropper.util.createPolygonShape
import com.smarttoolfactory.cropper.util.drawOutlineWithBlendModeAndChecker

@Composable
internal fun PolygonCropShapeEdit(
    aspectRatio: AspectRatio,
    dstBitmap: ImageBitmap,
    title: String,
    polygonCropShape: PolygonCropShape,
    onChange: (PolygonCropShape) -> Unit
) {

    var newTitle by remember {
        mutableStateOf(title)
    }

    val polygonProperties = remember {
        polygonCropShape.polygonProperties
    }

    var sides by remember {
        mutableIntStateOf(
            polygonProperties.sides
        )
    }

    var angle by remember {
        mutableFloatStateOf(
            polygonProperties.angle
        )
    }

    var shape by remember {
        mutableStateOf(
            polygonCropShape.shape
        )
    }

    onChange(
        polygonCropShape.copy(
            polygonProperties = polygonProperties.copy(
                sides = sides,
                angle = angle
            ),
            title = newTitle,
            shape = shape
        )
    )

    Column {

        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4 / 3f)
                .clipToBounds()
                .drawOutlineWithBlendModeAndChecker(
                    aspectRatio,
                    shape,
                    density,
                    dstBitmap
                )
        )

        CropTextField(
            value = newTitle,
            onValueChange = { newTitle = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SliderWithValueSelection(
            value = sides.toFloat(),
            title = "Sides",
            text = "$sides",
            onValueChange = {
                sides = it.toInt()
                shape = createPolygonShape(sides = sides, angle)
            },
            valueRange = 3f..15f
        )

        SliderWithValueSelection(
            value = angle,
            title = "Angle",
            text = "${angle.toInt()}°",
            onValueChange = {
                angle = it
                shape = createPolygonShape(sides = sides, degrees = angle)
            },
            valueRange = 0f..360f
        )
    }
}
