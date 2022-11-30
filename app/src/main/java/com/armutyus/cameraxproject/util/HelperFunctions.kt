package com.armutyus.cameraxproject.util

import androidx.camera.core.AspectRatio
import androidx.camera.video.Quality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun aspectRatio(width: Int, height: Int): Int {
    val previewRatio = max(width, height).toDouble() / min(width, height)
    if (abs(previewRatio - Util.RATIO_4_3_VALUE) <= abs(previewRatio - Util.RATIO_16_9_VALUE)) {
        return AspectRatio.RATIO_4_3
    }
    return AspectRatio.RATIO_16_9
}

/**
 * a helper function to retrieve the aspect ratio from a QualitySelector enum.
 */
fun getAspectRatio(quality: Quality): Int {
    return when {
        arrayOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.HIGHEST)
            .contains(quality) -> AspectRatio.RATIO_16_9
        (quality == Quality.SD) -> AspectRatio.RATIO_4_3
        else -> throw UnsupportedOperationException()
    }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            // restore original orientation when view disappears
            activity.requestedOrientation = originalOrientation
        }
    }
}