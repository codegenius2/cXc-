package com.armutyus.cameraxproject.ui.video

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.runtime.compositionLocalOf
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.armutyus.cameraxproject.ui.photo.models.CameraState
import com.armutyus.cameraxproject.ui.video.models.PreviewVideoState
import com.armutyus.cameraxproject.util.Util
import com.armutyus.cameraxproject.util.Util.Companion.TAG
import com.armutyus.cameraxproject.util.getAspectRatio
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class VideoCaptureManager private constructor(private val builder: Builder) :
    LifecycleEventObserver {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var activeRecording: Recording

    private val supportedQualities = mutableListOf<Quality>()

    var listener: Listener? = null

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

    /**
     * Using an OrientationEventListener allows you to continuously update the target rotation
     * of the camera use cases as the device’s orientation changes.
     */
    private val orientationEventListener by lazy {
        object : OrientationEventListener(getContext()) {
            @SuppressLint("RestrictedApi")
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == Util.UNKNOWN_ORIENTATION) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                videoCapture.targetRotation = rotation
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

        lifecycleOwner.lifecycleScope.launch {
            val provider = ProcessCameraProvider.getInstance(getContext()).await()
            provider.unbindAll()
            for (camSelector in arrayOf(
                CameraSelector.DEFAULT_BACK_CAMERA,
                CameraSelector.DEFAULT_FRONT_CAMERA
            )) {
                try {
                    // just get the camera.cameraInfo to query capabilities
                    // we are not binding anything here.
                    if (provider.hasCamera(camSelector)) {
                        val camera = provider.bindToLifecycle(lifecycleOwner, camSelector)
                        QualitySelector
                            .getSupportedQualities(camera.cameraInfo)
                            .filter { quality ->
                                listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
                                    .contains(quality)
                            }.also {
                                supportedQualities.addAll(it)
                            }
                    }
                } catch (exc: java.lang.Exception) {
                    Log.e(TAG, "Camera Face $camSelector is not supported")
                }
            }
        }
        listener?.onInitialised(cameraLensInfo, supportedQualities)
        listener?.onVideoStateChanged(cameraState = CameraState.READY)
    }

    /**
     * Takes a [previewVideoState] argument to determine the camera options
     *
     * Create a Preview.
     * Create Video Capture use case
     * Bind the selected camera and any use cases to the lifecycle.
     * Connect the Preview to the PreviewView.
     */
    fun showPreview(
        previewVideoState: PreviewVideoState,
        cameraPreview: PreviewView = getCameraPreview()
    ): View {
        getLifeCycleOwner().lifecycleScope.launchWhenCreated {
            val cameraProvider = cameraProviderFuture.await()
            cameraProvider.unbindAll()

            orientationEventListener.enable()

            //Select a camera lens
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(previewVideoState.cameraLens)
                .build()

            val camera = cameraProvider.bindToLifecycle(getLifeCycleOwner(), cameraSelector)

            // create the user required QualitySelector (video resolution): we know this is
            // supported, a valid qualitySelector will be created.
            val quality = previewVideoState.quality
            val qualitySelector = QualitySelector.from(quality)

            //Create Preview use case
            val preview: Preview = Preview.Builder()
                .setTargetAspectRatio(quality.getAspectRatio(quality))
                .build()
                .apply { setSurfaceProvider(cameraPreview.surfaceProvider) }

            //Create Video Capture use case
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            cameraProvider.bindToLifecycle(
                getLifeCycleOwner(),
                cameraSelector,
                preview,
                videoCapture
            ).apply {
                cameraControl.enableTorch(previewVideoState.torchState == TorchState.ON)
            }
            setupZoomAndTapToFocus(cameraPreview, camera)
        }
        return cameraPreview
    }

    fun updatePreview(previewVideoState: PreviewVideoState, previewView: View) {
        showPreview(previewVideoState, previewView as PreviewView)
    }

    @SuppressLint("MissingPermission")
    fun startRecording(filePath: String) {
        val outputOptions = FileOutputOptions.Builder(File(filePath)).build()
        activeRecording = videoCapture.output
            .prepareRecording(getContext(), outputOptions)
            .apply { withAudioEnabled() }
            .start(ContextCompat.getMainExecutor(getContext()), videoRecordingListener)
    }

    fun pauseRecording() {
        activeRecording.pause()
        listener?.recordingPaused()
    }

    fun resumeRecording() {
        activeRecording.resume()
    }

    fun stopRecording() {
        activeRecording.stop()
    }

    fun videoStateChanged() {
        listener?.onVideoStateChanged(cameraState = CameraState.READY)
    }

    private val videoRecordingListener = Consumer<VideoRecordEvent> { event ->
        when (event) {
            is VideoRecordEvent.Finalize -> if (event.hasError()) {
                listener?.onError(event.cause)
            } else {
                listener?.recordingCompleted(event.outputResults.outputUri)
            }
            is VideoRecordEvent.Pause -> listener?.recordingPaused()
            is VideoRecordEvent.Status -> {
                listener?.onProgress(event.recordingStats.recordedDurationNanos.fromNanoToSeconds())
            }
        }
    }

    interface Listener {
        fun onInitialised(
            cameraLensInfo: HashMap<Int, CameraInfo>,
            supportedQualities: List<Quality>
        )

        fun onVideoStateChanged(cameraState: CameraState)
        fun onProgress(progress: Int)
        fun recordingPaused()
        fun recordingCompleted(outputUri: Uri)
        fun onError(throwable: Throwable?)
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

    class Builder(val context: Context) {

        var lifecycleOwner: LifecycleOwner? = null
            private set

        fun registerLifecycleOwner(source: LifecycleOwner): Builder {
            this.lifecycleOwner = source
            return this
        }

        fun create(): VideoCaptureManager {
            requireNotNull(lifecycleOwner) { "Lifecycle owner is not set" }
            return VideoCaptureManager(this)
        }
    }

    private fun Long.fromNanoToSeconds() = (this / (1000 * 1000 * 1000)).toInt()
}

val LocalVideoCaptureManager =
    compositionLocalOf<VideoCaptureManager> { error("No capture manager found!") }