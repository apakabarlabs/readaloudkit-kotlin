package fm.apakabar.readaloudkit

import fm.apakabar.readalign.SpeechWeighting
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NarrationTimelineTests {
    private val passage =
        Passage(
            lines =
                listOf(
                    "From fairest creatures we desire increase,",
                    "That thereby beauty’s rose might never die,",
                    "But as the riper should by time decease,",
                ),
        )
    private val duration = 12.0

    private val timings: List<WordTiming>
        get() = NarrationTimeline.estimate(passage, duration = duration)

    private object TwoLines {
        val passage = Passage(lines = listOf("one two", "three four"))
        const val RATE = 1_000.0

        val samples: FloatArray
            get() {
                val result = FloatArray(1_000)
                for (index in 0 until 400) result[index] = 0.5f
                for (index in 500 until 700) result[index] = 0.5f
                return result
            }

        fun timings(marks: List<Pair<Double, Double>>): List<WordTiming> {
            val words = WordTokenizer.latinScript.words(passage)
            return words.zip(marks) { word, mark -> WordTiming(word = word, start = mark.first, end = mark.second) }
        }

        fun settled(marks: List<Pair<Double, Double>>): List<WordTiming> =
            NarrationTimeline.settledBetweenLines(timings(marks), samples = samples, sampleRate = RATE)
    }

    private fun assertRunsForward(timings: List<WordTiming>) {
        for ((earlier, later) in timings.zipWithNext()) {
            assertTrue(later.start >= earlier.end)
            assertTrue(later.end > later.start)
        }
    }

    @Test
    fun `a line ending marked inside a sound is carried to where the sound stops`() {
        val settled = TwoLines.settled(listOf(0.0 to 0.15, 0.15 to 0.30, 0.30 to 0.60, 0.60 to 0.70))

        assertTrue(abs(settled[1].end - 0.40) < 0.011)
        assertEquals(settled[1].end, settled[2].start)
    }

    @Test
    fun `a line ending marked in the quiet is left where it is`() {
        val settled = TwoLines.settled(listOf(0.0 to 0.15, 0.15 to 0.45, 0.45 to 0.60, 0.60 to 0.70))

        assertEquals(0.45, settled[1].end)
        assertEquals(0.45, settled[2].start)
    }

    @Test
    fun `a mark inside a line is left alone however loud the recording is there`() {
        val settled = TwoLines.settled(listOf(0.0 to 0.35, 0.35 to 0.60, 0.60 to 0.65, 0.65 to 0.70))

        assertEquals(0.35, settled[0].end)
        assertEquals(0.35, settled[1].start)
    }

    @Test
    fun `a line ending with no quiet to be found is left where it was measured`() {
        val samples = FloatArray(1_000)
        for (index in 0 until 900) samples[index] = 0.5f
        val settled =
            NarrationTimeline.settledBetweenLines(
                TwoLines.timings(listOf(0.0 to 0.15, 0.15 to 0.30, 0.30 to 0.60, 0.60 to 0.70)),
                samples = samples,
                sampleRate = TwoLines.RATE,
            )

        assertEquals(0.30, settled[1].end)
        assertEquals(0.30, settled[2].start)
    }

    @Test
    fun `a line ending with no room at all is left exactly where it was`() {
        val settled = TwoLines.settled(listOf(0.0 to 0.15, 0.15 to 0.306, 0.306 to 0.308, 0.308 to 0.70))

        assertEquals(0.306, settled[1].end)
        assertEquals(0.306, settled[2].start)
        assertRunsForward(settled)
    }

    @Test
    fun `carrying a line ending never leaves the next word without room`() {
        val settled = TwoLines.settled(listOf(0.0 to 0.15, 0.15 to 0.30, 0.30 to 0.33, 0.33 to 0.70))

        assertEquals(0.30, settled[1].end)
        assertEquals(0.30, settled[2].start)
        assertTrue(settled[2].end > settled[2].start)
    }

    @Test
    fun `the settled reading still runs forward and never overlaps itself`() {
        val settled = TwoLines.settled(listOf(0.0 to 0.15, 0.15 to 0.30, 0.30 to 0.60, 0.60 to 0.70))

        assertRunsForward(settled)
    }

    @Test
    fun `a word is held open after it, but never into the word that follows`() {
        val words = WordTokenizer.latinScript.words(passage)
        val clipped =
            listOf(
                WordTiming(word = words[0], start = 0.0, end = 0.5),
                WordTiming(word = words[1], start = 2.0, end = 2.5),
                WordTiming(word = words[2], start = 2.6, end = 2.8),
            )
        val held = NarrationTimeline.heldToTheNextWord(clipped, duration = 10.0, limit = 0.4)

        assertEquals(0.9, held[0].end)
        assertEquals(2.6, held[1].end)
        assertTrue(abs(held[2].end - 3.2) < 0.0001)
        for ((earlier, later) in held.zipWithNext()) {
            assertTrue(earlier.end <= later.start)
        }
    }

    @Test
    fun `holding a word open never shortens it`() {
        val words = WordTokenizer.latinScript.words(passage)
        val overlapping =
            listOf(
                WordTiming(word = words[0], start = 0.0, end = 1.5),
                WordTiming(word = words[1], start = 1.0, end = 1.4),
            )
        val held = NarrationTimeline.heldToTheNextWord(overlapping, duration = 10.0)
        assertEquals(1.5, held[0].end)
    }

    @Test
    fun `every word gets a timing inside the recording`() {
        val estimated = timings
        assertEquals(WordTokenizer.latinScript.words(passage).size, estimated.size)

        val first = assertNotNull(estimated.firstOrNull())
        val last = assertNotNull(estimated.lastOrNull())
        assertTrue(first.start >= NarrationTimeline.leadIn - 0.001)
        assertTrue(last.end <= duration + 0.001)
    }

    @Test
    fun `timings advance and never overlap`() {
        for ((previous, next) in timings.zipWithNext()) {
            assertTrue(next.start >= previous.end - 0.001)
            assertTrue(next.end > next.start)
        }
    }

    @Test
    fun `the narrator breathes at the line end`() {
        val estimated = timings
        val firstLineCount = WordTokenizer.latinScript.wordRanges(passage.lines[0]).size
        val acrossBreak = estimated[firstLineCount].start - estimated[firstLineCount - 1].end
        val insideLine = estimated[1].start - estimated[0].end

        assertTrue(acrossBreak > insideLine)
    }

    @Test
    fun `longer words get more of the clock`() {
        val estimated = timings
        val short = assertNotNull(estimated.firstOrNull { it.word.text == "we" })
        val long = assertNotNull(estimated.firstOrNull { it.word.text == "creatures" })

        assertTrue(long.end - long.start > short.end - short.start)
    }

    @Test
    fun `a weighting that treats every word alike splits the time evenly`() {
        class FlatWeighting : SpeechWeighting {
            override fun weight(word: String): Double = 1.0
        }
        val estimated = NarrationTimeline.estimate(passage, duration = duration, weighting = FlatWeighting())
        val first = assertNotNull(estimated.firstOrNull())
        val last = assertNotNull(estimated.lastOrNull())

        assertTrue(abs((first.end - first.start) - (last.end - last.start)) < 0.001)
    }

    @Test
    fun `lookup agrees with the spans it searches`() {
        val estimated = timings
        assertNull(NarrationTimeline.index(at = 0.0, timings = estimated))
        assertNull(NarrationTimeline.index(at = duration + 1, timings = estimated))

        for ((index, timing) in estimated.withIndex()) {
            val middle = (timing.start + timing.end) / 2
            assertEquals(index, NarrationTimeline.index(at = middle, timings = estimated))
        }
    }

    @Test
    fun `playback never marks a word beyond the requested lines`() {
        val estimated = timings
        val firstWordOfNextLine = assertNotNull(estimated.firstOrNull { it.word.lineIndex == 1 })
        val time = (firstWordOfNextLine.start + firstWordOfNextLine.end) / 2

        assertNull(NarrationTimeline.word(at = time, ofLines = 0..0, timings = estimated))
        assertEquals(firstWordOfNextLine.word, NarrationTimeline.word(at = time, ofLines = 1..1, timings = estimated))
    }

    @Test
    fun `a line covers its own words and nothing else`() {
        val estimated = timings
        val second = assertNotNull(NarrationTimeline.range(ofLine = 1, timings = estimated))
        val wordsOfSecondLine = estimated.filter { it.word.lineIndex == 1 }

        assertEquals(wordsOfSecondLine.first().start, second.start)
        assertEquals(wordsOfSecondLine.last().end, second.endInclusive)
        assertNull(NarrationTimeline.range(ofLine = 9, timings = estimated))
    }

    @Test
    fun `a reader's own recording is followed by how far through it is`() {
        val estimated = timings
        val line = 1

        val opening = assertNotNull(NarrationTimeline.wordAtFraction(0.0, ofLine = line, timings = estimated))
        val closing = assertNotNull(NarrationTimeline.wordAtFraction(1.0, ofLine = line, timings = estimated))
        val wordsOfTheLine = estimated.filter { it.word.lineIndex == line }.map { it.word }

        assertEquals(wordsOfTheLine.first(), opening)
        assertEquals(wordsOfTheLine.last(), closing)
        for (step in 0..10) {
            val word = assertNotNull(NarrationTimeline.wordAtFraction(step / 10.0, ofLine = line, timings = estimated))
            assertEquals(line, word.lineIndex)
        }
    }

    @Test
    fun `a line with no timings has no word to mark`() {
        assertNull(NarrationTimeline.wordAtFraction(0.5, ofLine = 9, timings = timings))
    }

    @Test
    fun `a recording of unknown length yields no timings`() {
        assertTrue(NarrationTimeline.estimate(passage, duration = 0.0).isEmpty())
    }
}
