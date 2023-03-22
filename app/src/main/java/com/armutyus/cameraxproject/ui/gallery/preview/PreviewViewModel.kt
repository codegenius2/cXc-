package com.armutyus.cameraxproject.ui.gallery.preview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.armutyus.cameraxproject.R
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.models.ImageFilter
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.repo.EditMediaRepository
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenEvent
import com.armutyus.cameraxproject.ui.gallery.preview.models.PreviewScreenState
import com.armutyus.cameraxproject.util.BaseViewModel
import com.armutyus.cameraxproject.util.FileManager
import com.armutyus.cameraxproject.util.Util.Companion.EDIT_CONTENT
import com.armutyus.cameraxproject.util.Util.Companion.EDIT_DIR
import com.armutyus.cameraxproject.util.Util.Companion.GALLERY_ROUTE
import com.armutyus.cameraxproject.util.Util.Companion.PHOTO_EXTENSION
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import kotlinx.coroutines.launch
import java.io.File

class PreviewViewModel constructor(
    private val editMediaRepository: EditMediaRepository,
    private val fileManager: FileManager,
    navController: NavController
) : BaseViewModel(navController) {

    private val _previewScreenState: MutableLiveData<PreviewScreenState> =
        MutableLiveData(PreviewScreenState())
    val previewScreenState: LiveData<PreviewScreenState> = _previewScreenState

    fun onEvent(previewScreenEvent: PreviewScreenEvent) {
        when (previewScreenEvent) {
            is PreviewScreenEvent.ShareTapped -> onShareTapped(
                previewScreenEvent.context,
                previewScreenEvent.file
            )
            is PreviewScreenEvent.DeleteTapped -> onDeleteTapped(previewScreenEvent.file)
            is PreviewScreenEvent.FullScreenToggleTapped -> onFullScreenToggleTapped(
                previewScreenEvent.isFullScreen
            )
            is PreviewScreenEvent.ChangeBarState -> onChangeBarState(previewScreenEvent.zoomState)
            is PreviewScreenEvent.HideController -> hideController(previewScreenEvent.isPlaying)
            is PreviewScreenEvent.SaveTapped -> saveEditedImage(previewScreenEvent.context)
            PreviewScreenEvent.EditTapped -> onEditTapped()
            PreviewScreenEvent.CancelEditTapped -> onCancelEditTapped()
            PreviewScreenEvent.PlayerViewTapped -> onPlayerViewTapped()
            PreviewScreenEvent.NavigateBack -> onNavigateBack()
        }
    }

    private fun onEditTapped() = viewModelScope.launch {
        _previewScreenState.value = _previewScreenState.value!!.copy(
            isInEditMode = true,
            showBars = false
        )
    }

    private fun onCancelEditTapped() = viewModelScope.launch {
        _previewScreenState.value = _previewScreenState.value!!.copy(
            isInEditMode = false,
            showBars = true
        )
        _imageHasFilter.value = false
        _isImageCropped.value = false
        _editedBitmap.value = null
        _croppedBitmap.value = null
    }

    private fun onDeleteTapped(file: File) = viewModelScope.launch {
        file.delete()
        navigateTo(GALLERY_ROUTE)
    }

    private fun onShareTapped(context: Context, file: File) = viewModelScope.launch {
        if (file.exists()) {
            val contentUri = FileProvider.getUriForFile(
                context,
                "com.armutyus.cameraxproject.fileprovider",
                file
            )
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            try {
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        context.getString(R.string.share)
                    )
                )
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_app_available, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.file_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onFullScreenToggleTapped(isFullScreen: Boolean) = viewModelScope.launch {
        _previewScreenState.value = _previewScreenState.value!!
            .copy(
                isFullScreen = !isFullScreen,
                showBars = isFullScreen,
                showMediaController = isFullScreen
            )
    }

    private fun onPlayerViewTapped() = viewModelScope.launch {
        val newValue =
            !(_previewScreenState.value!!.showBars && _previewScreenState.value!!.showMediaController)
        _previewScreenState.value =
            _previewScreenState.value!!.copy(showBars = newValue, showMediaController = newValue)
    }

    private fun onChangeBarState(zoomState: Boolean) = viewModelScope.launch {
        if (zoomState) {
            _previewScreenState.value = _previewScreenState.value?.copy(showBars = false)
        } else {
            val newValue = !_previewScreenState.value!!.showBars
            _previewScreenState.value = _previewScreenState.value?.copy(showBars = newValue)
        }
    }

    private fun hideController(isPlaying: Boolean) = viewModelScope.launch {
        _previewScreenState.value =
            _previewScreenState.value?.copy(showMediaController = !isPlaying, showBars = !isPlaying)
    }

    //region:: EditMedia Works

    private val _imageFilterList: MutableLiveData<List<ImageFilter>> = MutableLiveData(emptyList())
    val imageFilterList: LiveData<List<ImageFilter>> = _imageFilterList

    private val _editedBitmap: MutableLiveData<Bitmap?> = MutableLiveData()
    val editedBitmap: LiveData<Bitmap?> = _editedBitmap

    private val _croppedBitmap: MutableLiveData<Bitmap?> = MutableLiveData()
    val croppedBitmap: LiveData<Bitmap?> = _croppedBitmap

    private val _imageHasFilter: MutableLiveData<Boolean> = MutableLiveData(false)
    val imageHasFilter: LiveData<Boolean> = _imageHasFilter

    private val _isImageCropped: MutableLiveData<Boolean> = MutableLiveData(false)
    val isImageCropped: LiveData<Boolean> = _isImageCropped

    fun loadImageFilters(bitmap: Bitmap?) = viewModelScope.launch {
        kotlin.runCatching {
            val image = getPreviewImage(originalImage = bitmap!!)
            editMediaRepository.getImageFiltersList(image)
        }.onSuccess {
            setImageFilterList(it)
        }.onFailure {
            Log.e(TAG, it.localizedMessage ?: "Bitmaps with filter load failed.")
        }
    }

    private fun getPreviewImage(originalImage: Bitmap): Bitmap {
        return kotlin.runCatching {
            val previewWidth = 90
            val previewHeight = originalImage.height * previewWidth / originalImage.width
            Bitmap.createScaledBitmap(originalImage, previewWidth, previewHeight, true)
        }.getOrDefault(originalImage)
    }

    private fun saveEditedImage(context: Context) = viewModelScope.launch {
        if (_editedBitmap.value != null || _imageHasFilter.value == true) {
            fileManager.saveEditedImageToFile(_editedBitmap.value!!, EDIT_DIR, PHOTO_EXTENSION)
                .also {
                    navigateTo("preview_screen/?filePath=${it}/?contentFilter=${EDIT_CONTENT}")
                    onCancelEditTapped()
                }
            Toast.makeText(context, R.string.edited_image_saved, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.no_changes_on_image, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setImageFilterList(imageFilterList: List<ImageFilter>) = viewModelScope.launch {
        _imageFilterList.value = imageFilterList
    }

    fun setCroppedImage(croppedImage: Bitmap?) = viewModelScope.launch {
        _isImageCropped.value = croppedImage != null
        _croppedBitmap.value = croppedImage
        _editedBitmap.value = croppedImage
    }

    fun selectedFilter(filterName: String) = viewModelScope.launch {
        _imageHasFilter.value = filterName != "Normal"
    }

    fun setEditedBitmap(imageBitmap: Bitmap) = viewModelScope.launch {
        _editedBitmap.value = imageBitmap
    }

    fun switchEditMode(editModeName: String) = viewModelScope.launch {
        _previewScreenState.value = _previewScreenState.value?.copy(switchEditMode = editModeName)
    }

    //endregion

}