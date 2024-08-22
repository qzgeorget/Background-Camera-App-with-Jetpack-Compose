package com.example.voicecam

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity: ComponentActivity() {

    private var isBound: Boolean = false
    private var myService: VideoRecordingService? = null

    private val connection= object: ServiceConnection{
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as VideoRecordingService.LocalBinder
            myService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
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
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    @Composable
    fun MyApp() {
        var isLogging by remember { mutableStateOf(false) }

        Button(onClick={
            if (isLogging) {
                myService?.stopLogging()
            } else {
                myService?.startLogging()
            }
            isLogging = !isLogging
        }) {
            Text(text = if (isLogging) "Logging Started" else "Start Logging")
        }
    }

}