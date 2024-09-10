package com.example.voicecam

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity: ComponentActivity(), keywordListener, OnMapReadyCallback {

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

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
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

        mapView.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun GetPermissions(){
        val permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.POST_NOTIFICATIONS
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
        var isLogging by remember { mutableStateOf(false) }

        Column(){
            Button(onClick={
                if (isLogging) {
                    videoService?.stopLogging()
                    audioService?.isServiceListening = false
                    audioService?.stopListening()
                } else {
                    videoService?.startLogging()
                    audioService?.isServiceListening = true
                    audioService?.startListening()
                }
                isLogging = !isLogging
            }) {
                Text(text = if (isLogging) "Logging Now" else "Press to Start Logging")
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Google Map Composable
                GoogleMapComponent()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val location = LatLng(37.7749, -122.4194)
        googleMap.addMarker(MarkerOptions().position(location).title("Marker in San Francisco"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location))
    }

    @Composable
    fun GoogleMapComponent(){
        val context = LocalContext.current
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply{
                    onResume()
                }
            }
        )
    }

    override fun onKeywordDetected(speechText: String){
        Log.d("testing", "speechText: $speechText")
        Toast.makeText(this, "Keyword detected: $speechText", Toast.LENGTH_SHORT).show()
        videoService?.saveImagesToMediaStore()
    }

}