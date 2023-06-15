package com.armutyus.cameraxproject.ui.gallery.preview.editmedia.cropproperties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttoolfactory.cropper.settings.CropType

@Composable
internal fun CropTypeDialogSelection(
    cropType: CropType,
    onCropTypeChange: (CropType) -> Unit
) {

    val cropTypeOptions =
        remember { listOf(CropType.Dynamic.toString(), CropType.Static.toString()) }

    var showDialog by remember { mutableStateOf(false) }

    val index = when (cropType) {
        CropType.Dynamic -> 0
        else -> 1
    }

    Text(
        text = cropTypeOptions[index],
        fontSize = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showDialog = true
            }
            .padding(8.dp)

    )

    if (showDialog) {
        DialogWithMultipleSelection(
            title = "Crop Type",
            options = cropTypeOptions,
            value = index,
            onDismiss = { showDialog = false },
            onConfirm = {

                val cropTypeChange = when (it) {
                    0 -> CropType.Dynamic
                    else -> CropType.Static
                }
                onCropTypeChange(cropTypeChange)
                showDialog = false
            }
        )
    }

}