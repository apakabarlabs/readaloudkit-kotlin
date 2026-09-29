package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadmeTests {
    private val printedLines = listOf("From fairest creatures", "we desire increase")

    @Test
    fun `a transcript that says every word is complete`() {
        val tracker = SpokenLineTracker(lines = printedLines, quirks = RecognizerQuirks.none)

        assertTrue(tracker.progress(heard = "From fairest creatures we desire increase").isComplete)
    }

    @Test
    fun `a dropped word is not in the matches, so only isComplete shows it`() {
        val tracker = SpokenLineTracker(lines = printedLines, quirks = RecognizerQuirks.none)
        val transcript = "From fairest creatures desire increase"
        val check =
            SpokenWords.check(
                expected = tracker.expected,
                heard = transcript.split(" "),
                quirks = RecognizerQuirks.none,
            )

        assertEquals(check.matches.size, check.faithful.size)
        assertFalse(tracker.progress(heard = transcript).isComplete)
    }
}
