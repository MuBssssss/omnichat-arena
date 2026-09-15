package com.omnichat.arena.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiParserTest {

    @Test
    fun parseNonStreamResponse_normalResponse_returnsSuccessWithContent() {
        val payload = """{"id":"chatcmpl-redacted","choices":[{"index":0,"message":{"role":"assistant","content":"This is a valid non-streaming response."},"finish_reason":"stop"}]}"""
        val result = OpenAiParser.parseNonStreamResponse(payload)
        assertTrue(result is OpenAiParser.NonStreamResult.Success)
        val success = result as OpenAiParser.NonStreamResult.Success
        assertEquals("This is a valid non-streaming response.", success.content)
    }

    @Test
    fun parseNonStreamResponse_emptyChoices_returnsError() {
        val payload = """{"id":"chatcmpl-redacted","choices":[]}"""
        val result = OpenAiParser.parseNonStreamResponse(payload)
        assertTrue(result is OpenAiParser.NonStreamResult.Error)
    }

    @Test
    fun parseNonStreamResponse_emptyContent_returnsError() {
        val payload = """{"choices":[{"message":{"role":"assistant","content":""}}]}"""
        val result = OpenAiParser.parseNonStreamResponse(payload)
        assertTrue(result is OpenAiParser.NonStreamResult.Error)
    }

    @Test
    fun parseNonStreamResponse_blankContent_returnsError() {
        val payload = """{"choices":[{"message":{"role":"assistant","content":"   \n\t  "}}]}"""
        val result = OpenAiParser.parseNonStreamResponse(payload)
        assertTrue(result is OpenAiParser.NonStreamResult.Error)
    }

    @Test
    fun parseNonStreamResponse_missingMessage_returnsError() {
        val payload = """{"choices":[{"index":0}]}"""
        val result = OpenAiParser.parseNonStreamResponse(payload)
        assertTrue(result is OpenAiParser.NonStreamResult.Error)
    }

    @Test
    fun parseNonStreamResponse_malformedJson_returnsError() {
        val payload = "<html><head><title>502 Bad Gateway</title></head><body>502 Gateway error</body></html>"
        val result = OpenAiParser.parseNonStreamResponse(payload)
        assertTrue(result is OpenAiParser.NonStreamResult.Error)
    }

    @Test
    fun parseNonStreamResponse_budgetMarkerReached_returnsBudgetExhausted() {
        val payload = """{"choices":[{"message":{"role":"assistant","content":"Warning: this key has reached its budget for this cycle."}}]}"""
        val result = OpenAiParser.parseNonStreamResponse(payload)
        assertTrue(result is OpenAiParser.NonStreamResult.BudgetExhausted)
    }

    @Test
    fun parseNonStreamResponse_budgetMarkerRaiseKey_returnsBudgetExhausted() {
        val payload = """{"choices":[{"message":{"role":"assistant","content":"Please raise the key budget to proceed with inference."}}]}"""
        val result = OpenAiParser.parseNonStreamResponse(payload)
        assertTrue(result is OpenAiParser.NonStreamResult.BudgetExhausted)
    }

    @Test
    fun parseNonStreamResponse_budgetMarkerAgentKey_returnsBudgetExhausted() {
        val payload = """{"choices":[{"message":{"role":"assistant","content":"agent_key_budget exceeded on free tier endpoint."}}]}"""
        val result = OpenAiParser.parseNonStreamResponse(payload)
        assertTrue(result is OpenAiParser.NonStreamResult.BudgetExhausted)
    }

    @Test
    fun parseStreamDelta_normalDelta_returnsToken() {
        val payload = """{"choices":[{"delta":{"content":"streaming token"}}]}"""
        val result = OpenAiParser.parseStreamDelta(payload, "")
        assertTrue(result is OpenAiParser.StreamDeltaResult.Token)
        val token = result as OpenAiParser.StreamDeltaResult.Token
        assertEquals("streaming token", token.text)
    }

    @Test
    fun parseStreamDelta_emptyDelta_returnsEmptyOrNoDelta() {
        val payload = """{"choices":[{"delta":{}}]}"""
        val result = OpenAiParser.parseStreamDelta(payload, "")
        assertTrue(result is OpenAiParser.StreamDeltaResult.EmptyOrNoDelta)
    }

    @Test
    fun parseStreamDelta_budgetMarkerSpannedAcrossChunks_returnsBudgetExhausted() {
        val accumulated = "System notification: key has reached its "
        val payload = """{"choices":[{"delta":{"content":"budget for today."}}]}"""
        val result = OpenAiParser.parseStreamDelta(payload, accumulated)
        assertTrue(result is OpenAiParser.StreamDeltaResult.BudgetExhausted)
    }

    @Test
    fun parseStreamDelta_malformedJson_returnsEmptyOrNoDelta() {
        val result = OpenAiParser.parseStreamDelta("not a valid json line", "accumulated")
        assertTrue(result is OpenAiParser.StreamDeltaResult.EmptyOrNoDelta)
    }
}
