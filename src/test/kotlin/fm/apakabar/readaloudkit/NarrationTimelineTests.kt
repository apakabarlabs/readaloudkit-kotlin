package fm.apakabar.readaloudkit

import fm.apakabar.readalign.EnglishSyllableWeighting
import fm.apakabar.readalign.EvenWeighting
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Serializable
data class TimelineCases(
    val settle: List<SettleCase>,
    val hold: List<HoldCase>,
) {
    companion object {
        val all: TimelineCases by lazy { Cases.load("timeline_tests.yaml", serializer()) }
    }
}

fun assertTimings(
    expected: List<List<Double>>,
    actual: List<WordTiming>,
    name: String,
) {
    assertEquals(expected.size, actual.size, "$name: one timing per word")
    for ((index, pair) in actual.zip(expected).withIndex()) {
        val (timing, span) = pair
        assertTrue(Cases.close(timing.start, span[0]), "$name: start of word $index is ${timing.start}")
        assertTrue(Cases.close(timing.end, span[1]), "$name: end of word $index is ${timing.end}")
    }
}

@Serializable
data class SettleCase(
    val name: String,
    val lines: List<String>,
    @SerialName("sample_count") val sampleCount: Int,
    val level: Float,
    val loud: List<List<Int>>,
    val rate: Double,
    val marks: List<List<Double>>,
    val timings: List<List<Double>>,
) {
    val samples: FloatArray
        get() {
            val samples = FloatArray(sampleCount)
            for ((from, to) in loud) for (index in from until to) samples[index] = level
            return samples
        }

    val marked: List<WordTiming>
        get() =
            WordTokenizer.latinScript.words(Passage(lines = lines)).zip(marks) { word, mark ->
                WordTiming(word = word, start = mark[0], end = mark[1])
            }
}

@Serializable
data class HoldCase(
    val name: String,
    val spans: List<List<Double>>,
    val duration: Double,
    val limit: Double? = null,
    val timings: List<List<Double>>,
) {
    val marked: List<WordTiming>
        get() {
            val line = List(spans.size) { "word" }.joinToString(" ")
            val words = WordTokenizer.latinScript.words(Passage(lines = listOf(line)))
            return words.zip(spans) { word, span -> WordTiming(word = word, start = span[0], end = span[1]) }
        }
}

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
        get() =
            NarrationTimeline.estimate(
                passage,
                duration = duration,
                tokenizer = WordTokenizer.latinScript,
                weighting = EnglishSyllableWeighting(),
            )

    @TestFactory
    fun `settles line endings`(): List<DynamicTest> =
        Cases.tests(TimelineCases.all.settle, { it.name }) { case ->
            val settled = NarrationTimeline.settledBetweenLines(case.marked, samples = case.samples, sampleRate = case.rate)

            assertTimings(case.timings, settled, case.name)
        }

    @TestFactory
    fun `holds words open`(): List<DynamicTest> =
        Cases.tests(TimelineCases.all.hold, { it.name }) { case ->
            val held =
                case.limit?.let { NarrationTimeline.heldToTheNextWord(case.marked, duration = case.duration, limit = it) }
                    ?: NarrationTimeline.heldToTheNextWord(case.marked, duration = case.duration)

            assertTimings(case.timings, held, case.name)
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
        val estimated =
            NarrationTimeline.estimate(passage, duration = duration, tokenizer = WordTokenizer.latinScript, weighting = EvenWeighting())
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
        val estimated =
            NarrationTimeline.estimate(
                passage,
                duration = 0.0,
                tokenizer = WordTokenizer.latinScript,
                weighting = EnglishSyllableWeighting(),
            )

        assertTrue(estimated.isEmpty())
    }
}
