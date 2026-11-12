package com.amr.feature.episode.presentation

import com.amr.core.domain.model.Episode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SeasonRowsTest {

    private fun ep(id: Int, code: String) = Episode(id, "Episode $id", "date", code)

    private fun headers(rows: List<EpisodeListRow>) =
        rows.filterIsInstance<EpisodeListRow.SeasonHeader>().map { it.title }

    @Test
    fun seasonOf_parsesCodes() {
        assertEquals(1, seasonOf("S01E01"))
        assertEquals(12, seasonOf("s12e03"))
        assertNull(seasonOf("Pilot"))
        assertNull(seasonOf(""))
    }

    @Test
    fun seasonSpanningTwoPages_getsOneHeader() {
        // Page 1 ends mid-season 2, page 2 continues it: one "Musim 2" header.
        fun code(season: Int, number: Int) = "S0${season}E${number.toString().padStart(2, '0')}"
        val page1 = (1..11).map { ep(it, code(1, it)) } + (12..20).map { ep(it, code(2, it - 11)) }
        val page2 = listOf(ep(21, "S02E10"), ep(22, "S03E01"))

        val rows = seasonRows(page1 + page2)

        assertEquals(listOf("Musim 1", "Musim 2", "Musim 3"), headers(rows))
    }

    @Test
    fun episodeOrder_isPreserved() {
        val episodes = listOf(ep(1, "S01E01"), ep(2, "S01E02"), ep(3, "S02E01"))

        val order = seasonRows(episodes).filterIsInstance<EpisodeListRow.Item>().map { it.episode.id }

        assertEquals(listOf(1, 2, 3), order)
    }

    @Test
    fun keys_areUnique_evenIfASeasonIsRevisited() {
        val rows = seasonRows(listOf(ep(1, "S01E01"), ep(2, "S02E01"), ep(3, "S01E02"), ep(4, "Special")))

        val keys = rows.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(listOf("Musim 1", "Musim 2", "Musim 1", "Lainnya"), headers(rows))
    }

    @Test
    fun empty_hasNoRows() {
        assertEquals(emptyList(), seasonRows(emptyList()))
    }
}
