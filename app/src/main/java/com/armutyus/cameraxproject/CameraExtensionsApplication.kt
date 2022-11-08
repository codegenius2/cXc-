package com.armutyus.cameraxproject

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class CameraExtensionsApplication : Application(), CameraXConfig.Provider {
    override fun getCameraXConfig(): CameraXConfig =
        CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setCameraExecutor(Dispatchers.IO.asExecutor())
            .setMinimumLoggingLevel(Log.ERROR)
            .build()
}