package com.armutyus.cameraxproject

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.armutyus.cameraxproject.ui.gallery.GalleryScreen
import com.armutyus.cameraxproject.ui.gallery.GalleryViewModel
import com.armutyus.cameraxproject.ui.gallery.preview.PreviewScreen
import com.armutyus.cameraxproject.ui.gallery.preview.PreviewViewModel
import com.armutyus.cameraxproject.ui.photo.PhotoScreen
import com.armutyus.cameraxproject.ui.photo.PhotoViewModel
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme
import com.armutyus.cameraxproject.ui.video.VideoScreen
import com.armutyus.cameraxproject.ui.video.VideoViewModel
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Permissions
import com.armutyus.cameraxproject.util.Util.Companion.GALLERY_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.SETTINGS_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_ROUTE
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    private val fileManager = FileManager(this)

    @UnstableApi
    @OptIn(ExperimentalAnimationApi::class)
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraXProjectTheme {
                val navController = rememberAnimatedNavController()

                @Suppress("UNCHECKED_CAST")
                val viewModelFactory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(PhotoViewModel::class.java))
                            return PhotoViewModel(fileManager, navController) as T
                        if (modelClass.isAssignableFrom(PreviewViewModel::class.java))
                            return PreviewViewModel(navController) as T
                        if (modelClass.isAssignableFrom(VideoViewModel::class.java))
                            return VideoViewModel(fileManager, navController) as T
                        if (modelClass.isAssignableFrom(GalleryViewModel::class.java))
                            return GalleryViewModel(fileManager, navController) as T
                        throw IllegalArgumentException(getString(R.string.unknown_viewmodel))
                    }
                }
                Permissions(permissionGrantedContent = {
                    AnimatedNavHost(
                        navController = navController,
                        startDestination = GALLERY_ROUTE
                    ) {
                        composable(
                            GALLERY_ROUTE,
                            enterTransition = {
                                fadeIn(tween(700))
                            },
                            exitTransition = {
                                fadeOut(tween(700))
                            },
                            popEnterTransition = {
                                fadeIn(tween(700))
                            },
                            popExitTransition = {
                                fadeOut(tween(700))
                            }
                        ) {
                            GalleryScreen(
                                factory = viewModelFactory,
                            )
                        }
                        composable(
                            PHOTO_ROUTE,
                            enterTransition = {
                                fadeIn(tween(700))
                            },
                            exitTransition = {
                                fadeOut(tween(700))
                            },
                            popEnterTransition = {
                                fadeIn(tween(700))
                            },
                            popExitTransition = {
                                fadeOut(tween(700))
                            }
                        ) {
                            PhotoScreen(
                                factory = viewModelFactory
                            ) {
                                showMessage(this@MainActivity, it)
                            }
                        }
                        composable(
                            VIDEO_ROUTE,
                            enterTransition = {
                                fadeIn(tween(700))
                            },
                            exitTransition = {
                                fadeOut(tween(700))
                            },
                            popEnterTransition = {
                                fadeIn(tween(700))
                            },
                            popExitTransition = {
                                fadeOut(tween(700))
                            }
                        ) {
                            VideoScreen(
                                factory = viewModelFactory
                            ) {
                                showMessage(this@MainActivity, it)
                            }
                        }
                        composable(
                            route = "preview_screen/?filePath={filePath}",
                            arguments = listOf(
                                navArgument("filePath") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            ),
                            enterTransition = {
                                fadeIn(tween(700))
                            },
                            exitTransition = {
                                fadeOut(tween(700))
                            },
                            popEnterTransition = {
                                fadeIn(tween(700))
                            },
                            popExitTransition = {
                                fadeOut(tween(700))
                            }
                        ) {
                            val filePath = remember { it.arguments?.getString("filePath") }
                            PreviewScreen(
                                filePath = filePath ?: "",
                                factory = viewModelFactory
                            )
                        }
                        composable(SETTINGS_ROUTE) {
                            //SettingsScreen(navController = navController)
                        }
                    }
                })
            }
        }
    }
}

private fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}