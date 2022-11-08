package com.armutyus.cameraxproject.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.camera.core.AspectRatio
import androidx.camera.video.Quality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.armutyus.cameraxproject.util.Util.Companion.RATIO_16_9_VALUE
import com.armutyus.cameraxproject.util.Util.Companion.RATIO_4_3_VALUE
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun View.vibrate(feedbackConstant: Int) {
    // Either this needs to be set to true, or android:hapticFeedbackEnabled="true" needs to be set in XML
    isHapticFeedbackEnabled = true
    // Most of the constants are off by default: for example, clicking on a button doesn't cause the phone to vibrate anymore
    // if we still want to access this vibration, we'll have to ignore the global settings on that.
    performHapticFeedback(feedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
}


fun aspectRatio(width: Int, height: Int): Int {
    val previewRatio = max(width, height).toDouble() / min(width, height)
    if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
        return AspectRatio.RATIO_4_3
    }
    return AspectRatio.RATIO_16_9
}

/**
 * a helper function to retrieve the aspect ratio from a QualitySelector enum.
 */
fun Quality.getAspectRatio(quality: Quality): Int {
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

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


