package com.amr.feature.episode.presentation

import com.amr.core.domain.model.Episode

/** A row of the episode list: a season header or an episode. */
internal sealed interface EpisodeListRow {
    /** Stable lazy key; episodes keep their id, headers get a distinct string key. */
    val key: Any

    data class SeasonHeader(val season: Int?, val run: Int) : EpisodeListRow {
        override val key: Any get() = "season_${season ?: "other"}_$run"
        val title: String get() = season?.let { "Musim $it" } ?: "Lainnya"
    }

    data class Item(val episode: Episode) : EpisodeListRow {
        override val key: Any get() = episode.id
    }
}

private val SeasonCode = Regex("""^S(\d+)E\d+""", RegexOption.IGNORE_CASE)

/** Season number from an episode code such as "S01E01"; null when the code has another shape. */
internal fun seasonOf(code: String): Int? =
    SeasonCode.find(code.trim())?.groupValues?.get(1)?.toIntOrNull()

/**
 * Inserts a header wherever the season changes, keeping the list order. Built
 * from the whole loaded list (not per page), so a season spanning two pages gets
 * one header. [EpisodeListRow.SeasonHeader.run] counts how often a season has started, which
 * keeps keys unique even if the source order ever revisits a season.
 */
internal fun seasonRows(episodes: List<Episode>): List<EpisodeListRow> {
    val rows = ArrayList<EpisodeListRow>(episodes.size + 8)
    val runs = HashMap<Int?, Int>()
    var current: Int? = null
    var first = true
    for (episode in episodes) {
        val season = seasonOf(episode.code)
        if (first || season != current) {
            val run = runs[season] ?: 0
            runs[season] = run + 1
            rows += EpisodeListRow.SeasonHeader(season, run)
            current = season
            first = false
        }
        rows += EpisodeListRow.Item(episode)
    }
    return rows
}
