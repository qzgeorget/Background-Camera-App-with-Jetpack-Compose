package com.example.voicecam

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SpeechRecognitionService: Service() {
    private val binder = LocalBinder()
    private var speechRecognizer: SpeechRecognizer? = null
    private var keywordListener: keywordListener? = null
    private var isListening = false
    var isServiceListening = false

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
        keywordListener = null
        return super.onUnbind(intent)
    }

    //Speech initializations
    override fun onCreate() {
        super.onCreate()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply{
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    handleRecognitionError(error)
                }

                override fun onResults(results: Bundle?) {
                    processRecognitionResults(results)
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun handleRecognitionError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found. Please try again."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input. Please try again."
            SpeechRecognizer.ERROR_CLIENT -> "Speech Recognizer stopped."
            else -> "An unknown error occurred: $error"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        if(error == SpeechRecognizer.ERROR_CLIENT) {
            return
        }
        // Delay before restarting listening to prevent rapid loops
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000) // 1-second delay
            startListening()
        }
    }

    private fun processRecognitionResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val speechText = matches?.joinToString(separator = " ") ?: "No recognition"

        if (speechText.contains("capture", ignoreCase = true)) {
            Log.d("testing", "Speech text contains capture")
            keywordListener?.onKeywordDetected("capture")
        }
        // Continue listening
        startListening()
    }

    //Operation functions

    fun startListening(){
        if (!isListening && isServiceListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d("testing", "Speech Recognizer Started")
            Toast.makeText(this@SpeechRecognitionService, "Speech Recognizer started.", Toast.LENGTH_SHORT).show()
        }
        isListening = false
    }

    fun stopListening() {
        if (!isServiceListening){
            speechRecognizer?.stopListening()
            Log.d("testing", "Speech Recognizer Stopped")
        }
        isListening = false
    }

    fun setKeywordListener(listener: keywordListener?) {
        this.keywordListener = listener
    }
}