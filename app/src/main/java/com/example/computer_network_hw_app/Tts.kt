package com.example.computer_network_hw_app

import android.speech.tts.TextToSpeech
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale

@Module
@InstallIn(SingletonComponent::class)
object TtsModule {
    @Provides
    @Singleton
    fun provideTTSModel(@ApplicationContext appContext: Context): Tts {
        return Tts(appContext)
    }
}


// 定義一個狀態來追蹤 TTS 引擎的準備情況
enum class TtsState {
    INITIALIZING,
    READY,
    ERROR,
}



// 保持單例設定
@Singleton
class Tts @Inject constructor(private val context : Context) {

    private var tts: TextToSpeech? = null
    private var currentState: TtsState = TtsState.INITIALIZING
    init {
        Log.d("Tts", "Initializing TextToSpeech engine...")
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("Tts", "TextToSpeech initialized successfully.")
                val result = tts?.setLanguage(Locale.TAIWAN)

                when (result) {
                    TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE, TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                        currentState = TtsState.READY
                    }
                    else -> {
                        currentState = TtsState.ERROR
                        Log.e("Tts", "Failed to set language or unknown result: $result")
                    }
                }

            } else {
                currentState = TtsState.ERROR
                Log.e("Tts", "TextToSpeech initialization failed with status: $status")
            }
        }
    }

    public fun isSpeaking() : Boolean {
        return tts?.isSpeaking ?: false
    }

    public fun speech(text: String) {
        if (currentState != TtsState.READY) {
            return
        }

        tts?.let { engine ->
            if (engine.isSpeaking) {
                engine.stop() // 如果正在說話，先停止
            }
            // 呼叫 speak 方法，並檢查結果
            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            if (result == TextToSpeech.ERROR) {
                Log.e("Tts", "Error speaking text: \"$text\"")
            } else {
                Log.d("Tts", "Successfully queued speech for: \"$text\"")
            }
        } ?: run {
            currentState = TtsState.ERROR
        }
    }

    public fun shutdown() {
        Log.d("Tts", "Shutting down TextToSpeech engine.")
        tts?.apply {
            stop()
            shutdown()
        }
        tts = null
        currentState = TtsState.INITIALIZING
    }
}