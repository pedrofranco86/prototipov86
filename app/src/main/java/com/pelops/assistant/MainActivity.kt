package com.pelops.assistant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import org.json.JSONArray
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var log: LinearLayout
    private lateinit var input: EditText
    private var tts: TextToSpeech? = null
    private val prefs by lazy { getSharedPreferences("pelops", MODE_PRIVATE) }
    private val history by lazy { JSONArray(prefs.getString("history", "[]")) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 24, 20, 12); setBackgroundColor(0xFFF4F6FA.toInt()) }
        val header = TextView(this).apply { text = "PELOPS  •  ASISTENTE"; textSize = 20f; setTextColor(0xFF18243A.toInt()); setPadding(0, 0, 0, 12) }
        root.addView(header)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val bubble = Button(this).apply { text = "Activar burbuja"; setOnClickListener { enableBubble() } }
        val voice = Button(this).apply { text = "Hablar"; setOnClickListener { listen() } }
        controls.addView(bubble, LinearLayout.LayoutParams(0, -2, 1f)); controls.addView(voice, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(controls)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        log = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 12, 0, 12) }
        scroll.addView(log)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        input = EditText(this).apply { hint = "Escribí o preguntá…"; maxLines = 3 }
        val send = Button(this).apply { text = "Enviar"; setOnClickListener { sendMessage() } }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(send)
        root.addView(row)
        setContentView(root)
        if (history.length() == 0) addMessage("Pelops", "Estoy disponible. Podés escribirme o tocar «Hablar». Esta versión inicial guarda el historial en el teléfono.")
        else for (i in 0 until history.length()) {
            val item = history.optJSONObject(i) ?: continue
            addBubble(item.optString("who"), item.optString("text"))
        }
    }

    private fun enableBubble() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Permití mostrar sobre otras aplicaciones y volvé a tocar Activar burbuja.", Toast.LENGTH_LONG).show()
        } else {
            startService(Intent(this, FloatingBubbleService::class.java))
            Toast.makeText(this, "Burbuja activada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        addMessage("Vos", message)
        input.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0)
        val reply = answerLocally(message)
        addMessage("Pelops", reply)
        tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "pelops_reply")
    }

    private fun answerLocally(message: String): String {
        val m = message.lowercase(Locale.getDefault())
        return when {
            m.contains("hola") || m.contains("buenas") -> "Hola. Estoy listo para ayudarte. ¿Qué querés resolver?"
            m.contains("quién sos") || m.contains("quien sos") -> "Soy Pelops, tu asistente en versión inicial. Puedo conversar por texto y voz, y mantener un historial local."
            m.contains("recordá") || m.contains("recorda") || m.contains("recordar") -> "Puedo conservar esta conversación en el teléfono. La memoria avanzada y la IA en la nube todavía no están conectadas."
            m.contains("ayuda") -> "Escribí tu consulta abajo. También podés tocar Hablar para dictarla o activar la burbuja flotante."
            else -> "Recibí tu mensaje: «$message». Esta versión ya tiene chat, voz y memoria local básica, pero todavía necesita conectar un servicio de IA para responder preguntas abiertas de forma inteligente."
        }
    }

    private fun addMessage(who: String, text: String) {
        addBubble(who, text)
        val arr = JSONArray(prefs.getString("history", "[]"))
        arr.put(org.json.JSONObject().put("who", who).put("text", text))
        while (arr.length() > 100) arr.remove(0)
        prefs.edit().putString("history", arr.toString()).apply()
    }

    private fun addBubble(who: String, text: String) {
        val tv = TextView(this).apply {
            this.text = "$who\n$text"
            textSize = 16f
            setTextColor(0xFF18243A.toInt())
            setPadding(14, 12, 14, 12)
            setBackgroundColor(if (who == "Vos") 0xFFDCEBFF.toInt() else 0xFFFFFFFF.toInt())
        }
        val lp = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 6, 0, 6) }
        log.addView(tv, lp)
    }

    private fun listen() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Hablale a Pelops")
            }
            startActivityForResult(intent, 77)
        } catch (_: Exception) {
            Toast.makeText(this, "El reconocimiento de voz no está disponible en este teléfono.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Android API, kept for broad device support")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 77 && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            input.setText(words?.firstOrNull() ?: "")
            input.setSelection(input.text.length)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale("es", "AR")
    }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}
