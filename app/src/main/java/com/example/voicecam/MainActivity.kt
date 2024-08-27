package com.example.voicecam

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner

class MainActivity: ComponentActivity(), keywordListener {

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
            audioService?.setKeywordListener(this@MainActivity)
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
                GetPermissions()
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
            audioService?.setKeywordListener(null)
            isBoundAudio = false
        }
    }

    @Composable
    fun GetPermissions(){
        val permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )

        var allPermissionGranted by remember { mutableStateOf(false)}

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {permissionsResult ->
            allPermissionGranted = permissionsResult.values.all{it}
        }

        LaunchedEffect(Unit) {
            launcher.launch(permissions.toTypedArray())
        }

        if (allPermissionGranted) {
            MyApp()
        }
    }

    @Composable
    fun MyApp() {
        var isVideoLogging by remember { mutableStateOf(false) }
        var isAudioRecording by remember { mutableStateOf(false) }

        Row {
            Button(onClick={
                if (isVideoLogging) {
                    videoService?.stopRecording()
                } else {
                    videoService?.startRecording()
                }
                isVideoLogging = !isVideoLogging
            }) {
                Text(text = if (isVideoLogging) "Recording" else "Recording Ended")
            }

            Button(onClick={
                if (isAudioRecording) {
                    audioService?.stopListening()
                } else {
                    audioService?.startListening()
                }
                isAudioRecording = !isAudioRecording
            }) {
                Text(text = if (isAudioRecording) "Stop Audio Recording" else "Start Audio Recording")
            }
        }
    }

    override fun onKeywordDetected(speechText: String){
        Log.d("testing", "speechText: $speechText")
        Toast.makeText(this, "Keyword detected: $speechText", Toast.LENGTH_SHORT).show()
    }

}