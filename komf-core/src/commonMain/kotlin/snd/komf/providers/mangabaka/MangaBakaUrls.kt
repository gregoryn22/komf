package snd.komf.providers.mangabaka

import snd.komf.mangabaka.model.MangaBakaSeriesId

// matches both the site's page url (https://mangabaka.org/manga/602380/Salamander)
// and the short form komf writes into series links (https://mangabaka.org/602380)
private val seriesUrlRegex = Regex(
    """^(?:https?://)?(?:www\.)?mangabaka\.org/(?:manga/)?(\d+)(?:[/?#].*)?$""",
    RegexOption.IGNORE_CASE
)

fun parseMangaBakaSeriesUrl(text: String): MangaBakaSeriesId? {
    val id = seriesUrlRegex.matchEntire(text.trim())?.groupValues?.get(1) ?: return null
    return id.toLongOrNull()?.let { MangaBakaSeriesId(it) }
}
