package com.armutyus.cameraxproject

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraXProjectTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "gallery_screen"
                ) {
                    composable("gallery_screen") {
                        //GalleryScreen(navController = navController)
                    }
                    composable("camera_screen") {
                        //CameraScreen(navController = navController)
                    }
                    composable("settings_screen") {
                        //SettingsScreen(navController = navController)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting() {
    val context = LocalContext.current
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { Toast.makeText(context, "Clicked", Toast.LENGTH_LONG).show() }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Open Camera")
            }
        },
        bottomBar = {
            BottomAppBar() {
                IconButton(onClick = { Toast.makeText(context, "Search", Toast.LENGTH_LONG).show() }) {
                    Icon(imageVector = Icons.Sharp.Search, contentDescription = "Search")
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            TestText(string = "Hello!")
        }
    }
}

@Composable
fun TestText(string: String) {
    Column(modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.Center, 
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = string, fontSize = 24.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    CameraXProjectTheme {
        Greeting()
    }
}