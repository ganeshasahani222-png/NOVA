package com.nova.assistant.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Skeleton for a real LLM-backed [AiEngine].
 *
 * This class deliberately does NOT ship with a hardcoded API key or a
 * fixed provider. Wire it up by:
 *   1. Supplying [apiKeyProvider] — read the key from the Android
 *      Keystore, EncryptedSharedPreferences, or a backend you control.
 *      Never hardcode a key in source or commit one to version control.
 *   2. Setting [endpoint] and [buildRequestBody] to match whichever
 *      API you integrate (Anthropic, OpenAI, your own backend, etc).
 *
 * The request/response shapes below are illustrative placeholders —
 * update them to match the real API's documented schema before use.
 */
class RemoteAiEngine(
    private val endpoint: String,
    private val apiKeyProvider: () -> String?,
    private val client: OkHttpClient = OkHttpClient()
) : AiEngine {

    override suspend fun generateResponse(prompt: String, history: List<String>): AiResult =
        withContext(Dispatchers.IO) {
            val apiKey = apiKeyProvider()
                ?: return@withContext AiResult.Failure("No API key configured.")

            try {
                val body = buildRequestBody(prompt, history)
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext AiResult.Failure(
                            "AI request failed: HTTP ${response.code}"
                        )
                    }
                    val text = parseResponseText(response.body?.string().orEmpty())
                    AiResult.Success(text)
                }
            } catch (e: IOException) {
                AiResult.Failure("Network error: ${e.message}")
            }
        }

    /** Override/replace to match the target API's request schema. */
    private fun buildRequestBody(prompt: String, history: List<String>): JSONObject =
        JSONObject().apply {
            put("prompt", prompt)
            put("history", JSONArray(history))
        }

    /** Override/replace to match the target API's response schema. */
    private fun parseResponseText(rawBody: String): String {
        return runCatching {
            JSONObject(rawBody).optString("text", rawBody)
        }.getOrDefault(rawBody)
    }
}
