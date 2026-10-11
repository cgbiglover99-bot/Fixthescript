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
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

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
                        onManageStorageClick = { requestManageStoragePermission() },
                        onSendTermuxCommand = { command, isOnline ->
                            handleTermuxCommand(command, isOnline)
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
        super.onDestroy()
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun handleTermuxCommand(command: String, isOnline: Boolean) {
        val parts = command.trim().split(" ", limit = 2)
        val action = parts.getOrNull(0)?.lowercase() ?: return
        val query = parts.getOrNull(1) ?: ""

        var scriptName: String? = null
        var args: Array<String> = emptyArray()

        when (action) {
            "research" -> {
                scriptName = "vesper_research.py"
                args = arrayOf(query, if (isOnline) "online" else "offline")
            }
            "render" -> {
                val intent = Intent(this, RenderService::class.java)
                startService(intent)
                scriptName = "vesper_render.py"
                args = arrayOf()
            }
            "legal" -> {
                scriptName = "vesper_legal.py"
                // Expecting query format: "John Doe 1990-01-01"
                val legalParts = query.split(Regex("(?=\\d{4}-\\d{2}-\\d{2})"), limit = 2)
                val name = legalParts.getOrNull(0)?.trim() ?: "Unknown"
                val dob = legalParts.getOrNull(1)?.trim() ?: "1900-01-01"
                args = arrayOf(name, dob)
            }
        }

        scriptName?.let {
            val scriptPath = "/sdcard/LookUpProductionStudios/Scripts/$it"
            TermuxBridge.executePythonScript(this, scriptPath, args)
            speak("Executing $action command.")
        } ?: run {
            speak("Command not recognized. Please use research, render, or legal.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesperUI(
    onManageStorageClick: () -> Unit,
    onSendTermuxCommand: (String, Boolean) -> Unit
) {
    var isGatekeeperOnline by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<String>()) }

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

        // Input Area
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

            // Microphone Button Placeholder
            Button(onClick = {
                chatHistory = chatHistory + "System: (Voice Recording...)"
                // STT logic would go here
            }) {
                Text("🎤")
            }

            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (inputText.isNotBlank()) {
                    chatHistory = chatHistory + "You: $inputText"
                    onSendTermuxCommand(inputText, isGatekeeperOnline)
                    chatHistory = chatHistory + "Vesper: Processing command..."
                    inputText = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}
