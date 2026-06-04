package snd.komf.mediaserver.metadata

import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerBookMetadata
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataMapperTest {

    private fun book(numberLock: Boolean = false, numberSortLock: Boolean = false) = MediaServerBook(
        id = MediaServerBookId("1"),
        seriesId = MediaServerSeriesId("s1"),
        libraryId = MediaServerLibraryId("lib1"),
        seriesTitle = "Test Series",
        name = "Test Book",
        url = "/test",
        number = 1,
        oneshot = false,
        deleted = false,
        metadata = MediaServerBookMetadata(
            title = "Test Book",
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
            numberLock = numberLock,
            numberSortLock = numberSortLock,
            releaseDateLock = false,
            authorsLock = false,
            tagsLock = false,
            isbnLock = false,
            linksLock = false,
        )
    )

    private val bookMetadata = BookMetadata(number = BookRange(5.0))

    @Test
    fun `respectBookNumberLock=false always writes number`() {
        val mapper = MetadataMapper(respectBookNumberLock = false)
        val result = mapper.toBookMetadataUpdate(bookMetadata, null, book(numberLock = true))
        assertEquals("5", result.number)
        assertEquals(5.0, result.numberSort)
    }

    @Test
    fun `respectBookNumberLock=true skips number when locked`() {
        val mapper = MetadataMapper(respectBookNumberLock = true)
        val result = mapper.toBookMetadataUpdate(bookMetadata, null, book(numberLock = true, numberSortLock = true))
        assertNull(result.number)
        assertNull(result.numberSort)
    }

    @Test
    fun `respectBookNumberLock=true writes number when not locked`() {
        val mapper = MetadataMapper(respectBookNumberLock = true)
        val result = mapper.toBookMetadataUpdate(bookMetadata, null, book(numberLock = false, numberSortLock = false))
        assertEquals("5", result.number)
        assertEquals(5.0, result.numberSort)
    }

    @Test
    fun `respectBookNumberLock=true skips number only, not numberSort independently`() {
        val mapper = MetadataMapper(respectBookNumberLock = true)
        val resultNumberLocked = mapper.toBookMetadataUpdate(bookMetadata, null, book(numberLock = true, numberSortLock = false))
        assertNull(resultNumberLocked.number)
        assertEquals(5.0, resultNumberLocked.numberSort)

        val resultSortLocked = mapper.toBookMetadataUpdate(bookMetadata, null, book(numberLock = false, numberSortLock = true))
        assertEquals("5", resultSortLocked.number)
        assertNull(resultSortLocked.numberSort)
    }
}
