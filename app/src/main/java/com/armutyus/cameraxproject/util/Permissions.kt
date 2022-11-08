package com.armutyus.cameraxproject.util

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.armutyus.cameraxproject.R
import com.google.accompanist.permissions.*

@RequiresApi(Build.VERSION_CODES.Q)
@ExperimentalPermissionsApi
@Composable
fun Permissions(
    permissions: List<String> = listOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.ACCESS_MEDIA_LOCATION,
        android.Manifest.permission.RECORD_AUDIO
    ),
    permissionGrantedContent: @Composable () -> Unit = { }
) {
    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)
    RequestPermissions(
        multiplePermissionsState = permissionsState,
        permissionGrantedContent = permissionGrantedContent
    )
}

@ExperimentalPermissionsApi
@Composable
private fun RequestPermissions(
    multiplePermissionsState: MultiplePermissionsState,
    permissionGrantedContent: @Composable (() -> Unit)
) {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    if (multiplePermissionsState.allPermissionsGranted) {
        // If all permissions are granted, then show screen with the feature enabled
        permissionGrantedContent()
    } else {
        if (multiplePermissionsState.shouldShowRationale) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter), action = {
                    Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                        Text(text = stringResource(id = R.string.give_permissions))
                    }
                }) {
                    Text(text = stringResource(id = R.string.permissions_required))
                }
            }
        } else {
            AlertDialog(onDismissRequest = { /*TODO*/ },
                title = { Text(text = stringResource(id = R.string.permissions)) },
                text = { Text(text = stringResource(id = R.string.permissions_important)) },
                confirmButton = {
                    Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                        Text(text = stringResource(id = R.string.give_permissions))
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        activity.finish()
                        Toast.makeText(context, R.string.permissions_needed, Toast.LENGTH_SHORT)
                            .show()
                    }
                    ) {
                        Text(text = stringResource(id = R.string.deny))
                    }
                }
            )
        }
    }
}