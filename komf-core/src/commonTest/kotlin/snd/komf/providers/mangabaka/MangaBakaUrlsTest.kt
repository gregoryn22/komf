package snd.komf.providers.mangabaka

import snd.komf.mangabaka.model.MangaBakaSeriesId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MangaBakaUrlsTest {

    @Test
    fun `parses site page url with slug`() {
        assertEquals(
            MangaBakaSeriesId(602380),
            parseMangaBakaSeriesUrl("https://mangabaka.org/manga/602380/Salamander")
        )
    }

    @Test
    fun `parses short url komf writes into series links`() {
        assertEquals(MangaBakaSeriesId(602380), parseMangaBakaSeriesUrl("https://mangabaka.org/602380"))
    }

    @Test
    fun `tolerates missing scheme, www, trailing slash, query and whitespace`() {
        val expected = MangaBakaSeriesId(602380)
        assertEquals(expected, parseMangaBakaSeriesUrl("mangabaka.org/manga/602380"))
        assertEquals(expected, parseMangaBakaSeriesUrl("http://www.mangabaka.org/manga/602380/"))
        assertEquals(expected, parseMangaBakaSeriesUrl("https://mangabaka.org/manga/602380?tab=links"))
        assertEquals(expected, parseMangaBakaSeriesUrl("  https://MangaBaka.org/602380  "))
    }

    @Test
    fun `plain titles are not treated as urls`() {
        assertNull(parseMangaBakaSeriesUrl("86"))
        assertNull(parseMangaBakaSeriesUrl("1984"))
        assertNull(parseMangaBakaSeriesUrl("Salamander"))
    }

    @Test
    fun `rejects other hosts and non series paths`() {
        assertNull(parseMangaBakaSeriesUrl("https://anilist.co/manga/602380"))
        assertNull(parseMangaBakaSeriesUrl("https://notmangabaka.org/602380"))
        assertNull(parseMangaBakaSeriesUrl("https://mangabaka.org/manga/"))
        assertNull(parseMangaBakaSeriesUrl("https://mangabaka.org/search?q=602380"))
    }

    @Test
    fun `rejects ids that overflow`() {
        assertNull(parseMangaBakaSeriesUrl("https://mangabaka.org/99999999999999999999"))
    }
}
