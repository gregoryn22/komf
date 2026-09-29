package snd.komf.mediaserver.metadata

import snd.komf.api.config.TitleSanitizationConfig

fun sanitizeTitle(raw: String, config: TitleSanitizationConfig): String {
    if (!config.enabled) return raw
    
    // Trim first to catch trailing whitespace before suffix removal
    var result = raw.trim()
    
    // 1) Strip explicit suffixes
    var prev: String
    do {
        prev = result
        config.stripSuffixes.forEach { suffix ->
            if (suffix.isNotBlank() && result.trimEnd().endsWith(suffix, ignoreCase = true)) {
                result = result.trimEnd().dropLast(suffix.length)
            }
        }
    } while (result != prev)

    // 2) Apply regex patterns (ignore empty patterns)
    config.stripPatterns.forEach { pattern ->
        if (pattern.isNotBlank()) {
            result = result.replace(Regex(pattern), "")
        }
    }
    
    return result.trim()
}


/**
 * A library-level `titleSanitization` block replaces the default one entirely, so `enabled: true` with no
 * lists silently strips nothing. Inherit the default lists when the library defines none of its own.
 */
fun TitleSanitizationConfig.inheritingFrom(default: TitleSanitizationConfig): TitleSanitizationConfig {
    if (!enabled || stripSuffixes.isNotEmpty() || stripPatterns.isNotEmpty()) return this
    return copy(stripSuffixes = default.stripSuffixes, stripPatterns = default.stripPatterns)
}
