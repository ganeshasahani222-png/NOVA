package com.nova.assistant.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChatMessageTest {
    @Test
    fun `each message gets a unique id by default`() {
        val a = ChatMessage(sender = Sender.USER, text = "hi")
        val b = ChatMessage(sender = Sender.USER, text = "hi")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `preserves provided sender and text`() {
        val message = ChatMessage(sender = Sender.NOVA, text = "hello there")
        assertEquals(Sender.NOVA, message.sender)
        assertEquals("hello there", message.text)
    }
}
