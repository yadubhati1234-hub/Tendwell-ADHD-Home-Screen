package app.tendwell.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    private val apps = listOf("Calculator", "Calendar", "Camera", "Google Maps", "Keep Notes", "Maps", "Messages", "Music", "YouTube Music", "Café Finder", "7-Eleven")
        .sorted()

    private fun names(query: String) = AppSearch.search(apps, query) { it }.map { it.item }

    @Test
    fun emptyQueryReturnsEverythingInOrderWithoutHighlight() {
        val hits = AppSearch.search(apps, "   ") { it }
        assertEquals(apps, hits.map { it.item })
        assertTrue(hits.none { it.hasMatch })
    }

    @Test
    fun matchingIgnoresCase() {
        assertEquals(listOf("Calculator", "Calendar"), names("CAL"))
    }

    @Test
    fun prefixMatchesRankAboveWordMatchesAboveInnerMatches() {
        // "Maps" starts with it, "Google Maps" has a word starting with it.
        assertEquals(listOf("Maps", "Google Maps"), names("map"))
        // "Music" (prefix), then "YouTube Music" (word start); nothing matches only inside a word.
        assertEquals(listOf("Music", "YouTube Music"), names("mus"))
        // "ssa" only appears inside a word.
        assertEquals(listOf("Messages"), names("ssa"))
    }

    @Test
    fun highlightPointsAtTheMatchedWord() {
        val hit = AppSearch.search(apps, "notes") { it }.single()
        assertEquals("Keep Notes", hit.label)
        assertEquals("Notes", hit.label.substring(hit.matchStart, hit.matchEnd))
    }

    @Test
    fun accentsAreIgnoredAndHighlightStaysAligned() {
        val hit = AppSearch.search(apps, "cafe") { it }.single()
        assertEquals("Café", hit.label.substring(hit.matchStart, hit.matchEnd))
    }

    @Test
    fun noMatchGivesEmptyList() {
        assertTrue(names("zzz").isEmpty())
    }

    @Test
    fun groupsByFirstLetterWithSymbolsUnderHash() {
        val groups = AppSearch.groupByLetter(AppSearch.search(apps, "") { it })
        assertEquals("#", groups.first().letter)
        assertEquals(listOf("7-Eleven"), groups.first().hits.map { it.item })
        val c = groups.first { it.letter == "C" }
        assertEquals(listOf("Café Finder", "Calculator", "Calendar", "Camera"), c.hits.map { it.item })
    }
}
