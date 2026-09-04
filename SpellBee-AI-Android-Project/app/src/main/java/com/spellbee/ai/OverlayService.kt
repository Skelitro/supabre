package com.spellbee.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class OverlayService : Service(), RecognitionListener {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var speechService: SpeechService? = null
    private var isUltraMode = false
    private var selectedLanguage = "English"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        setupFloatingView()
        initVosk()
    }

    private fun startForegroundService() {
        val channelId = "spellbee_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SpellBee Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SpellBee AI Active")
            .setContentText("Floating bubble is running live")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    private fun setupFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_overlay, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 80
        params.y = 120

        val bubble = floatingView.findViewById<View>(R.id.bubble_root)
        val modeBtn = floatingView.findViewById<Button>(R.id.toggle_mode_btn)
        val modeLabel = floatingView.findViewById<TextView>(R.id.mode_label)
        val langSpinner = floatingView.findViewById<Spinner>(R.id.language_spinner)

        val languages = arrayOf("English", "Spanish", "French", "German")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        langSpinner.adapter = adapter

        langSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLanguage = languages[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        modeBtn.setOnClickListener {
            isUltraMode = !isUltraMode
            modeLabel.text = if (isUltraMode) "ULTRA AI" else "NORMAL MODE"
            modeBtn.text = if (isUltraMode) "Switch to Normal" else "Switch to Ultra"
        }

        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    private fun initVosk() {
        StorageService.unpack(this, "model-en-us", "model",
            { model: Model ->
                try {
                    speechService = SpeechService(Recognizer(model, 16000.0f), 16000.0f)
                    speechService?.startListening(this)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            },
            { e: IOException -> e.printStackTrace() }
        )
    }

    override fun onResult(hypothesis: String) {
        val text = parseHypothesis(hypothesis)
        updateUI(text)
    }

    override fun onPartialResult(hypothesis: String) {
        val text = parseHypothesis(hypothesis)
        updateUI(text)
    }

    private fun updateUI(text: String) {
        val textView = floatingView.findViewById<TextView>(R.id.transcription_text)
        val suggestionsView = floatingView.findViewById<TextView>(R.id.suggestions_text)
        val commandView = floatingView.findViewById<TextView>(R.id.command_text)

        textView.text = if (text.isEmpty()) "Listening..." else text

        val command = PhonemeAI.detectCommands(text)
        if (command != null) {
            commandView.visibility = View.VISIBLE
            commandView.text = command
        } else {
            commandView.visibility = View.GONE
        }

        if (isUltraMode && text.isNotEmpty()) {
            val suggestions = PhonemeAI.generateSuggestions(text, selectedLanguage)
            suggestionsView.text = "[$selectedLanguage] Suggestions: " + suggestions.joinToString(", ")
            suggestionsView.visibility = View.VISIBLE
        } else {
            suggestionsView.visibility = View.GONE
        }
    }

    private fun parseHypothesis(hypothesis: String): String {
        return hypothesis.substringAfter(""text" : "").substringBefore(""")
    }

    override fun onDestroy() {
        super.onDestroy()
        speechService?.stop()
        speechService?.shutdown()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }

    override fun onError(error: Exception?) {}
    override fun onTimeout() {}
    override fun onFinalResult(hypothesis: String?) {}
}
