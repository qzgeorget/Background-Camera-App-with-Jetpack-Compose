package com.example.voicecam

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class VideoRecordingService: Service(){

    private val binder = LocalBinder()

    inner class LocalBinder: Binder() {
        fun getService(): VideoRecordingService = this@VideoRecordingService
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("testing", "Service Bound")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("testing", "Service Unbound")
        return super.onUnbind(intent)
    }

    fun logMessage() {
        Log.d("testing", "Service is doing something...")
    }
}