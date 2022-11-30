package com.armutyus.cameraxproject.ui.photo

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.compositionLocalOf
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.*
import com.armutyus.cameraxproject.ui.photo.models.PreviewPhotoState
import com.armutyus.cameraxproject.util.Util
import com.armutyus.cameraxproject.util.Util.Companion.CAPTURE_FAIL
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import com.armutyus.cameraxproject.util.Util.Companion.UNKNOWN_ORIENTATION
import com.armutyus.cameraxproject.util.aspectRatio
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PhotoCaptureManager private constructor(private val builder: Builder) :
    LifecycleEventObserver, ImageAnalysis.Analyzer {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis

    var photoListener: PhotoListener = object : PhotoListener {
        override fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>) {}
        override fun onExtensionModeChanged(availableExtensions: List<Int>) {}
        override fun onSuccess(imageResult: ImageCapture.OutputFileResults) {}
        override fun onError(exception: Exception) {}
    }

    init {
        getLifecycle().addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                cameraProviderFuture = ProcessCameraProvider.getInstance(getContext())
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    queryCameraInfo(source, cameraProvider)
                }, ContextCompat.getMainExecutor(getContext()))
            }
            else -> Unit
        }
    }

    private fun getCameraPreview() = PreviewView(getContext()).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        keepScreenOn = true
    }

    private fun getLifecycle() = builder.lifecycleOwner?.lifecycle!!

    private fun getContext() = builder.context

    private fun getLifeCycleOwner() = builder.lifecycleOwner!!

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getView() = builder.context.display!!

    /**
     * Using an OrientationEventListener allows you to continuously update the target rotation
     * of the camera use cases as the device’s orientation changes.
     */
    private val orientationEventListener by lazy {
        object : OrientationEventListener(getContext()) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == UNKNOWN_ORIENTATION) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageAnalyzer.targetRotation = rotation
                imageCapture.targetRotation = rotation
            }
        }
    }

    /**
     * Queries the capabilities of the FRONT and BACK camera lens
     * The result is stored in an array map.
     *
     * With this, we can determine if a camera lens is available or not,
     * and what capabilities the lens can support e.g flash support
     */
    private fun queryCameraInfo(
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider
    ) {
        val cameraLensInfo = HashMap<Int, CameraInfo>()

        arrayOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT).forEach { lens ->
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lens).build()
            if (cameraProvider.hasCamera(cameraSelector)) {
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
                if (lens == CameraSelector.LENS_FACING_BACK) {
                    cameraLensInfo[CameraSelector.LENS_FACING_BACK] = camera.cameraInfo
                } else if (lens == CameraSelector.LENS_FACING_FRONT) {
                    cameraLensInfo[CameraSelector.LENS_FACING_FRONT] = camera.cameraInfo
                }
            }
        }
        photoListener.onInitialised(cameraLensInfo)
    }

    fun queryExtensions(previewPhotoState: PreviewPhotoState) {
        getLifeCycleOwner().lifecycleScope.launch {
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(previewPhotoState.cameraLens)
                .build()

            cameraProviderFuture = ProcessCameraProvider.getInstance(getContext())

            val cameraProvider = cameraProviderFuture.await()
            val extensionsManager =
                ExtensionsManager.getInstanceAsync(getContext(), cameraProvider).await()

            cameraProvider.unbindAll()

            // get the supported extensions for the selected camera lens by filtering the full list
            // of extensions and checking each one if it's available
            val availableExtensions = listOf(
                ExtensionMode.AUTO,
                ExtensionMode.BOKEH,
                ExtensionMode.HDR,
                ExtensionMode.NIGHT,
                ExtensionMode.FACE_RETOUCH
            ).filter { extensionMode ->
                extensionsManager.isExtensionAvailable(cameraSelector, extensionMode)
            }

            photoListener.onExtensionModeChanged(
                availableExtensions = listOf(
                    ExtensionMode.NONE
                ) + availableExtensions
            )
        }
    }

    /**
     * Takes a [previewPhotoState] argument to determine the camera options
     *
     * Create a Preview.
     * Create Image Capture use case
     * Bind the selected camera and any use cases to the lifecycle.
     * Connect the Preview to the PreviewView.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    @Synchronized
    private fun showPreview(
        previewPhotoState: PreviewPhotoState,
        cameraPreview: PreviewView
    ): View {
        getLifeCycleOwner().lifecycleScope.launch {
            getLifecycle().repeatOnLifecycle(Lifecycle.State.RESUMED) {
                cameraProviderFuture = ProcessCameraProvider.getInstance(getContext())

                val cameraProvider = cameraProviderFuture.await()
                val extensionsManager =
                    ExtensionsManager.getInstanceAsync(getContext(), cameraProvider).await()

                cameraProvider.unbindAll()

                // Every time the orientation of device changes, update rotation for use cases
                orientationEventListener.enable()

                // Get screen metrics used to setup camera for full screen resolution
                val metrics = Util.ScreenSizeCompat.getScreenSize(getContext())
                Log.d(TAG, "Screen metrics: ${metrics.width} x ${metrics.height}")
                val screenAspectRatio = aspectRatio(metrics.width, metrics.height)
                Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

                val rotation = getView().rotation

                //Select a camera lens with or without extensions
                val cameraSelector: CameraSelector =
                    if (previewPhotoState.extensionMode == ExtensionMode.NONE) {
                        CameraSelector.Builder()
                            .requireLensFacing(previewPhotoState.cameraLens)
                            .build()
                    } else {
                        extensionsManager.getExtensionEnabledCameraSelector(
                            CameraSelector.Builder()
                                .requireLensFacing(previewPhotoState.cameraLens)
                                .build(),
                            previewPhotoState.extensionMode
                        )
                    }

                val camera = cameraProvider.bindToLifecycle(getLifeCycleOwner(), cameraSelector)

                //Create Preview use case
                val preview: Preview = Preview.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build()
                    .apply { setSurfaceProvider(cameraPreview.surfaceProvider) }

                //Create Image Capture use case
                imageCapture = ImageCapture.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .setFlashMode(previewPhotoState.flashMode)
                    .build()

                //Create Image Analyzer use case
                imageAnalyzer = ImageAnalysis.Builder()
                    // We request aspect ratio but no resolution
                    .setTargetAspectRatio(screenAspectRatio)
                    // Set initial target rotation, we will have to call this again if rotation changes
                    // during the lifecycle of this use case
                    .setTargetRotation(rotation)
                    .build()

                //imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(getContext()), this@PhotoCaptureManager)

                cameraProvider.bindToLifecycle(
                    getLifeCycleOwner(),
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
                setupZoomAndTapToFocus(cameraPreview, camera)
            }
        }
        return cameraPreview
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showPreview(previewPhotoState: PreviewPhotoState): View {
        return showPreview(previewPhotoState, getCameraPreview())
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun updatePreview(previewPhotoState: PreviewPhotoState, previewView: View) {
        showPreview(previewPhotoState, previewView as PreviewView)
    }

    fun takePhoto(filePath: String, cameraLens: Int) {
        val photoFile = File(filePath)
        val outputFileOptions = getOutputFileOptions(cameraLens, photoFile)

        imageCapture.takePicture(
            outputFileOptions,
            Executors.newSingleThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    // If the folder selected is an external media directory, this is
                    // unnecessary but otherwise other apps will not be able to access our
                    // images unless we scan them using [MediaScannerConnection]
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(savedUri.toFile().extension)
                    MediaScannerConnection.scanFile(
                        getContext(),
                        arrayOf(savedUri.toFile().absolutePath),
                        arrayOf(mimeType)
                    ) { _, uri ->
                        Log.d(TAG, "Image capture scanned into media store: $uri")
                    }
                    photoListener.onSuccess(output)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, exception.localizedMessage ?: CAPTURE_FAIL)
                    photoListener.onError(exception)
                }
            }
        )
    }

    override fun analyze(image: ImageProxy) {
        TODO("Not yet implemented")
    }

    private fun setupZoomAndTapToFocus(cameraView: PreviewView, camera: Camera) {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio: Float = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1F
                val delta = detector.scaleFactor
                camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(cameraView.context, listener)

        cameraView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                cameraView.performClick()
                val factory = cameraView.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(5, TimeUnit.SECONDS)
                    .build()
                camera.cameraControl.startFocusAndMetering(action)
            }
            true
        }
    }

    private fun getOutputFileOptions(
        cameraLens: Int,
        photoFile: File
    ): ImageCapture.OutputFileOptions {
        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = cameraLens == CameraSelector.LENS_FACING_FRONT
        }
        // Create output options object which contains file + metadata
        return ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()
    }

    class Builder(val context: Context) {
        var lifecycleOwner: LifecycleOwner? = null
            private set

        fun registerLifecycleOwner(source: LifecycleOwner): Builder {
            this.lifecycleOwner = source
            return this
        }

        fun create(): PhotoCaptureManager {
            requireNotNull(lifecycleOwner) { "Lifecycle owner is not set" }
            return PhotoCaptureManager(this)
        }
    }

    interface PhotoListener {
        fun onInitialised(cameraLensInfo: HashMap<Int, CameraInfo>)
        fun onExtensionModeChanged(availableExtensions: List<Int>)
        fun onSuccess(imageResult: ImageCapture.OutputFileResults)
        fun onError(exception: Exception)
    }

}

val LocalPhotoCaptureManager =
    compositionLocalOf<PhotoCaptureManager> { error("No capture manager found!") }