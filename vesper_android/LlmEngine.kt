package com.lookupproduction.vesper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object LlmEngine {

    private const val LLM_API_URL = "http://127.0.0.1:8080/completion"

    // Core system persona
    private const val BASE_SYSTEM_PROMPT = """
You are Vesper, the Specialized Production Arm for Look Up Production Studios.
You are an expert in private independent film pacing, model release legalities, and high-intensity voiceover generation.
Your output formatting must cater to mobile shorts (9:16) or feature scenes. Keep responses concise and professional.
"""

    // Tools injected based on Gatekeeper state
    private const val TOOLS_PROMPT_OFFLINE = """
You have the following tools available. To use a tool, reply ONLY with a JSON block in this exact format: {"tool": "TOOL_NAME", "args": "ARGUMENTS"}.
Available Tools:
1. render: Executes FFmpeg commands to render video. (No arguments needed).
2. legal: Generates a model release form. Arguments MUST be in format "Name YYYY-MM-DD".
"""

    private const val TOOLS_PROMPT_ONLINE = """
$TOOLS_PROMPT_OFFLINE
3. research: Performs web research. Arguments MUST be the search query.
"""

    suspend fun queryLlm(prompt: String, isOnline: Boolean, chatHistory: List<String>): LlmResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL(LLM_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000 // 15 seconds to connect to local server
            connection.readTimeout = 60000    // 60 seconds to wait for generation

            val systemPrompt = if (isOnline) BASE_SYSTEM_PROMPT + TOOLS_PROMPT_ONLINE else BASE_SYSTEM_PROMPT + TOOLS_PROMPT_OFFLINE

            // Construct a basic prompt template (assuming a standard instruction-tuned model format like Llama-3 or Mistral)
            val fullPromptBuilder = StringBuilder()
            fullPromptBuilder.append("<|system|>\n$systemPrompt\n")

            // Add a few recent messages for context
            val recentHistory = chatHistory.takeLast(6)
            for (msg in recentHistory) {
                if (msg.startsWith("You:")) {
                    fullPromptBuilder.append("<|user|>\n${msg.removePrefix("You: ")}\n")
                } else if (msg.startsWith("Vesper:") || msg.startsWith("System:")) {
                    fullPromptBuilder.append("<|model|>\n${msg}\n")
                }
            }

            fullPromptBuilder.append("<|user|>\n$prompt\n<|model|>\n")

            val jsonBody = JSONObject()
            jsonBody.put("prompt", fullPromptBuilder.toString())
            jsonBody.put("n_predict", 256)
            jsonBody.put("temperature", 0.3)
            jsonBody.put("stop", JSONArray(listOf("<|user|>", "<|system|>")))

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseString)
                val textOutput = jsonResponse.getString("content").trim()

                return@withContext parseResponse(textOutput)
            } else {
                return@withContext LlmResponse.Text("Error: Local AI server returned code $responseCode. Is the Termux server running?")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext LlmResponse.Text("Error connecting to Vesper Core. Please ensure the local Termux AI server is running at http://127.0.0.1:8080.")
        }
    }

    private fun parseResponse(text: String): LlmResponse {
        // Look for JSON tool commands in the output
        try {
            // Very naive JSON extraction (assumes the model followed strict JSON output for tools)
            val startIndex = text.indexOf("{")
            val endIndex = text.lastIndexOf("}")
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                val jsonStr = text.substring(startIndex, endIndex + 1)
                val json = JSONObject(jsonStr)
                if (json.has("tool")) {
                    val tool = json.getString("tool")
                    val args = if (json.has("args")) json.getString("args") else ""
                    return LlmResponse.ToolCall(tool, args)
                }
            }
        } catch (e: Exception) {
            // Not JSON or invalid JSON, treat as text
        }

        return LlmResponse.Text(text)
    }
}

sealed class LlmResponse {
    data class Text(val message: String) : LlmResponse()
    data class ToolCall(val toolName: String, val args: String) : LlmResponse()
}
