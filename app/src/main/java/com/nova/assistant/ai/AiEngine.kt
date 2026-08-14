package com.nova.assistant.ai

/**
 * Abstraction over "whatever generates Nova's replies".
 *
 * Keeping this as an interface means the rest of the app (chat UI,
 * voice pipeline) never needs to know whether responses come from a
 * remote LLM API, an on-device model, or a canned stub used for
 * development/testing. Swap implementations in NovaContainer without
 * touching any other module.
 */
interface AiEngine {
    /**
     * Returns Nova's reply to [prompt]. [history] is provided so a real
     * implementation can maintain conversational context; implementations
     * are free to ignore it (e.g. StubAiEngine does).
     */
    suspend fun generateResponse(prompt: String, history: List<String> = emptyList()): AiResult
}

sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Failure(val message: String) : AiResult
}
