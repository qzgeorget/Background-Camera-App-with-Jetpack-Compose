package com.example.voicecam

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity: ComponentActivity() {

    private var isBoundVideo: Boolean = false
    private var videoService: VideoRecordingService? = null

    private var isBoundAudio: Boolean = false
    private var audioService: SpeechRecognitionService? = null

    private val videoConnection= object: ServiceConnection{
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as VideoRecordingService.LocalBinder
            videoService = binder.getService()
            isBoundVideo = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBoundVideo = false
        }
    }

    private val audioConnection= object: ServiceConnection{
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SpeechRecognitionService.LocalBinder
            audioService = binder.getService()
            isBoundAudio = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBoundAudio = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            Surface {
                MyApp()
            }
        }
        Intent(this, VideoRecordingService::class.java).also { intent ->
            bindService(intent, videoConnection, Context.BIND_AUTO_CREATE)
        }
        Intent(this, SpeechRecognitionService::class.java).also { intent ->
            bindService(intent, audioConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isBoundVideo) {
            unbindService(videoConnection)
            isBoundVideo = false
        }
        if (isBoundAudio) {
            unbindService(audioConnection)
            isBoundAudio = false
        }
    }

    @Composable
    fun MyApp() {
        var isVideoLogging by remember { mutableStateOf(false) }
        Row {
            Button(onClick={
                if (isVideoLogging) {
                    videoService?.stopLogging()
                } else {
                    videoService?.startLogging()
                }
                isVideoLogging = !isVideoLogging
            }) {
                Text(text = if (isVideoLogging) "Video Logging Started" else "Start Video Logging")
            }
            Button(onClick={
                audioService?.logMessage()
            }) {
                Text(text = "Audio Message")
            }
        }
    }

}