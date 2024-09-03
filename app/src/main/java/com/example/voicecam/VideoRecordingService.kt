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
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class VideoRecordingService: Service(){

    private val binder = LocalBinder()
    private var imageCapture: ImageCapture? = null

    private val imageFileQueue = ArrayDeque<File>()
    private val maxImages = 25

    private val job = Job()
    private var loggingJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + job)

    private val queueMutex = Mutex()

    private var capturing = false


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
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    ProcessLifecycleOwner.get(),
                    cameraSelector,
                    imageCapture
                )
                Log.d("testing", "CameraX initialized without preview")
            } catch (exc: Exception) {
                Log.e("testing", "CameraX initialization failed: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startRecording() {
        serviceScope.launch {
            try {
                while (isActive && capturing) {
                    val startTime = System.currentTimeMillis()
                    captureImage()

                    // Adjust delay based on the time taken to capture an image
                    val captureTime = System.currentTimeMillis() - startTime
                    val delayTime = (500L - captureTime).coerceAtLeast(250L)

                    delay(delayTime) //delayTime
                }
            } catch (e: Exception) {
                Log.e("testing", "Error in capturing loop: ${e.message}")
            }
        }
    }

    private fun captureImage(){
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(System.currentTimeMillis())
        val file = File(filesDir, "IMG_$name.jpg")

        if (imageFileQueue.size >= maxImages) {
            val oldestFile = imageFileQueue.removeFirst()
            if (oldestFile.exists()){
                oldestFile.delete()
            }
        }
        imageFileQueue.addLast(file)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("testing", "Image captured: ${file.absolutePath}")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("testing", "Image capture failed: ${exception.message}")
                }
            })
    }

    fun saveVideosToMediaStore(){
        serviceScope.launch{
            queueMutex.withLock {
                for (file in imageFileQueue){
                    try{
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VoiceCam")
                            }
                        }

                        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                        uri?.let {
                            contentResolver.openOutputStream(it)?.use {outputStream ->
                                file.inputStream().use{inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            Log.d("testing", "Image saved to MediaStore: ${file.absolutePath}")
                        }?: Log.e("testing", "Failed to save image to MediaStore: ${file.absolutePath}")
                    } catch (exc: Exception) {
                        Log.e("testing", "Error saving image to MediaStore: ${exc.message}")
                    }
                }
            }
        }
    }

    fun startLogging() {
        if (loggingJob == null || loggingJob?.isCancelled == true) {
            capturing = true
            loggingJob = serviceScope.launch{
                startRecording()
            }
        }
    }

    fun stopLogging() {
        capturing = false
        loggingJob?.cancel()
        loggingJob = null
        Log.d("testing", "Video Service has stopped...")
    }
}