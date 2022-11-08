package com.armutyus.cameraxproject.ui.gallery

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(PHOTO_ROUTE) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.open_camera)
                )
            }
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {
                    Toast.makeText(context, R.string.search, Toast.LENGTH_LONG).show()
                }) {
                    Icon(
                        imageVector = Icons.Sharp.Search,
                        contentDescription = stringResource(id = R.string.search)
                    )
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            TestText(string = "Gallery Screen")
        }
    }
}

@Composable
fun TestText(string: String) {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = string, fontSize = 24.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    CameraXProjectTheme {
        GalleryScreen(navController = NavController(context))
    }
}