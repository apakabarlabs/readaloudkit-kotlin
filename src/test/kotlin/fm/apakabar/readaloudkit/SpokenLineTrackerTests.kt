package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpokenLineTrackerTests {
    private val tracker = SpokenLineTracker(line = "From fairest creatures we desire increase,")

    private val correct = WordCheck.CORRECT
    private val close = WordCheck.CLOSE
    private val wrong = WordCheck.WRONG

    @Test
    fun `a line said whole is complete`() {
        val progress = tracker.progress(heard = "from fairest creatures we desire increase")

        assertTrue(progress.checks.all { it == correct })
        assertTrue(progress.isComplete)
    }

    @Test
    fun `a line half said is a line not said`() {
        val progress = tracker.progress(heard = "from fairest creatures")

        assertEquals(listOf(correct, correct, correct), progress.checks.take(3))
        assertFalse(progress.isComplete)
        assertTrue(progress.wordStates.takeLast(3).all { it == WordReadingState.MISSED })
    }

    @Test
    fun `nothing heard yet is no progress and no words to fix`() {
        val progress = tracker.progress(heard = "")

        assertEquals(listOf(wrong, wrong, wrong, wrong, wrong, wrong), progress.checks)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `a reading nobody could make out is told from a reading that went wrong`() {
        assertTrue(tracker.progress(heard = "").isAllWrong)
        assertTrue(tracker.progress(heard = "the quick brown fox jumped over").isAllWrong)
        assertFalse(tracker.progress(heard = "from the quick brown fox jumped").isAllWrong)
        assertFalse(tracker.progress(heard = "from fairest creatures we desire increase").isAllWrong)
    }

    @Test
    fun `a short word one letter apart is still put opposite the word it answers`() {
        val line = SpokenLineTracker(line = "Th’ expense of spirit in a waste of shame")
        val progress = line.progress(heard = "the expense of spirit in a waste of shame")

        assertEquals(close, progress.checks[0])
        assertTrue(progress.checks.drop(1).all { it == correct })
    }

    @Test
    fun `a word spelt differently is a different word, however close it looks`() {
        val progress = tracker.progress(heard = "from farest creatures we desire increase")

        assertFalse(progress.isComplete)
        assertEquals(listOf(correct, close, correct, correct, correct, correct), progress.checks)
        assertEquals(WordReadingState.CLOSE, progress.wordStates[1])
    }

    @Test
    fun `a word said with an ending that is not there is wrong, however close it looks`() {
        val line = SpokenLineTracker(line = "But as the riper should by time decease,")
        val progress = line.progress(heard = "but as the ripers should by time decease")

        assertFalse(progress.isComplete)
        assertEquals(close, progress.checks[3])
        assertEquals(correct, progress.checks[4])
    }

    @Test
    fun `an elision the recogniser spells out in full counts as said`() {
        val line = SpokenLineTracker(line = "Will be a tatter’d weed of small worth held:")
        val progress = line.progress(heard = "will be a tattered weed of small worth held")

        assertTrue(progress.isComplete)
        assertEquals(correct, progress.checks[3])
    }

    @Test
    fun `a word patched for this recogniser passes as though it had been heard right`() {
        val line =
            SpokenLineTracker(
                line = "Will be a tatter’d weed of small worth held:",
                quirks = RecognizerQuirks(allowances = mapOf("tatter’d" to listOf("tattered"))),
            )
        val progress = line.progress(heard = "will be a tattered weed of small worth held")

        assertTrue(progress.isComplete)
    }

    @Test
    fun `two written words the recogniser runs into one are patched as the pair they make`() {
        val line =
            SpokenLineTracker(
                line = "For to thy sensual fault I bring in sense;",
                quirks = RecognizerQuirks(allowances = mapOf("in sense" to listOf("incense"))),
            )
        val progress = line.progress(heard = "for to thy sensual fault I bring incense")

        assertTrue(progress.isComplete)
    }

    @Test
    fun `a pair patched for one turn of phrase does not fire elsewhere`() {
        val quirks =
            RecognizerQuirks(
                allowances = mapOf("in sense" to listOf(RecognizerQuirks.Allowance(heard = "incense", after = "bring"))),
            )
        val itsOwn = SpokenLineTracker(line = "I bring in sense;", quirks = quirks)
        val elsewhere = SpokenLineTracker(line = "I burn in sense;", quirks = quirks)

        assertTrue(itsOwn.progress(heard = "I bring incense").isComplete)
        assertFalse(elsewhere.progress(heard = "I burn incense").isComplete)
    }

    @Test
    fun `a patch fires only where its own word is expected`() {
        val quirks = RecognizerQuirks(allowances = mapOf("th’" to listOf("the")))
        val elided = SpokenLineTracker(line = "Which, used, lives th’ executor to be.", quirks = quirks)
        val plain = SpokenLineTracker(line = "And only herald to the gaudy spring,", quirks = quirks)

        assertTrue(elided.progress(heard = "which used lives the executor to be").isComplete)
        assertTrue(plain.progress(heard = "and only herald to the gaudy spring").isComplete)
    }

    @Test
    fun `a dropped word leaves the line unfinished but does not cost the rest`() {
        val progress = tracker.progress(heard = "from fairest creatures desire increase")

        assertFalse(progress.isComplete)
        assertEquals(listOf(correct, correct, correct, wrong, correct, correct), progress.checks)
    }

    @Test
    fun `a false start before the line is skipped rather than held against the reader`() {
        val progress = tracker.progress(heard = "wait sorry from fairest creatures")

        assertEquals(listOf(correct, correct, correct), progress.checks.take(3))
    }

    @Test
    fun `a word said as something else is the only one marked wrong`() {
        val progress = tracker.progress(heard = "from fairest butterfly we desire increase")

        assertEquals(listOf(correct, correct, wrong, correct, correct, correct), progress.checks)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `a mangled opening does not hide the words said correctly after it`() {
        val progress = tracker.progress(heard = "From Fair Screeches V desire increase.")

        assertEquals(listOf(correct, wrong, wrong, wrong, correct, correct), progress.checks)
    }

    @Test
    fun `a hyphenated word heard as two words still counts as said`() {
        val hyphenated = SpokenLineTracker(line = "Feed’st thy light’s flame with self-substantial fuel,")
        val progress = hyphenated.progress(heard = "feedst thy lights flame with self substantial fuel")

        assertTrue(progress.isComplete)
    }

    @Test
    fun `a patched word heard as two words still counts as said`() {
        val quirks = RecognizerQuirks(allowances = mapOf("long-liv’d" to listOf("longlived")))
        val line = SpokenLineTracker(line = "And burn the long-liv’d phoenix, in her blood;", quirks = quirks)

        assertTrue(line.progress(heard = "and burn the long lived phoenix in her blood").isComplete)
    }

    @Test
    fun `two words heard as one still count as said`() {
        val joined = SpokenLineTracker(line = "And do what ever thou wilt swift-footed Time")
        val progress = joined.progress(heard = "and do whatever thou wilt swift footed time")

        assertTrue(progress.isComplete)
    }

    @Test
    fun `elisions in the verse are matched the way they are heard`() {
        val elided = SpokenLineTracker(line = "Feed’st thy light’s flame with self-substantial fuel,")
        val progress = elided.progress(heard = "feedst thy lights flame with self substantial fuel")

        assertTrue(progress.checks.all { it == correct })
    }

    @Test
    fun `a line not yet attempted waits on its first word`() {
        val expected = WordReadingState.EXPECTED
        val ahead = WordReadingState.AHEAD
        assertEquals(listOf(expected, ahead, ahead, ahead, ahead, ahead), tracker.untriedWordStates)
    }

    @Test
    fun `print elides a letter and the recogniser writes it back`() {
        assertTrue(SpokenLineTracker.isFaithful("tattered", to = "tatter’d"))
        assertTrue(SpokenLineTracker.isFaithful("crowned", to = "crown’d"))
        assertTrue(SpokenLineTracker.isFaithful("bestowest", to = "bestow’st"))
        assertTrue(SpokenLineTracker.isFaithful("beguiled", to = "beguil’d"))
        assertTrue(SpokenLineTracker.isFaithful("the", to = "th’"))
    }

    @Test
    fun `only a vowel may be written back, and only where the apostrophe stands`() {
        assertFalse(SpokenLineTracker.isFaithful("whatever", to = "whate’er"))
        assertFalse(SpokenLineTracker.isFaithful("loves", to = "lov’st"))
        assertFalse(SpokenLineTracker.isFaithful("crown", to = "crown’d"))
        assertFalse(SpokenLineTracker.isFaithful("time", to = "times"))
    }
}
