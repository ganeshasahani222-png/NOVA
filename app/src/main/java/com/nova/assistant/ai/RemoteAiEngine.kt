package com.nova.assistant.ai

import com.nova.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class RemoteAiEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) : AiEngine {

    private val apiKey: String get() = BuildConfig.GEMINI_API_KEY
    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    override suspend fun generateResponse(prompt: String, history: List<String>): AiResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext AiResult.Failure("No Gemini API key configured.")
            }

            try {
                val body = buildRequestBody(prompt)
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext AiResult.Failure(
                            "Gemini request failed with code " + response.code + " body: " + responseBody.take(300)
                        )
                    }
                    val text = parseResponseText(responseBody)
                    AiResult.Success(text)
                }
            } catch (e: IOException) {
                AiResult.Failure("Network error")
            } catch (e: Exception) {
                AiResult.Failure("Unexpected error")
            }
        }

    private fun buildRequestBody(prompt: String): JSONObject {
        val contents = JSONArray()
        val message = JSONObject()
        message.put("role", "user")
        val partsArray = JSONArray()
        val part = JSONObject()
        part.put("text", prompt)
        partsArray.put(part)
        message.put("parts", partsArray)
        contents.put(message)

        val root = JSONObject()
        root.put("contents", contents)
        return root
    }

    private fun parseResponseText(rawBody: String): String {
        val fallback = "Sorry, I did not understand the response."
        return try {
            val json = JSONObject(rawBody)
            val candidates = json.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            fallback
        }
    }
}
