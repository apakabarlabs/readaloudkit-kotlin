package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpokenPieceTests {
    private val quatrain =
        listOf(
            "From fairest creatures we desire increase,",
            "That thereby beauty's rose might never die,",
            "But as the riper should by time decease,",
            "His tender heir might bear his memory:",
        )

    @Test
    fun `the states come back apart line by line`() {
        val tracker =
            SpokenLineTracker(
                lines = quatrain,
                quirks = RecognizerQuirks.none,
                elisions = Elisions.none,
                tokenizer = Cases.sonnetsTokenizer,
            )
        val progress = tracker.progress(heard = quatrain.joinToString(" "))
        val states = progress.wordStates

        for ((index, length) in tracker.lineLengths.withIndex()) {
            assertEquals(length, tracker.wordStates(states, forLineAt = index).size)
        }
        assertTrue(tracker.wordStates(states, forLineAt = 4).isEmpty())
    }

    @Test
    fun `a check shows as the state a reader sees`() {
        val progress = SpokenLineTracker.Progress(listOf(WordCheck.CORRECT, WordCheck.CLOSE, WordCheck.WRONG))

        assertEquals(listOf(WordReadingState.SAID, WordReadingState.CLOSE, WordReadingState.MISSED), progress.wordStates)
    }

    @Test
    fun `a tracker keeps the tokenizer it was made with`() {
        val plain = WordTokenizer(interiorMarks = "-")
        val tracker = SpokenLineTracker(line = "beauty's rose", quirks = RecognizerQuirks.none, elisions = Elisions.none, tokenizer = plain)

        assertSame(plain, tracker.tokenizer)
    }

    @Test
    fun `a tracker keeps the elisions it was made with`() {
        val listed = Elisions(mapOf("tatter’d" to "tattered"))
        val tracker =
            SpokenLineTracker(
                line = "a tatter’d weed",
                quirks = RecognizerQuirks.none,
                elisions = listed,
                tokenizer = Cases.sonnetsTokenizer,
            )

        assertSame(listed, tracker.elisions)
    }
}
