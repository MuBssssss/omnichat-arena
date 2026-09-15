package com.omnichat.arena.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerplexityParserTest {

    @Test
    fun parseLine_cumulativeChunks_extractsDeltasSequentially() {
        // Event 1: First chunk
        val payload1 = """{"blocks":[{"markdown_block":{"chunks":["Hello"]}}]}"""
        val result1 = PerplexityParser.parseLine(payload1, "")
        assertTrue(result1 is PerplexityParser.ParseResult.TextDelta)
        val delta1 = result1 as PerplexityParser.ParseResult.TextDelta
        assertEquals("Hello", delta1.delta)
        assertEquals("Hello", delta1.newAccumulated)

        // Event 2: Cumulative update
        val payload2 = """{"blocks":[{"markdown_block":{"chunks":["Hello world"]}}]}"""
        val result2 = PerplexityParser.parseLine(payload2, delta1.newAccumulated)
        assertTrue(result2 is PerplexityParser.ParseResult.TextDelta)
        val delta2 = result2 as PerplexityParser.ParseResult.TextDelta
        assertEquals(" world", delta2.delta)
        assertEquals("Hello world", delta2.newAccumulated)

        // Event 3: Third cumulative chunk
        val payload3 = """{"blocks":[{"markdown_block":{"chunks":["Hello world!"]}}]}"""
        val result3 = PerplexityParser.parseLine(payload3, delta2.newAccumulated)
        assertTrue(result3 is PerplexityParser.ParseResult.TextDelta)
        val delta3 = result3 as PerplexityParser.ParseResult.TextDelta
        assertEquals("!", delta3.delta)
        assertEquals("Hello world!", delta3.newAccumulated)
    }

    @Test
    fun parseLine_multipleChunksInOneEvent_combinesAllNewText() {
        val payload = """{"blocks":[{"markdown_block":{"chunks":["Hello", "Hello world", "Hello world!"]}}]}"""
        val result = PerplexityParser.parseLine(payload, "")
        assertTrue(result is PerplexityParser.ParseResult.TextDelta)
        val delta = result as PerplexityParser.ParseResult.TextDelta
        assertEquals("Hello world!", delta.delta)
        assertEquals("Hello world!", delta.newAccumulated)
    }

    @Test
    fun parseLine_duplicateChunk_suppressesDuplicate() {
        val payload = """{"blocks":[{"markdown_block":{"chunks":["Hello world"]}}]}"""
        val result = PerplexityParser.parseLine(payload, "Hello world")
        assertTrue(result is PerplexityParser.ParseResult.NoDelta)
    }

    @Test
    fun parseLine_authWallEvent_detectsFraudAuthwallUpsell() {
        val payload = """{"upsell_information":{"name":"fraud_authwall_upsell"}}"""
        val result = PerplexityParser.parseLine(payload, "Hello")
        assertTrue(result is PerplexityParser.ParseResult.AuthWall)
    }

    @Test
    fun parseLine_nonCumulativeChunk_appendsCorrectly() {
        val payload = """{"blocks":[{"markdown_block":{"chunks":[" and goodbye"]}}]}"""
        val result = PerplexityParser.parseLine(payload, "Hello")
        assertTrue(result is PerplexityParser.ParseResult.TextDelta)
        val delta = result as PerplexityParser.ParseResult.TextDelta
        assertEquals(" and goodbye", delta.delta)
        assertEquals("Hello and goodbye", delta.newAccumulated)
    }

    @Test
    fun parseLine_emptyAndMalformedPayloads_returnsNoDeltaResiliently() {
        assertEquals(PerplexityParser.ParseResult.NoDelta, PerplexityParser.parseLine("{}", ""))
        assertEquals(PerplexityParser.ParseResult.NoDelta, PerplexityParser.parseLine("""{"blocks":[]}""", ""))
        assertEquals(PerplexityParser.ParseResult.NoDelta, PerplexityParser.parseLine("""{"blocks":[{"markdown_block":{"chunks":[""]}}]}""", ""))
        assertEquals(PerplexityParser.ParseResult.NoDelta, PerplexityParser.parseLine("invalid json payload", "accumulated"))
    }
}
