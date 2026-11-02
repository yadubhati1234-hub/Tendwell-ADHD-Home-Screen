package app.tendwell.search

import java.text.Normalizer
import java.util.Locale

/** A search result: the item plus which part of its label matched, for highlighting. */
data class SearchHit<T>(val item: T, val label: String, val matchStart: Int, val matchEnd: Int) {
    val hasMatch: Boolean get() = matchEnd > matchStart
}

/** A run of apps under one letter header in the full list. */
data class LetterGroup<T>(val letter: String, val hits: List<SearchHit<T>>)

/**
 * Pure search logic, kept free of Android types so it can be unit tested.
 *
 * Matching ignores case and accents ("cafe" finds "Café"). Results are ranked:
 * label starts with the query, then a word in the label starts with it, then
 * the query appears anywhere. Within a rank, the input order (alphabetical)
 * is kept, so the top result is stable and predictable for "press Enter to open".
 */
object AppSearch {

    fun <T> search(items: List<T>, query: String, label: (T) -> String): List<SearchHit<T>> {
        val q = fold(query.trim())
        if (q.isEmpty()) return items.map { SearchHit(it, label(it), 0, 0) }

        val ranked = ArrayList<Pair<Int, SearchHit<T>>>()
        for (item in items) {
            val text = label(item)
            val folded = fold(text)
            // fold() keeps one char per input char, so indices map straight back.
            val at = folded.indexOf(q)
            if (at < 0) continue
            val rank = when {
                at == 0 -> 0
                wordStartIndex(folded, q) >= 0 -> 1
                else -> 2
            }
            val start = if (rank == 1) wordStartIndex(folded, q) else at
            ranked += rank to SearchHit(item, text, start, start + q.length)
        }
        // sortedBy is stable, so alphabetical order survives inside each rank.
        return ranked.sortedBy { it.first }.map { it.second }
    }

    /** Groups an alphabetical list under first-letter headers. Digits and symbols go under "#". */
    fun <T> groupByLetter(hits: List<SearchHit<T>>): List<LetterGroup<T>> {
        val groups = LinkedHashMap<String, MutableList<SearchHit<T>>>()
        for (hit in hits) {
            groups.getOrPut(letterOf(hit.label)) { mutableListOf() } += hit
        }
        return groups.map { (letter, list) -> LetterGroup(letter, list) }
    }

    fun letterOf(label: String): String {
        val first = fold(label.trim()).firstOrNull() ?: return "#"
        return if (first.isLetter()) first.uppercase(Locale.getDefault()) else "#"
    }

    private fun wordStartIndex(folded: String, q: String): Int {
        var from = 0
        while (true) {
            val at = folded.indexOf(q, from)
            if (at < 0) return -1
            if (at == 0 || !folded[at - 1].isLetterOrDigit()) return at
            from = at + 1
        }
    }

    /**
     * Lower-cases and strips accents one character at a time, so the result
     * has exactly the same length as the input and match indices line up.
     */
    internal fun fold(s: String): String {
        val out = StringBuilder(s.length)
        for (ch in s) {
            val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)
                .firstOrNull { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
                ?: ch
            out.append(base.lowercaseChar())
        }
        return out.toString()
    }
}
