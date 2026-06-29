package snd.komf.util

import snd.komf.model.BookRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookNameParserTest {

    @Test
    fun `getBookNumber parses number at end of string`() {
        assertEquals(BookRange(123.0), BookNameParser.getBookNumber("Batman 123"))
    }

    @Test
    fun `getBookNumber parses hash-prefixed number`() {
        assertEquals(BookRange(5.0), BookNameParser.getBookNumber("Spider-Man #5"))
    }

    @Test
    fun `getBookNumber parses Issue prefix`() {
        assertEquals(BookRange(42.0), BookNameParser.getBookNumber("Issue 42"))
    }

    @Test
    fun `getBookNumber parses Volume prefix`() {
        assertEquals(BookRange(3.0), BookNameParser.getBookNumber("Volume 3"))
    }

    @Test
    fun `getBookNumber parses number with parenthetical suffix`() {
        assertEquals(BookRange(10.0), BookNameParser.getBookNumber("Series 10 (2020)"))
    }

    @Test
    fun `getBookNumber parses dash-delimited number`() {
        assertEquals(
            BookRange(195.0),
            BookNameParser.getBookNumber("Suske En Wiske HQ - 195 - De Hippe Heksen")
        )
    }

    @Test
    fun `getBookNumber parses dash-delimited leading zeros`() {
        assertEquals(
            BookRange(1.0),
            BookNameParser.getBookNumber("The Walking Dead - 001 - Days Gone Bye")
        )
    }

    @Test
    fun `getBookNumber parses dash-delimited decimal number`() {
        assertEquals(
            BookRange(12.5),
            BookNameParser.getBookNumber("Series - 12.5 - Special Edition")
        )
    }

    @Test
    fun `getBookNumber parses dash-delimited range`() {
        assertEquals(
            BookRange(1.0, 5.0),
            BookNameParser.getBookNumber("Series - 1-5 - Omnibus")
        )
    }

    @Test
    fun `getBookNumber prefers existing regex over dash-delimited`() {
        // "Issue 5" should match the Issue regex, not the dash pattern
        assertEquals(BookRange(5.0), BookNameParser.getBookNumber("Series - 2099 - Issue 5"))
    }

    @Test
    fun `getBookNumber returns null for unrecognized format`() {
        assertNull(BookNameParser.getBookNumber("No Number Here"))
    }

    // --- getVolumes ---

    @Test
    fun `getVolumes parses Vol dot no space`() {
        assertEquals(BookRange(5.0), BookNameParser.getVolumes("Vol.5"))
    }

    @Test
    fun `getVolumes parses Vol dot with space`() {
        assertEquals(BookRange(5.0), BookNameParser.getVolumes("Vol. 5"))
    }

    @Test
    fun `getVolumes parses lowercase vol dot`() {
        assertEquals(BookRange(5.0), BookNameParser.getVolumes("vol.5"))
    }

    @Test
    fun `getVolumes parses bare v prefix`() {
        assertEquals(BookRange(5.0), BookNameParser.getVolumes("v5"))
    }

    @Test
    fun `getVolumes parses t prefix with leading zero`() {
        assertEquals(BookRange(5.0), BookNameParser.getVolumes("t05"))
    }

    @Test
    fun `getVolumes parses volume range`() {
        assertEquals(BookRange(1.0, 3.0), BookNameParser.getVolumes("v1-3"))
    }

    @Test
    fun `getVolumes returns null for unrecognized format`() {
        assertNull(BookNameParser.getVolumes("No Volume Here"))
    }

    // --- getChapters ---

    @Test
    fun `getChapters parses Ch dot no space`() {
        assertEquals(BookRange(5.0), BookNameParser.getChapters("Ch.5"))
    }

    @Test
    fun `getChapters parses Ch dot with space`() {
        assertEquals(BookRange(5.0), BookNameParser.getChapters("Ch. 5"))
    }

    @Test
    fun `getChapters parses bare c prefix`() {
        assertEquals(BookRange(5.0), BookNameParser.getChapters("c5"))
    }

    @Test
    fun `getChapters parses chapter word`() {
        assertEquals(BookRange(5.0), BookNameParser.getChapters("chapter 5"))
    }

    @Test
    fun `getChapters parses ep prefix`() {
        assertEquals(BookRange(5.0), BookNameParser.getChapters("ep5"))
    }

    @Test
    fun `getChapters parses chapter range`() {
        assertEquals(BookRange(5.0, 7.0), BookNameParser.getChapters("Ch.5-7"))
    }

    @Test
    fun `getChapters does not false-positive on mid-word c`() {
        // PR #11 tightened the chapter prefix to (?:^|\s); a bare 'c' inside a
        // word (here the 'c' in "Sonic2") must not be read as a chapter marker.
        assertNull(BookNameParser.getChapters("Sonic2"))
    }

    @Test
    fun `getChapters returns null for unrecognized format`() {
        assertNull(BookNameParser.getChapters("No Chapter Here"))
    }

    // --- PR #11 documented edge behaviors (current intended behavior) ---

    @Test
    fun `getVolumes does not match parenthesized volume`() {
        // PR #11 dropped the optional '(' + leading-space prefix from the bare
        // v/t branch, so '(v5)' no longer matches. Locked as intended.
        assertNull(BookNameParser.getVolumes("(v5)"))
    }

    @Test
    fun `getChapters does not match parenthesized chapter`() {
        // Same as above: '(Ch.5)' no longer matches after the prefix change.
        assertNull(BookNameParser.getChapters("(Ch.5)"))
    }

    @Test
    fun `getVolumes matches underscore-separated vol dot`() {
        // Underscore was added to the vol. branch prefix.
        assertEquals(BookRange(5.0), BookNameParser.getVolumes("Title_vol.5"))
    }

    @Test
    fun `getVolumes does not match underscore-separated bare v`() {
        // Underscore was NOT added to the bare v/t branch, so 'Title_v5' misses.
        assertNull(BookNameParser.getVolumes("Title_v5"))
    }
}
