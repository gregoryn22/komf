package snd.komf.mediaserver.metadata

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SafeProviderCallTest {

    private val cloudflareBody = """
        <!DOCTYPE html><html lang="en-US"><head><title>Just a moment...</title></head>
        <body><script>window._cf_chl_opt = {};</script>
        <iframe src="https://challenges.cloudflare.com/turnstile"></iframe></body></html>
    """.trimIndent()

    /** Mimics what a real provider's HTTP call does when BookWalker returns a 403 Cloudflare page. */
    private fun cloudflareException(): ClientRequestException {
        val client = HttpClient(MockEngine {
            respond(
                content = cloudflareBody,
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }) { expectSuccess = true }

        return runBlocking {
            assertFailsWith<ClientRequestException> {
                client.get("https://bookwalker.com/migration/search/?word=Country%20of%20Dolls")
            }
        }
    }

    private fun resultProvider(name: CoreProviders, onCall: () -> Unit = {}): MetadataProvider =
        FakeProvider(name) {
            onCall()
            listOf(
                SeriesSearchResult(
                    url = "https://example.test/$name",
                    title = "$name result",
                    provider = name,
                    resultId = "$name-1",
                )
            )
        }

    @Test
    fun `one provider failing with 403 does not abort the others`() = runBlocking {
        val failing = FakeProvider(CoreProviders.BOOK_WALKER) { throw cloudflareException() }

        var malCalled = false
        var anilistCalled = false
        val mal = resultProvider(CoreProviders.MAL) { malCalled = true }
        val anilist = resultProvider(CoreProviders.ANILIST) { anilistCalled = true }

        // Replicates MetadataService.searchSeriesMetadata aggregation over providers.
        val results = listOf(failing, mal, anilist)
            .flatMap { safeProviderSearch(it, "Country of Dolls") }

        assertTrue(malCalled, "providers after the failed one must still be called")
        assertTrue(anilistCalled, "providers after the failed one must still be called")
        assertEquals(2, results.size, "failed provider contributes no results, others remain")
        assertEquals(
            setOf(CoreProviders.MAL, CoreProviders.ANILIST),
            results.map { it.provider }.toSet(),
        )
    }

    @Test
    fun `failing provider returns empty results instead of throwing`() = runBlocking {
        val failing = FakeProvider(CoreProviders.BOOK_WALKER) { throw cloudflareException() }
        val results = safeProviderSearch(failing, "Country of Dolls")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `cancellation is not swallowed`() = runBlocking {
        val provider = FakeProvider(CoreProviders.BOOK_WALKER) {
            throw CancellationException("cancelled")
        }
        assertFailsWith<CancellationException> {
            safeProviderSearch(provider, "anything")
        }
        Unit
    }

    @Test
    fun `describeProviderError flags cloudflare 403`() = runBlocking {
        val message = describeProviderError(cloudflareException())
        assertTrue(message.contains("403"), "status code should be reported: $message")
        assertTrue(message.contains("Cloudflare", ignoreCase = true), "cloudflare challenge should be reported: $message")
        assertTrue(!message.contains("<html"), "raw challenge HTML body should not be dumped: $message")
    }

    private class FakeProvider(
        private val name: CoreProviders,
        private val onSearch: suspend () -> Collection<SeriesSearchResult>,
    ) : MetadataProvider {
        override fun providerName(): CoreProviders = name
        override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> = onSearch()
        override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata =
            error("not used in test")
        override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? = error("not used in test")
        override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata =
            error("not used in test")
        override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? =
            error("not used in test")
    }
}
