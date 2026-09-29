package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpokenWordsTests {
    private val quirks = RecognizerQuirks(allowances = mapOf("heir" to listOf("air"), "O" to listOf("oh")))

    @Test
    fun `a word said as written is faithful`() {
        val checked = SpokenWords.check(expected = listOf("love"), heard = listOf("love"), quirks = RecognizerQuirks.none)
        assertEquals(setOf(0), checked.faithful)
    }

    @Test
    fun `a similar word is paired but not faithful`() {
        val checked = SpokenWords.check(expected = listOf("love"), heard = listOf("dove"), quirks = RecognizerQuirks.none)
        assertEquals(1, checked.matches.size)
        assertTrue(checked.faithful.isEmpty())
    }

    @Test
    fun `an elision spelled out is faithful without a patch`() {
        val checked = SpokenWords.check(expected = listOf("tatter’d"), heard = listOf("tattered"), quirks = RecognizerQuirks.none)
        assertEquals(setOf(0), checked.faithful)
    }

    @Test
    fun `a patched word is paired by the patch and then accepted by it`() {
        val checked = SpokenWords.check(expected = listOf("heir"), heard = listOf("air"), quirks = quirks)
        assertEquals(1, checked.matches.size)
        assertEquals(setOf(0), checked.faithful)
    }

    @Test
    fun `without the table the same pair is not even put together`() {
        val checked = SpokenWords.check(expected = listOf("heir"), heard = listOf("air"), quirks = RecognizerQuirks.none)
        assertTrue(checked.faithful.isEmpty())
    }

    @Test
    fun `a word nothing was heard for is neither paired nor faithful`() {
        val checked = SpokenWords.check(expected = listOf("love", "is"), heard = listOf("love"), quirks = RecognizerQuirks.none)
        assertEquals(setOf(0), checked.faithful)
    }

    @Test
    fun `the reader's word checks are that same pass`() {
        val tracker = SpokenLineTracker(line = "O heir of love", quirks = quirks)
        val progress = tracker.progress(heard = "oh air of dove")
        val checked =
            SpokenWords.check(
                expected = listOf("O", "heir", "of", "love"),
                heard = listOf("oh", "air", "of", "dove"),
                quirks = quirks,
            )
        val faithful = progress.checks.indices.filter { progress.checks[it] == WordCheck.CORRECT }
        val byCheck =
            checked.matches.indices
                .filter { it in checked.faithful }
                .flatMap { checked.matches[it].expected }
        assertEquals(byCheck.sorted(), faithful)
    }
}
