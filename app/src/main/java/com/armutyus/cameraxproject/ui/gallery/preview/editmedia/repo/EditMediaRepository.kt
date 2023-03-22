package com.armutyus.cameraxproject.ui.gallery.preview.editmedia.repo

import android.graphics.Bitmap
import com.armutyus.cameraxproject.ui.gallery.preview.editmedia.models.ImageFilter

interface EditMediaRepository {
    suspend fun getImageFiltersList(image: Bitmap): List<ImageFilter>
}