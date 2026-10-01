package snd.komf.providers.mangaupdates

import snd.komf.providers.mangaupdates.model.SearchResult
import snd.komf.providers.mangaupdates.model.SearchResultHit
import snd.komf.util.NameSimilarityMatcher.Companion.nameSimilarityMatcher
import snd.komf.util.NameSimilarityMatcher.NameMatchingMode.CLOSEST_MATCH
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MangaUpdatesMatchTest {
    private val matcher = nameSimilarityMatcher(CLOSEST_MATCH)

    private fun hit(id: Long, title: String, hitTitle: String?) = SearchResultHit(
        record = SearchResult(
            id = id,
            title = title,
            description = null,
            image = null,
            genres = null,
            year = null,
            url = "",
        ),
        hitTitle = hitTitle,
    )

    // shaped after a real search for "Attack on Titan"
    private val attackOnTitanResults = listOf(
        hit(1, "Shingeki no Kyojin", "Attack on Titan"),
        hit(2, "Shingeki no Kyojin dj - Addendum", "Attack on Titan dj - Addendum"),
    )

    @Test
    fun `matches on primary title`() {
        assertEquals(1, findMatch("Shingeki no Kyojin", attackOnTitanResults, matcher)?.id)
    }

    @Test
    fun `matches english name through hit title`() {
        assertEquals(1, findMatch("Attack on Titan", attackOnTitanResults, matcher)?.id)
    }

    @Test
    fun `hit title tolerates small punctuation differences`() {
        val results = listOf(hit(3, "Sousou no Frieren", "Frieren: Beyond Journey’s End"))
        assertEquals(3, findMatch("Frieren: Beyond Journey's End", results, matcher)?.id)
    }

    @Test
    fun `primary title match is preferred over earlier hit title match`() {
        val results = listOf(
            hit(4, "Something Else", "Blue Period"),
            hit(5, "Blue Period", "Blue Period"),
        )
        assertEquals(5, findMatch("Blue Period", results, matcher)?.id)
    }

    @Test
    fun `no match`() {
        assertNull(findMatch("Completely Different", attackOnTitanResults, matcher))
    }
}
