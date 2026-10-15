package com.lookupproduction.vesper

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lookupproduction.vesper.ui.theme.VesperTheme
import android.speech.tts.TextToSpeech
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.BroadcastReceiver
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.IntentFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    // Mutable state for the UI to observe speech results
    private var sttResult = mutableStateOf("")

    // Mutable state to hold incoming messages from Termux
    private var incomingMessage = mutableStateOf("")

    private val termuxReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("message") ?: return
            incomingMessage.value = message
            speak(message)
        }
    }

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val res = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = res?.get(0) ?: ""
            sttResult.value = spokenText
        }
    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a command...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            // Handle error, e.g. device doesn't support STT
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        // Register receiver for Termux callbacks
        val filter = IntentFilter("com.lookupproduction.vesper.TERMUX_CALLBACK")
        ContextCompat.registerReceiver(
            this,
            termuxReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Setup environment only if permission is already granted, otherwise do it later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            try {
                ScriptManager.setupEnvironment(this)
            } catch (e: Exception) {
                // Ignore initial setup failure, retry on button click
            }
        }

        setContent {
            VesperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VesperUI(
                        sttState = sttResult,
                        termuxState = incomingMessage,
                        onManageStorageClick = { requestManageStoragePermission() },
                        onStartMicClick = { startSpeechToText() },
                        onSendPrompt = { prompt, isOnline, history ->
                            processPrompt(prompt, isOnline, history)
                        }
                    )
                }
            }
        }
    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivity(intent)
                }
            } else {
                // Already granted, initialize environment
                try {
                    ScriptManager.setupEnvironment(this)
                } catch (e: Exception) {
                    // Log or show error
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale.US)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        unregisterReceiver(termuxReceiver)
        super.onDestroy()
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun processPrompt(prompt: String, isOnline: Boolean, history: List<String>) {
        lifecycleScope.launch {
            val response = LlmEngine.queryLlm(prompt, isOnline, history)

            when (response) {
                is LlmResponse.Text -> {
                    // Send text to UI via broadcast hack for simplicity (to reuse termuxState variable)
                    sendBroadcast(Intent("com.lookupproduction.vesper.TERMUX_CALLBACK").apply {
                        putExtra("message", response.message)
                    })
                }
                is LlmResponse.ToolCall -> {
                    handleTermuxTool(response.toolName, response.args, isOnline)
                }
            }
        }
    }

    private fun handleTermuxTool(action: String, query: String, isOnline: Boolean) {
        var scriptName: String? = null
        var args: Array<String> = emptyArray()

        when (action.lowercase()) {
            "research" -> {
                scriptName = "vesper_research.py"
                args = arrayOf(query, if (isOnline) "online" else "offline")
            }
            "render" -> {
                val intent = Intent(this, RenderService::class.java)
                startService(intent)
                scriptName = "vesper_render.py"
                args = emptyArray()
            }
            "legal" -> {
                scriptName = "vesper_legal.py"
                // Attempt simple extraction: "John Doe 1990-01-01"
                val legalParts = query.split(Regex("(?=\\d{4}-\\d{2}-\\d{2})"), limit = 2)
                val name = legalParts.getOrNull(0)?.trim() ?: "Unknown"
                val dob = legalParts.getOrNull(1)?.trim() ?: "1900-01-01"
                args = arrayOf(name, dob)
            }
        }

        scriptName?.let {
            val scriptPath = "/sdcard/LookUpProductionStudios/Scripts/$it"
            TermuxBridge.executePythonScript(this, scriptPath, args)
            speak("Executing $action tool.")
        } ?: run {
            speak("LLM attempted to call unknown tool: $action.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesperUI(
    sttState: MutableState<String>,
    termuxState: MutableState<String>,
    onManageStorageClick: () -> Unit,
    onStartMicClick: () -> Unit,
    onSendPrompt: (String, Boolean, List<String>) -> Unit
) {
    var isGatekeeperOnline by remember { mutableStateOf(false) }
    val chatHistory = remember { mutableStateListOf<String>() }

    // Append incoming termux messages to the chat history
    LaunchedEffect(termuxState.value) {
        if (termuxState.value.isNotBlank()) {
            chatHistory.add("System: ${termuxState.value}")
            termuxState.value = ""
        }
    }

    // System Prompt placeholder context
    val systemPrompt = "You are Vesper, the Specialized Production Arm for Look Up Production Studios. You are an expert in private independent film pacing, model release legalities, and high-intensity voiceover generation. Output formatting must cater to mobile shorts (9:16) or feature scenes."

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // Gatekeeper Toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isGatekeeperOnline) "🌐 ONLINE" else "🔒 NETWORK LOCKED",
                color = if (isGatekeeperOnline) Color.Green else Color.Red,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = isGatekeeperOnline,
                onCheckedChange = { isGatekeeperOnline = it }
            )
        }

        // Storage Permission Button (Debugging/Setup)
        Button(
            onClick = onManageStorageClick,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Grant 'All Files Access'")
        }

        // Chat Interface Area
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            LazyColumn {
                items(chatHistory) { message ->
                    Text(text = message, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }

        // Extracted Input Area for optimization
        VesperInputArea(
            sttState = sttState,
            onStartMicClick = onStartMicClick,
            onSendPrompt = { inputText ->
                chatHistory.add("You: $inputText")
                chatHistory.add("System: Thinking...")
                onSendPrompt(inputText, isGatekeeperOnline, chatHistory.toList())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesperInputArea(
    sttState: MutableState<String>,
    onStartMicClick: () -> Unit,
    onSendPrompt: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    // Automatically populate the text field if STT returns a result
    LaunchedEffect(sttState.value) {
        if (sttState.value.isNotBlank()) {
            inputText = sttState.value
            sttState.value = "" // clear it after copying
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Command Vesper...") }
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Microphone Button
        Button(onClick = onStartMicClick) {
            Text("🎤")
        }

        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = {
            if (inputText.isNotBlank()) {
                val currentInput = inputText
                inputText = ""
                onSendPrompt(currentInput)
            }
        }) {
            Text("Send")
        }
    }
}
