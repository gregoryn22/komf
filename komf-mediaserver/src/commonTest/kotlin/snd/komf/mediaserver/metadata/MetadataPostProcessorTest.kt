package snd.komf.mediaserver.metadata

import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerBookMetadata
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.SeriesAndBookMetadata
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
import snd.komf.model.MediaType
import snd.komf.model.SeriesMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataPostProcessorTest {

    private fun book(name: String) = MediaServerBook(
        id = MediaServerBookId("1"),
        seriesId = MediaServerSeriesId("s1"),
        libraryId = MediaServerLibraryId("lib1"),
        seriesTitle = "Test Series",
        name = name,
        url = "/test",
        number = 1,
        oneshot = false,
        deleted = false,
        metadata = MediaServerBookMetadata(
            title = name,
            summary = null,
            number = "1",
            numberSort = "1.0",
            releaseDate = null,
            authors = emptyList(),
            tags = emptyList(),
            isbn = null,
            links = emptyList(),
            titleLock = false,
            summaryLock = false,
            numberLock = false,
            numberSortLock = false,
            releaseDateLock = false,
            authorsLock = false,
            tagsLock = false,
            isbnLock = false,
            linksLock = false,
        )
    )

    private fun processor(libraryType: MediaType) = MetadataPostProcessor(
        libraryType = libraryType,
        seriesTitle = false,
        seriesTitleLanguage = null,
        alternativeSeriesTitles = false,
        alternativeSeriesTitleLanguages = emptyList(),
        orderBooks = true,
        readingDirectionValue = null,
        languageValue = null,
        fallbackToAltTitle = false,
        scoreTagName = null,
        originalPublisherTagName = null,
        publisherTagNames = emptyList(),
    )

    private fun orderedNumber(libraryType: MediaType, name: String): BookMetadata? {
        val book = book(name)
        val input = SeriesAndBookMetadata(SeriesMetadata(), mapOf(book to BookMetadata()))
        return processor(libraryType).process(input).bookMetadata.getValue(book)
    }

    @Test
    fun `manga ordering prefers chapter over volume`() {
        // PR #10 regression: getChapters runs before getVolumes for MANGA, so a
        // name carrying both a volume and a chapter sorts on the chapter.
        val result = orderedNumber(MediaType.MANGA, "Title v01 c05")
        assertEquals(5.0, result?.numberSort)
        assertEquals(BookRange(5.0), result?.number)
    }

    @Test
    fun `manga ordering falls back to volume when no chapter`() {
        val result = orderedNumber(MediaType.MANGA, "Title v03")
        assertEquals(3.0, result?.numberSort)
        assertEquals(BookRange(3.0), result?.number)
    }
}
