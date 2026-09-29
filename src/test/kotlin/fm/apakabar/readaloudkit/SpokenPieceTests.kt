package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun `a piece is right only when every word of every line was said`() {
        val tracker = SpokenLineTracker(lines = quatrain)
        val said = quatrain.joinToString(" ")

        assertTrue(tracker.progress(heard = said).isComplete)
        assertFalse(tracker.progress(heard = quatrain[0]).isComplete)
    }

    @Test
    fun `a slip anywhere in the piece fails the piece`() {
        val tracker = SpokenLineTracker(lines = quatrain)
        val said = quatrain.joinToString(" ").replace("riper", "ripest")

        val progress = tracker.progress(heard = said)
        assertFalse(progress.isComplete)
        assertEquals(1, progress.checks.count { it != WordCheck.CORRECT })
        assertTrue(WordCheck.CLOSE in progress.checks)
        assertEquals(
            listOf(WordAttempt(word = "riper", check = WordCheck.CLOSE)),
            tracker.attempts(progress).filter { it.check != WordCheck.CORRECT },
        )
    }

    @Test
    fun `word attempts use the passage spelling and include correct words`() {
        val tracker = SpokenLineTracker(line = "Love is not love")
        val progress = tracker.progress(heard = "glove is love")

        assertEquals(
            listOf(
                WordAttempt(word = "Love", check = WordCheck.CLOSE),
                WordAttempt(word = "is", check = WordCheck.CORRECT),
                WordAttempt(word = "not", check = WordCheck.WRONG),
                WordAttempt(word = "love", check = WordCheck.CORRECT),
            ),
            tracker.attempts(progress),
        )
    }

    @Test
    fun `the states come back apart line by line`() {
        val tracker = SpokenLineTracker(lines = quatrain)
        val progress = tracker.progress(heard = quatrain.joinToString(" "))
        val states = progress.wordStates

        assertEquals(listOf(6, 7, 8, 7), tracker.lineLengths)
        for ((index, length) in tracker.lineLengths.withIndex()) {
            assertEquals(length, tracker.wordStates(states, forLineAt = index).size)
        }
        assertTrue(tracker.wordStates(states, forLineAt = 4).isEmpty())
    }

    @Test
    fun `a line on its own is a piece of one line`() {
        val tracker = SpokenLineTracker(line = quatrain[0])
        assertEquals(listOf(6), tracker.lineLengths)
        assertTrue(tracker.progress(heard = quatrain[0]).isComplete)
    }
}
