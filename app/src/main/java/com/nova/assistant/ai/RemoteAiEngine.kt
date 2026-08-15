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

/**
 * AiEngine implementation backed by Google's Gemini API.
 * The API key is read from BuildConfig, which is generated at build
 * time from local.properties (locally) or a GitHub Actions secret
 * (in CI) — it is never hardcoded in source.
 */
class RemoteAiEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) : AiEngine {

    private val apiKey: String get() = BuildConfig.GEMINI_API_KEY
    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    override suspend fun generateResponse(prompt: String, history: List<String>): AiResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext AiResult.Failure("No Gemini API key configured.")
            }

            try {
                val body = buildRequestBody(prompt)
                val request = Request.Builder()
                    .url("$endpoint?key=$apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext AiResult.Failure(
                            "Gemini request failed: HTTP ${response.code} — ${responseBody.take(200)}"
                        )
                    }
                    val text = parseResponseText(responseBody)
                    AiResult.Success(text)
                }
            } catch (e: IOException) {
                AiResult.Failure("Network error: ${e.message}")
            } catch (e: Exception) {
                AiResult.Failure("Unexpected error: ${e.message}")
            }
        }

    private fun buildRequestBody(prompt: String): JSONObject {
        val contents = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            })
        }
        return JSONObject().apply {
            put("contents", contents)
        }
    }

    private fun parseResponseText(rawBody: String): String {
        return runCatching {
            val json = JSONObject(rawBody)
            val candidates = json.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        }.getOrDefault("Sorry, I
