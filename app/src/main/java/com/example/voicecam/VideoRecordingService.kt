package com.example.voicecam

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class VideoRecordingService: Service(){

    private val binder = LocalBinder()
    private var recording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    private val job = Job()
    private var loggingJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + job)
    private var isRecording = false


    //Service helpers
    inner class LocalBinder: Binder() {
        fun getService(): VideoRecordingService = this@VideoRecordingService
    }
    override fun onBind(intent: Intent?): IBinder {
        Log.d("testing", "Video Service Bound")
        return binder
    }
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("testing", "Video Service Unbound")
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        initializeCamera()
    }
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    //Operation functions
    private fun initializeCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    ProcessLifecycleOwner.get(),
                    cameraSelector,
                    videoCapture
                )
                Log.d("testing", "CameraX initialized without preview")
            } catch (exc: Exception) {
                Log.e("testing", "CameraX initialization failed: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun startRecording(){
        val videoCapture = videoCapture ?: return

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "VID_$name.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MyAppVideos")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.d("testing", "Recording started")
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(5000L)
                            stopRecording()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Log.d("testing", "Video saved successfully to MediaStore")
                        } else {
                            Log.e("testing", "Recording failed: ${recordEvent.error}")
                        }
                        recording?.close()
                        recording = null
                    }
                }
            }
    }

    fun stopRecording(){
        recording?.stop()
        Log.d("testing", "Recording stopped")
    }

    fun startLogging() {
        if (loggingJob == null || loggingJob?.isCancelled == true) {
            loggingJob = serviceScope.launch{
                while (isActive) {
                    if (recording == null) { // Ensure no recording is in progress
                        startRecording()
                        // Wait for 5 seconds before starting the next recording
                        delay(5000L)
                    }
                }
            }
        }
    }

    fun stopLogging() {
        loggingJob?.cancel()
        loggingJob = null
        Log.d("testing", "Video Service has stopped...")
    }

    suspend fun logMessage() {
        while (true) {
            delay(1000L)
            Log.d("testing", "Video Service is doing something...")
        }
    }
}