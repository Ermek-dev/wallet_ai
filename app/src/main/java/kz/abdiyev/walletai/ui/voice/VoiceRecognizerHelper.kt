package kz.abdiyev.walletai.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class VoiceRecognizerHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (Int) -> Unit = {},
    private val onListeningChanged: (Boolean) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isAvailable()) {
            onError(SpeechRecognizer.ERROR_CLIENT)
            return
        }
        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            speechRecognizer = it
            it.setRecognitionListener(createListener())
        }
        recognizer.startListening(createIntent())
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        onListeningChanged(false)
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        onListeningChanged(false)
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onListeningChanged(true)
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            onListeningChanged(false)
        }

        override fun onError(error: Int) {
            onListeningChanged(false)
            this@VoiceRecognizerHelper.onError(error)
        }

        override fun onResults(results: Bundle?) {
            onListeningChanged(false)
            val matches = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!matches.isNullOrBlank()) {
                onResult(matches)
            } else {
                this@VoiceRecognizerHelper.onError(SpeechRecognizer.ERROR_NO_MATCH)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun createIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ru", "RU").toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    companion object {
        fun userMessageForError(errorCode: Int): String = when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH -> "Не расслышал — попробуйте ещё раз"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Тишина — повторите фразу"
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Нет сети для распознавания"
            SpeechRecognizer.ERROR_AUDIO -> "Ошибка микрофона"
            SpeechRecognizer.ERROR_CLIENT -> "Распознавание недоступно"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нужен доступ к микрофону"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознавание занято — подождите"
            else -> "Ошибка голосового ввода"
        }
    }
}
