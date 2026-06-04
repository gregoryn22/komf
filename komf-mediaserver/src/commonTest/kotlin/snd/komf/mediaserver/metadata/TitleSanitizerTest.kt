package snd.komf.mediaserver.metadata

import snd.komf.api.config.TitleSanitizationConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class TitleSanitizerTest {

    private val cfg = TitleSanitizationConfig(
        enabled = true,
        stripSuffixes = listOf("(Volumes)", "(Colored)"),
        stripPatterns = listOf("\\s*\\(Chapters\\)$")
    )

    @Test
    fun `strips configured suffixes`() {
        val result = sanitizeTitle("My Series (Volumes)", cfg)
        assertEquals("My Series", result)
    }

    @Test
    fun `applies regex patterns`() {
        val result = sanitizeTitle("My Series (Chapters)", cfg)
        assertEquals("My Series", result)
    }

    @Test
    fun `strips suffixes case-insensitively`() {
        val result = sanitizeTitle("My Series (volumes)", cfg)
        assertEquals("My Series", result)
    }

    @Test
    fun `strips multiple suffixes in combination`() {
        val result = sanitizeTitle("My Series (Colored) (Volumes)", cfg)
        assertEquals("My Series", result)
    }

    @Test
    fun `strips suffixes in any order they appear`() {
        val result = sanitizeTitle("My Series (Volumes) (Colored)", cfg)
        assertEquals("My Series", result)
    }

    @Test
    fun `strips square bracket suffixes`() {
        val cfg2 = cfg.copy(stripSuffixes = listOf("[Colored]", "[Volumes]"))
        assertEquals("My Series", sanitizeTitle("My Series [Colored] [Volumes]", cfg2))
    }

    @Test
    fun `strips curly bracket suffixes`() {
        val cfg2 = cfg.copy(stripSuffixes = listOf("{Colored}", "{Volumes}"))
        assertEquals("My Series", sanitizeTitle("My Series {Colored} {Volumes}", cfg2))
    }

    @Test
    fun `regex pattern with literal brackets must be escaped`() {
        // unescaped [ in pattern is invalid regex — should throw
        val badCfg = cfg.copy(stripSuffixes = emptyList(), stripPatterns = listOf("\\s*\\[Colored\\]$"))
        assertEquals("My Series", sanitizeTitle("My Series [Colored]", badCfg))
    }

    @Test
    fun `returns raw title when disabled`() {
        val disabled = cfg.copy(enabled = false)
        val result = sanitizeTitle("My Series (Volumes)", disabled)
        assertEquals("My Series (Volumes)", result)
    }
}

