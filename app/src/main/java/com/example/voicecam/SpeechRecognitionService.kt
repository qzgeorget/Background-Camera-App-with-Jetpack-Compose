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
import java.util.Locale

class SpeechRecognitionService: Service() {
    private val binder = LocalBinder()
    private var speechRecognizer: SpeechRecognizer? = null
    private var keywordListener: keywordListener? = null
    private var isListening = false

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
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            Toast.makeText(this@SpeechRecognitionService, "No match found. Please try again.", Toast.LENGTH_SHORT).show()
                            Log.e("command", SpeechRecognizer.RESULTS_RECOGNITION)
                            startListening()
                        }
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            Toast.makeText(this@SpeechRecognitionService, "No speech input. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                        SpeechRecognizer.ERROR_CLIENT -> {
                            Toast.makeText(this@SpeechRecognitionService, "Speech Recognizer stopped.", Toast.LENGTH_SHORT).show()
                        }
                        // Handle other errors accordingly
                        else -> {
                            Toast.makeText(this@SpeechRecognitionService, "An unknown error occurred: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val speechText = matches?.joinToString(separator = " ") ?: "No recognition"

                    // Check for a specific keyword in the recognized speech
                    if (speechText.contains("capture", ignoreCase = true)) {
                        Log.d("testing", "speechText does contain capture")
                        keywordListener?.onKeywordDetected("capture")
                    }
                    startListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    //Operation functions

    fun startListening(){
        if (!isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d("testing", "Speech Recognizer Started")
            Toast.makeText(this@SpeechRecognitionService, "Speech Recognizer started.", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d("testing", "Speech Recognizer Stopped")
        }
    }

    fun setKeywordListener(listener: keywordListener?) {
        this.keywordListener = listener
    }
}