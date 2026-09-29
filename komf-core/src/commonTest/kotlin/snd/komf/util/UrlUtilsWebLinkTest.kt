package snd.komf.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UrlUtilsWebLinkTest {

    @Test
    fun webLinkFromExternalUrl_truncatedPercentEncoding_fallsBackToRawUrl() {
        val bad = "https://example.com/%E3%82%BD%E3%83%"
        val link = webLinkFromExternalUrl(bad)
        assertNotNull(link)
        assertEquals("example.com", link.label)
        // Ktor 3.6+ parses leniently and re-encodes the stray '%'; older versions fell back to the raw string
        assertEquals(true, link.url == bad || link.url == "$bad" + "25")
    }

    @Test
    fun webLinkFromExternalUrl_noScheme_returnsNull() {
        assertNull(webLinkFromExternalUrl("not-a-url/path%E3%"))
    }
}
