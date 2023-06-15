package com.armutyus.cameraxproject.ui.gallery

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.models.GalleryEvent
import com.armutyus.cameraxproject.ui.gallery.models.GalleryState
import com.armutyus.cameraxproject.ui.gallery.models.MediaItem
import com.armutyus.cameraxproject.util.BaseViewModel
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util.Companion.EDIT_DIR
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_DIR
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.VIDEO_DIR
import kotlinx.coroutines.launch

class GalleryViewModel constructor(
    private val fileManager: FileManager,
    navController: NavController
) : BaseViewModel(navController) {

    private val _galleryState: MutableLiveData<GalleryState> = MutableLiveData(GalleryState())
    val galleryState: LiveData<GalleryState> = _galleryState

    private val _mediaItems: MutableLiveData<Map<String, List<MediaItem>>> =
        MutableLiveData(mapOf())
    val mediaItems: LiveData<Map<String, List<MediaItem>>> = _mediaItems

    fun onEvent(galleryEvent: GalleryEvent) {
        when (galleryEvent) {
            is GalleryEvent.ItemClicked -> onItemClicked(
                galleryEvent.item,
                galleryEvent.contentFilter
            )

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

    fun loadMedia() = viewModelScope.launch {
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

        val editedMediaDir = fileManager.getPrivateFileDirectory(EDIT_DIR)
        val editedMedia = editedMediaDir?.listFiles()?.mapIndexed { _, file ->
            val editTime = file.name.substring(4, 14).replace("-", "/")
            MediaItem(
                editTime,
                name = file.name,
                uri = file.toUri(),
                type = if (file.extension == "jpg") MediaItem.Type.PHOTO else MediaItem.Type.VIDEO
            )
        } as List<MediaItem>

        media.addAll(photos + videos + editedMedia)

        val groupedMedia = media.sortedByDescending { it.takenTime }.groupBy { it.takenTime }
        _mediaItems.value = groupedMedia
    }

    fun onItemCheckedChange(checked: Boolean, item: MediaItem) = viewModelScope.launch {
        val itemList = _mediaItems.value?.values?.flatten() ?: emptyList()
        val findItem = itemList.firstOrNull { it.uri == item.uri }
        findItem?.selected = checked
    }

    private fun onFabClicked() = viewModelScope.launch {
        cancelSelectableMode()
        navigateTo(PHOTO_ROUTE)
    }

    private fun changeSelectAllState() = viewModelScope.launch {
        val newValue: Boolean = !_galleryState.value!!.selectAllClicked
        _mediaItems.value?.forEach {
            it.value.forEach { mediaItem ->
                mediaItem.selected = newValue
            }
        }
        _galleryState.value = _galleryState.value!!.copy(selectAllClicked = newValue)
    }

    private fun onItemClicked(item: MediaItem?, contentFilter: String) = viewModelScope.launch {
        val uri = item?.uri
        navigateTo("preview_screen/?filePath=${uri?.toString()}/?contentFilter=${contentFilter}")
    }

    private fun onItemLongClicked() = viewModelScope.launch {
        _galleryState.value = _galleryState.value!!.copy(selectableMode = true)
    }

    private fun cancelSelectableMode() = viewModelScope.launch {
        _mediaItems.value?.forEach {
            it.value.forEach { mediaItem ->
                mediaItem.selected = false
            }
        }
        _galleryState.value =
            _galleryState.value!!.copy(selectableMode = false, selectAllClicked = false)
    }

    private fun onShareTapped(context: Context) = viewModelScope.launch {
        val itemList = _mediaItems.value?.values?.flatten() ?: emptyList()
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

    private fun cancelDeleteAction() = viewModelScope.launch {
        _galleryState.value = _galleryState.value!!.copy(deleteTapped = false)
    }

    private fun onDeleteTapped() = viewModelScope.launch {
        _galleryState.value = _galleryState.value!!.copy(deleteTapped = true)
    }

    private fun deleteSelectedItems() = viewModelScope.launch {
        val itemList = _mediaItems.value?.values?.flatten() ?: emptyList()
        val selectedItems = itemList.filter { it.selected }
        selectedItems.forEach {
            it.uri?.toFile()?.delete()
        }
        cancelDeleteAction()
        cancelSelectableMode()
        loadMedia()
    }
}