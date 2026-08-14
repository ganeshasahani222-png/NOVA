package com.nova.assistant.ai

import kotlinx.coroutines.delay

/**
 * Local, no-network implementation of [AiEngine]. Useful for UI
 * development, screenshots, and tests before a real API key/backend
 * is wired up. Swap for [RemoteAiEngine] (or your own implementation)
 * in NovaContainer when ready.
 */
class StubAiEngine : AiEngine {
    override suspend fun generateResponse(prompt: String, history: List<String>): AiResult {
        delay(400) // simulate latency so the UI's loading state is exercised
        return AiResult.Success(
            "You said: \"$prompt\". (This is a placeholder reply — connect a real " +
                "AiEngine implementation in NovaContainer to get live AI responses.)"
        )
    }
}
