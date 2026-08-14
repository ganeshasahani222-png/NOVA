package com.nova.assistant.data

import java.util.UUID

enum class Sender { USER, NOVA, SYSTEM }

/**
 * Immutable model for a single turn in the conversation.
 * Kept independent of any UI framework so it can be reused by the
 * chat screen, voice pipeline, and (later) a persistence layer.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)
