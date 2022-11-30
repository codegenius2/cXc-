package com.armutyus.cameraxproject.ui.gallery

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.models.GalleryEffect
import com.armutyus.cameraxproject.ui.gallery.models.GalleryEvent
import com.armutyus.cameraxproject.ui.gallery.models.GalleryState
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GalleryViewModel constructor(private val fileManager: FileManager) : ViewModel() {

    private val _galleryState = MutableStateFlow(GalleryState())
    val galleryState = _galleryState.asStateFlow()

    private val _mediaItems = MutableStateFlow(mapOf<String, List<MediaItem>>())
    val mediaItems = _mediaItems.asStateFlow()

    private val _galleryEffect = MutableSharedFlow<GalleryEffect>()
    val galleryEffect = _galleryEffect.asSharedFlow()

    fun onEvent(galleryEvent: GalleryEvent) {
        when (galleryEvent) {
            is GalleryEvent.ItemClicked -> onItemClicked(galleryEvent.item)
            is GalleryEvent.ShareTapped -> onShareTapped(galleryEvent.context)

            GalleryEvent.FabClicked -> onFabClicked()
            GalleryEvent.SelectAllClicked -> changeSelectAllState()
            GalleryEvent.ItemLongClicked -> onItemLongClicked()
            GalleryEvent.CancelSelectableMode -> cancelSelectableMode()
            GalleryEvent.CancelDelete -> cancelDeleteAction()
            GalleryEvent.DeleteTapped -> onDeleteTapped()
            GalleryEvent.DeleteSelectedItems -> deleteSelectedItems()
        }
    }

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            val media = mutableSetOf<MediaItem>()

            val photoDir = fileManager.getPrivateFileDirectory(PHOTO_DIR)
            val photos = photoDir?.listFiles()?.mapIndexed { _, file ->
                val takenTime = file.name.substring(0, 10).replace("-", "/")
                MediaItem(
                    takenTime,
                    name = file.name,
                    uri = file.toUri(),
                    type = MediaItem.Type.PHOTO
                )
            } as List<MediaItem>

            val videoDir = fileManager.getPrivateFileDirectory(VIDEO_DIR)
            val videos = videoDir?.listFiles()?.mapIndexed { _, file ->
                val takenTime = file.name.substring(0, 10).replace("-", "/")
                MediaItem(
                    takenTime,
                    name = file.name,
                    uri = file.toUri(),
                    type = MediaItem.Type.VIDEO
                )
            } as List<MediaItem>

            media.addAll(photos + videos)

            val groupedMedia = media.sortedByDescending { it.takenTime }.groupBy { it.takenTime }

            _mediaItems.value += groupedMedia
        }
    }

    private fun onFabClicked() {
        cancelSelectableMode()
        viewModelScope.launch {
            _galleryEffect.emit(GalleryEffect.NavigateTo(PHOTO_ROUTE))
        }
    }

    private fun changeSelectAllState() {
        viewModelScope.launch {
            when (_galleryState.value.selectAllClicked) {
                false -> {
                    _mediaItems.value.forEach {
                        it.value.forEach { mediaItem ->
                            mediaItem.selected = true
                        }
                    }
                }
                true -> {
                    _mediaItems.value.forEach {
                        it.value.forEach { mediaItem ->
                            mediaItem.selected = false
                        }
                    }
                }
            }
        }
        if (_galleryState.value.selectAllClicked) {
            _galleryState.update {
                it.copy(selectAllClicked = false)
            }
        } else {
            _galleryState.update {
                it.copy(selectAllClicked = true)
            }
        }
    }

    private fun onItemClicked(item: MediaItem?) {
        cancelSelectableMode()
        val uri = item?.uri
        viewModelScope.launch {
            _galleryEffect.emit(
                GalleryEffect.NavigateTo("preview_screen/?filePath=${uri?.toString()}")
            )
        }
    }

    private fun onItemLongClicked() {
        _galleryState.update {
            it.copy(
                selectableMode = true
            )
        }
    }

    private fun cancelSelectableMode() {
        viewModelScope.launch {
            _mediaItems.value.forEach {
                it.value.forEach { mediaItem ->
                    mediaItem.selected = false
                }
            }
        }
        _galleryState.update {
            it.copy(
                selectableMode = false,
                selectAllClicked = false
            )
        }
    }

    fun onItemCheckedChange(checked: Boolean, item: MediaItem) {
        viewModelScope.launch {
            if (checked) {
                val itemList = _mediaItems.value.values.flatten()
                val findItem = itemList.firstOrNull { it.uri == item.uri }
                findItem?.selected = true
            } else {
                val itemList = _mediaItems.value.values.flatten()
                val findItem = itemList.firstOrNull { it.uri == item.uri }
                findItem?.selected = false
            }
        }
    }

    private fun onShareTapped(context: Context) {
        viewModelScope.launch {
            val itemList = _mediaItems.value.values.flatten()
            val selectedItems = itemList.filter { it.selected }
            if (selectedItems.isNotEmpty()) {
                val uriList = ArrayList<Uri>()
                selectedItems.forEach {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "com.armutyus.cameraxproject.fileprovider",
                        it.uri?.toFile()!!
                    )
                    uriList.add(contentUri)
                }
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                try {
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.share)
                        )
                    )
                    uriList.clear()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.no_app_available, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, R.string.choose_media, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelDeleteAction() {
        _galleryState.update {
            it.copy(deleteTapped = false)
        }
    }

    private fun onDeleteTapped() {
        _galleryState.update {
            it.copy(deleteTapped = true)
        }
    }

    private fun deleteSelectedItems() {
        viewModelScope.launch {
            val itemList = _mediaItems.value.values.flatten()
            val selectedItems = itemList.filter { it.selected }
            selectedItems.forEach {
                it.uri?.toFile()?.delete()
            }
            cancelDeleteAction()
            cancelSelectableMode()
            loadMedia()
        }
    }
}