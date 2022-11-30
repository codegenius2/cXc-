package com.armutyus.cameraxproject.ui.gallery

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.models.*
import com.armutyus.cameraxproject.ui.theme.CameraXProjectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    factory: ViewModelProvider.Factory,
    galleryViewModel: GalleryViewModel = viewModel(factory = factory),
    onShowMessage: (message: String) -> Unit
) {

    val state by galleryViewModel.galleryState.collectAsState()
    val media by galleryViewModel.mediaItems.collectAsState()
    val groupedPhotos =
        media.values.flatten().filter { it.type == MediaItem.Type.PHOTO }.groupBy { it.takenTime }
    val groupedVideos =
        media.values.flatten().filter { it.type == MediaItem.Type.VIDEO }.groupBy { it.takenTime }

    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    var filterContent by remember { mutableStateOf(MediaItem.Filter.ALL) }
    val bottomNavItems = listOf(
        BottomNavItem.Gallery,
        BottomNavItem.Photos,
        BottomNavItem.Videos
    )
    val selectableModeItems = listOf(
        BottomNavItem.Cancel,
        BottomNavItem.Delete,
        BottomNavItem.Share
    )

    BackHandler {
        if (state.selectableMode) {
            galleryViewModel.onEvent(GalleryEvent.CancelSelectableMode)
        } else {
            activity.finish()
        }
    }

    LaunchedEffect(galleryViewModel) {
        galleryViewModel.loadMedia()
        galleryViewModel.galleryEffect.collect {
            when (it) {
                is GalleryEffect.NavigateTo -> {
                    navController.navigate(it.route) {
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
                }
                is GalleryEffect.ShowMessage -> onShowMessage(it.message)
            }
        }
    }

    Scaffold(
        topBar = {
            if (state.selectableMode)
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { galleryViewModel.onEvent(GalleryEvent.CancelSelectableMode) }) {
                            Icon(
                                imageVector = Icons.Sharp.ArrowBack,
                                contentDescription = stringResource(id = R.string.cancel)
                            )
                        }
                    },
                    title = { Text(text = stringResource(id = R.string.select)) },
                    actions = {
                        Icon(
                            imageVector = Icons.Sharp.Checklist,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clickable { galleryViewModel.onEvent(GalleryEvent.SelectAllClicked) },
                            contentDescription = stringResource(id = R.string.select_all)
                        )
                    }
                )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    galleryViewModel.onEvent(GalleryEvent.FabClicked)
                }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.open_camera)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                if (!state.selectableMode) {
                    bottomNavItems.forEach { bottomNavItem ->
                        NavigationBarItem(
                            selected = filterContent == bottomNavItem.filter,
                            icon = {
                                Icon(
                                    imageVector = bottomNavItem.icon,
                                    contentDescription = stringResource(id = bottomNavItem.label)
                                )
                            },
                            label = {
                                Text(text = stringResource(id = bottomNavItem.label))
                            },
                            alwaysShowLabel = false,
                            onClick = { filterContent = bottomNavItem.filter!! }
                        )
                    }
                } else {
                    selectableModeItems.forEach { selectableModeItem ->
                        NavigationBarItem(
                            selected = false,
                            icon = {
                                Icon(
                                    imageVector = selectableModeItem.icon,
                                    contentDescription = stringResource(id = selectableModeItem.label)
                                )
                            },
                            label = {
                                Text(text = stringResource(id = selectableModeItem.label))
                            },
                            alwaysShowLabel = true,
                            onClick = {
                                when (selectableModeItem) {
                                    BottomNavItem.Cancel -> {
                                        galleryViewModel.onEvent(GalleryEvent.CancelSelectableMode)
                                    }
                                    BottomNavItem.Delete -> {
                                        galleryViewModel.onEvent(GalleryEvent.DeleteTapped)
                                    }
                                    BottomNavItem.Share -> {
                                        galleryViewModel.onEvent(GalleryEvent.ShareTapped(context))
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            if (state.deleteTapped) {
                val selectedItems = media.values.flatten().filter { item -> item.selected }
                if (selectedItems.isNotEmpty()) {
                    AlertDialog(onDismissRequest = { /* */ },
                        title = { Text(text = stringResource(id = R.string.delete)) },
                        text = { Text(text = stringResource(id = R.string.delete_items)) },
                        confirmButton = {
                            Button(onClick = { galleryViewModel.onEvent(GalleryEvent.DeleteSelectedItems) }) {
                                Text(text = stringResource(id = R.string.delete))
                            }
                        },
                        dismissButton = {
                            Button(onClick = {
                                galleryViewModel.onEvent(GalleryEvent.CancelDelete)
                            }
                            ) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        }
                    )
                } else {
                    galleryViewModel.onEvent(GalleryEvent.CancelDelete)
                    Toast.makeText(
                        context,
                        stringResource(id = R.string.no_item),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            GalleryScreenContent(
                context = context,
                groupedMedia = when (filterContent) {
                    MediaItem.Filter.ALL -> media
                    MediaItem.Filter.PHOTOS -> groupedPhotos
                    MediaItem.Filter.VIDEOS -> groupedVideos
                },
                selectableMode = state.selectableMode,
                galleryViewModel = galleryViewModel,
                onEvent = galleryViewModel::onEvent
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreenContent(
    context: Context,
    groupedMedia: Map<String, List<MediaItem>>,
    selectableMode: Boolean,
    galleryViewModel: GalleryViewModel,
    onEvent: (GalleryEvent) -> Unit
) {
    val numberOfItemsByRow = LocalConfiguration.current.screenWidthDp / 96
    LazyColumn {
        groupedMedia.forEach { (takenTime, mediaForTakenTime) ->
            stickyHeader {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    text = takenTime,
                )
            }
            items(
                items = mediaForTakenTime.chunked(numberOfItemsByRow)
            ) { mediaList ->
                Row(
                    modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (media in mediaList) {
                        MediaItemBox(
                            item = media,
                            context = context,
                            selectableMode = selectableMode,
                            galleryViewModel = galleryViewModel,
                            onItemClicked = {
                                if (!selectableMode) onEvent(
                                    GalleryEvent.ItemClicked(
                                        it
                                    )
                                )
                            }
                        ) { onEvent(GalleryEvent.ItemLongClicked) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemBox(
    item: MediaItem,
    context: Context,
    selectableMode: Boolean,
    galleryViewModel: GalleryViewModel,
    onItemClicked: (item: MediaItem) -> Unit,
    onItemLongClicked: () -> Unit
) {

    var checked by remember { mutableStateOf(item.selected) }

    LaunchedEffect(checked) {
        snapshotFlow { checked }.collect {
            galleryViewModel.onItemCheckedChange(it, item)
        }
    }

    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(VideoFrameDecoder.Factory())
        }.crossfade(true)
        .build()

    val painter = rememberAsyncImagePainter(
        model = item.uri,
        imageLoader = if (item.type == MediaItem.Type.VIDEO) {
            imageLoader
        } else {
            ImageLoader.Builder(context)
                .crossfade(true)
                .build()
        }
    )

    Box(
        modifier = Modifier
            .height(96.dp)
            .width(96.dp)
            .combinedClickable(
                onLongClick = { onItemLongClicked() },
                onClick = { onItemClicked(item) }
            )
    ) {
        Image(
            modifier = Modifier
                .height(96.dp)
                .width(96.dp)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.background,
                    shape = RectangleShape
                ),
            contentScale = ContentScale.Crop,
            painter = painter,
            contentDescription = stringResource(id = R.string.gallery_items)
        )
        if (item.type == MediaItem.Type.VIDEO) {
            Icon(
                imageVector = Icons.Sharp.PlayCircleOutline,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.inverseSurface
            )
        }
        if (selectableMode) {
            Checkbox(
                modifier = Modifier
                    .offset(x = 5.dp, y = (-10).dp)
                    .align(Alignment.TopEnd),
                checked = item.selected,
                onCheckedChange = {
                    checked = it
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    CameraXProjectTheme {

    }
}