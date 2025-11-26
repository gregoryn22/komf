package snd.komf.mediaserver.metadata

import kotlinx.coroutines.runBlocking
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerBookMetadataUpdate
import snd.komf.mediaserver.model.MediaServerBookThumbnail
import snd.komf.mediaserver.model.MediaServerLibrary
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeries
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerSeriesMetadata
import snd.komf.mediaserver.model.MediaServerSeriesMetadataUpdate
import snd.komf.mediaserver.model.MediaServerSeriesThumbnail
import snd.komf.mediaserver.model.MediaServerThumbnailId
import snd.komf.mediaserver.model.Page
import snd.komf.model.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UnmatchedTagHandlerTest {

    @Test
    fun `adds unmatched tag when missing`() = runBlocking {
        val client = RecordingMediaServerClient()
        val handler = UnmatchedTagHandler(client, "unmatched")
        val series = series(tags = listOf("existing"))

        handler.markUnmatched(series)

        assertEquals(listOf("existing", "unmatched"), client.lastUpdate?.tags)
    }

    @Test
    fun `does nothing when unmatched tag name is blank`() = runBlocking {
        val client = RecordingMediaServerClient()
        val handler = UnmatchedTagHandler(client, " ")

        handler.markUnmatched(series())

        assertNull(client.lastUpdate)
    }

    @Test
    fun `removes unmatched tag when present`() = runBlocking {
        val client = RecordingMediaServerClient()
        val handler = UnmatchedTagHandler(client, "unmatched")
        val series = series(tags = listOf("keep", "unmatched"))

        val updatedSeries = handler.clearUnmatched(series)

        assertEquals(listOf("keep"), client.lastUpdate?.tags)
        assertEquals(listOf("keep"), updatedSeries.metadata.tags)
    }

    @Test
    fun `skips removal when tag is locked`() = runBlocking {
        val client = RecordingMediaServerClient()
        val handler = UnmatchedTagHandler(client, "unmatched")
        val series = series(tags = listOf("unmatched"), tagsLock = true)

        val updatedSeries = handler.clearUnmatched(series)

        assertNull(client.lastUpdate)
        assertEquals(series, updatedSeries)
    }

    private fun series(tags: List<String> = emptyList(), tagsLock: Boolean = false) = MediaServerSeries(
        id = MediaServerSeriesId("series-id"),
        libraryId = MediaServerLibraryId("library-id"),
        name = "Series",
        booksCount = 0,
        metadata = MediaServerSeriesMetadata(
            status = SeriesStatus.ONGOING,
            title = "Series",
            titleSort = "Series",
            alternativeTitles = emptyList(),
            summary = "",
            readingDirection = null,
            publisher = null,
            alternativePublishers = emptySet(),
            ageRating = null,
            language = null,
            genres = emptyList(),
            tags = tags,
            totalBookCount = null,
            authors = emptyList(),
            releaseYear = null,
            links = emptyList(),
            statusLock = false,
            titleLock = false,
            titleSortLock = false,
            alternativeTitlesLock = false,
            summaryLock = false,
            readingDirectionLock = false,
            publisherLock = false,
            ageRatingLock = false,
            languageLock = false,
            genresLock = false,
            tagsLock = tagsLock,
            totalBookCountLock = false,
            authorsLock = false,
            releaseYearLock = false,
            linksLock = false,
        ),
        url = "",
        deleted = false,
    )

    private class RecordingMediaServerClient : MediaServerClient {
        var lastUpdate: MediaServerSeriesMetadataUpdate? = null

        override suspend fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries {
            error("Not used")
        }

        override suspend fun getSeries(libraryId: MediaServerLibraryId, pageNumber: Int): Page<MediaServerSeries> {
            error("Not used")
        }

        override suspend fun getSeriesThumbnail(seriesId: MediaServerSeriesId) = null

        override suspend fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail> {
            error("Not used")
        }

        override suspend fun getBook(bookId: MediaServerBookId): MediaServerBook {
            error("Not used")
        }

        override suspend fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook> {
            error("Not used")
        }

        override suspend fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail> {
            error("Not used")
        }

        override suspend fun getBookThumbnail(bookId: MediaServerBookId) = null

        override suspend fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary {
            error("Not used")
        }

        override suspend fun getLibraries(): List<MediaServerLibrary> {
            error("Not used")
        }

        override suspend fun updateSeriesMetadata(seriesId: MediaServerSeriesId, metadata: MediaServerSeriesMetadataUpdate) {
            lastUpdate = metadata
        }

        override suspend fun deleteSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnailId: MediaServerThumbnailId) {
            error("Not used")
        }

        override suspend fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate) {
            error("Not used")
        }

        override suspend fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId) {
            error("Not used")
        }

        override suspend fun resetBookMetadata(bookId: MediaServerBookId, bookName: String, bookNumber: Int?) {
            error("Not used")
        }

        override suspend fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String) {
            error("Not used")
        }

        override suspend fun uploadSeriesThumbnail(
            seriesId: MediaServerSeriesId,
            thumbnail: snd.komf.model.Image,
            selected: Boolean,
            lock: Boolean
        ) = null

        override suspend fun uploadBookThumbnail(
            bookId: MediaServerBookId,
            thumbnail: snd.komf.model.Image,
            selected: Boolean,
            lock: Boolean
        ) = null

        override suspend fun refreshMetadata(libraryId: MediaServerLibraryId, seriesId: MediaServerSeriesId) {
            error("Not used")
        }
    }
}
