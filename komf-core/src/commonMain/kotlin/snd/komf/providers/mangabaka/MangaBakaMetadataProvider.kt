package snd.komf.providers.mangabaka

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.reactivecircus.cache4k.Cache
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import snd.komf.mangabaka.model.MangaBakaSeries
import snd.komf.mangabaka.model.MangaBakaSeriesId
import snd.komf.mangabaka.model.MangaBakaType
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.MediaType
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataProvider
import snd.komf.util.NameSimilarityMatcher
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

class MangaBakaMetadataProvider(
    private val dataSource: MangaBakaDataSource,
    private val metadataMapper: MangaBakaMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val coverFetchClient: HttpClient?,
    mediaType: MediaType,
) : MetadataProvider {
    private val typeExcludes: List<MangaBakaType>? = when (mediaType) {
        MediaType.MANGA -> listOf(MangaBakaType.NOVEL)
        else -> null
    }
    private val typeIncludes: List<MangaBakaType>? = when (mediaType) {
        MediaType.MANGA -> null
        MediaType.NOVEL -> listOf(MangaBakaType.NOVEL)
        MediaType.COMIC -> listOf(MangaBakaType.OEL, MangaBakaType.OTHER)
        MediaType.WEBTOON -> listOf(MangaBakaType.MANHUA, MangaBakaType.MANHWA)
    }

    private val cache = Cache.Builder<MangaBakaSeriesId, MangaBakaSeries>()
        .expireAfterWrite(30.minutes)
        .build()

    override fun providerName() = CoreProviders.MANGA_BAKA

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val id = seriesId.toMangaBakaId()
        val series = cache.get(id) { dataSource.getSeries(id) }
        val cover = fetchCover(series)

        return metadataMapper.toSeriesMetadata(series, cover)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val id = seriesId.toMangaBakaId()
        val series = cache.get(id) { dataSource.getSeries(id) }
        return fetchCover(series)
    }

    override suspend fun getBookMetadata(
        seriesId: ProviderSeriesId,
        bookId: ProviderBookId
    ): ProviderBookMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun searchSeries(
        seriesName: String,
        limit: Int
    ): Collection<SeriesSearchResult> {
        parseMangaBakaSeriesUrl(seriesName)?.let { return searchById(it) }

        val results = dataSource.search(
            title = seriesName,
            types = typeIncludes,
            typesNot = typeExcludes,
        )
        results.forEach { cache.put(it.id, it) }

        return results.take(limit).map { metadataMapper.toSeriesSearchResult(it) }
    }

    // a pasted series url is an explicit pick, so media type filters are intentionally not applied
    private suspend fun searchById(id: MangaBakaSeriesId): Collection<SeriesSearchResult> {
        val series = try {
            cache.get(id) { dataSource.getSeries(id) }
        } catch (e: ClientRequestException) {
            if (e.response.status != HttpStatusCode.NotFound) throw e
            null
        } catch (e: NoSuchElementException) {
            // local database has no row for this id
            null
        }

        if (series == null) {
            logger.warn { "MangaBaka series $id not found" }
            return emptyList()
        }
        return listOf(metadataMapper.toSeriesSearchResult(series))
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = dataSource.search(
            title = seriesName.take(400),
            types = typeIncludes,
            typesNot = typeExcludes
        )
        searchResults.forEach { cache.put(it.id, it) }

        val match = searchResults.firstOrNull { series ->
            val titles = series.titles?.map { it.title } ?: emptyList()
            nameMatcher.matches(seriesName, titles)
        }

        return match?.let { series -> metadataMapper.toSeriesMetadata(series, fetchCover(series)) }
    }

    private suspend fun fetchCover(series: MangaBakaSeries): Image? {
        if (coverFetchClient == null || series.cover.x350?.x1 == null) return null
        val url = series.cover.x350.x1
        return try {
            val response = coverFetchClient.get(url)
            Image(
                response.body(),
                response.contentType()?.let { "${it.contentType}/${it.contentSubtype}" }
            )
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                logger.warn { "Cover image not found for series ${series.id} (${e.response.status}), continuing without cover" }
                null
            } else throw e
        }
    }

    private fun ProviderSeriesId.toMangaBakaId() = MangaBakaSeriesId(this.value.toLong())
}
