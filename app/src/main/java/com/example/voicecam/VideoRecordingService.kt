package com.example.voicecam

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoRecordingService: Service(){

    private val binder = LocalBinder()

    private val job = Job()
    private var loggingJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + job)


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
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    //Operation functions
    fun startLogging() {
        if (loggingJob == null || loggingJob?.isCancelled == true) {
            loggingJob = serviceScope.launch{
                logMessage()
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