package snd.komf.mediaserver.metadata

import io.github.oshai.kotlinlogging.KotlinLogging
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.model.MediaServerSeries
import snd.komf.mediaserver.model.MediaServerSeriesMetadataUpdate

private val logger = KotlinLogging.logger {}

class UnmatchedTagHandler(
    private val mediaServerClient: MediaServerClient,
    private val unmatchedTagName: String?,
) {

    suspend fun markUnmatched(series: MediaServerSeries) {
        val tagName = unmatchedTagName?.takeIf { it.isNotBlank() } ?: return
        if (series.metadata.tagsLock) {
            logger.warn { "Cannot add unmatched tag to series ${series.id} because tags are locked" }
            return
        }
        if (series.metadata.tags.contains(tagName)) return

        val tags = series.metadata.tags + tagName
        runCatching {
            mediaServerClient.updateSeriesMetadata(series.id, MediaServerSeriesMetadataUpdate(tags = tags.toList()))
        }.onFailure { logger.warn(it) { "Failed to add unmatched tag to series ${series.id}" } }
    }

    suspend fun clearUnmatched(series: MediaServerSeries): MediaServerSeries {
        val tagName = unmatchedTagName?.takeIf { it.isNotBlank() } ?: return series
        if (tagName !in series.metadata.tags) return series
        if (series.metadata.tagsLock) {
            logger.warn { "Cannot remove unmatched tag from series ${series.id} because tags are locked" }
            return series
        }

        val tags = series.metadata.tags.filterNot { it == tagName }
        return runCatching {
            mediaServerClient.updateSeriesMetadata(series.id, MediaServerSeriesMetadataUpdate(tags = tags))
            series.copy(metadata = series.metadata.copy(tags = tags))
        }.onFailure {
            logger.warn(it) { "Failed to remove unmatched tag from series ${series.id}" }
        }.getOrDefault(series)
    }
}
