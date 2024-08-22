package com.example.voicecam

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class SpeechRecognitionService: Service() {
    private val binder = LocalBinder()

    //Service Helpers
    inner class LocalBinder: Binder() {
        fun getService(): SpeechRecognitionService = this@SpeechRecognitionService
    }
    override fun onBind(intent: Intent?): IBinder {
        Log.d("testing", "Audio Service Bound")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("testing", "Audio Service Unbound")
        return super.onUnbind(intent)
    }

    //Operation functions
    fun logMessage() {
        Log.d("testing", "Audio Service is doing something...")
    }
}