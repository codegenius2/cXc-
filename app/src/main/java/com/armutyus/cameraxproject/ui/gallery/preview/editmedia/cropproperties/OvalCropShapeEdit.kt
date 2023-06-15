package com.armutyus.cameraxproject.ui.gallery.preview.editmedia.cropproperties

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.model.OvalCropShape
import com.smarttoolfactory.cropper.util.drawOutlineWithBlendModeAndChecker


@Composable
internal fun OvalCropShapeEdit(
    aspectRatio: AspectRatio,
    dstBitmap: ImageBitmap,
    title: String,
    ovalCropShape: OvalCropShape,
    onChange: (OvalCropShape) -> Unit
) {

    var newTitle by remember {
        mutableStateOf(title)
    }

    val ovalProperties = remember {
        ovalCropShape.ovalProperties
    }

    var startAngle by remember {
        mutableFloatStateOf(
            ovalProperties.startAngle
        )
    }

    var sweepAngle by remember {
        mutableFloatStateOf(
            ovalProperties.sweepAngle
        )
    }

    var offsetX by remember {
        mutableFloatStateOf(
            ovalProperties.offset.x
        )
    }

    var offsetY by remember {
        mutableFloatStateOf(
            ovalProperties.offset.y
        )
    }

    val shape by remember(startAngle, sweepAngle) {
        derivedStateOf {
            GenericShape { size: Size, _: LayoutDirection ->
                val width = size.width
                val height = size.height
                val diameter = width.coerceAtMost(height)
                val left = (width - diameter) / 2
                val top = (height - diameter) / 2

                val rect = Rect(offset = Offset(left, top), size = Size(diameter, diameter))

                if (sweepAngle == 360f) {
                    addOval(rect)
                } else {
                    moveTo(size.width / 2, size.height / 2)
                    arcTo(rect, startAngle, sweepAngle, false)

                }

                close()
            }
        }
    }

    onChange(
        ovalCropShape.copy(
            ovalProperties = ovalProperties.copy(
                startAngle = startAngle,
                sweepAngle = sweepAngle
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
            value = startAngle,
            title = "Start Angle",
            text = "${startAngle.toInt()}°",
            onValueChange = { startAngle = it },
            valueRange = 0f..360f
        )
        SliderWithValueSelection(
            value = sweepAngle,
            title = "Sweep Angle",
            text = "${sweepAngle.toInt()}°",
            onValueChange = { sweepAngle = it },
            valueRange = 0f..360f
        )

        // TODO Add offset
//        Slider(
//            value = offsetX,
//            onValueChange = { offsetX = it },
//            valueRange = 0f..100f
//        )
//        Slider(
//            value = offsetY,
//            onValueChange = { offsetY = it },
//            valueRange = 0f..100f
//        )

    }
}
