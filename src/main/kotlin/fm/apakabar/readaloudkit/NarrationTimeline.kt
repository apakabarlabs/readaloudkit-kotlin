package fm.apakabar.readaloudkit

import fm.apakabar.readalign.EnglishSyllableWeighting
import fm.apakabar.readalign.SilenceHold
import fm.apakabar.readalign.SpeechWeighting
import fm.apakabar.readalign.WordSpan
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.truncate

/**
 * The interval in which one word is spoken in a recording.
 *
 * @property word The passage word occupying the interval.
 * @property start Start time in seconds.
 * @property end End time in seconds.
 */
@ConsistentCopyVisibility
data class WordTiming internal constructor(
    val word: SpokenWord,
    val start: Double,
    val end: Double,
)

/**
 * Builds and queries word-level timelines for recorded speech.
 *
 * [estimate] is fallback scaffolding for a recording without measured alignment.
 * Prefer [NarrationAlignment] whenever measured word timings are available.
 */
object NarrationTimeline {
    private const val MINIMUM_SPEECH_FRACTION = 0.5
    private const val BINARY_SEARCH_DIVISOR = 2
    private const val INSIDE_A_SOUND = 0.5
    private const val RESTING_QUIET = 0.3
    private const val SHORTEST_WORD = 0.04

    /** Silence reserved before the first estimated word. */
    val leadIn = 0.35

    /** Time reserved for a breath at each line break in an estimated timeline. */
    val linePause = 0.35

    /**
     * Estimates word timings by speech weight, reserving a pause at each line break.
     *
     * The default weighting is English-specific. An empty passage, nonpositive
     * duration, or weighting with no positive total produces an empty timeline.
     *
     * [weighting] must return a nonnegative weight for every word.
     */
    fun estimate(
        passage: Passage,
        duration: Double,
        tokenizer: WordTokenizer = WordTokenizer.latinScript,
        weighting: SpeechWeighting = EnglishSyllableWeighting(),
    ): List<WordTiming> {
        val words = tokenizer.words(passage)
        if (words.isEmpty() || !(duration > 0)) return emptyList()

        val pauses = max(passage.lines.size - 1, 0) * linePause
        val speech = max(duration - leadIn - pauses, duration * MINIMUM_SPEECH_FRACTION)
        val weights = words.map { weighting.weight(it.text) }
        val totalWeight = weights.sum()
        if (!(totalWeight > 0)) return emptyList()

        val timings = ArrayList<WordTiming>(words.size)
        var cursor = leadIn
        var previousLine = words[0].lineIndex

        for ((word, weight) in words.zip(weights)) {
            if (word.lineIndex != previousLine) {
                cursor += linePause
                previousLine = word.lineIndex
            }
            val length = speech * weight / totalWeight
            timings.add(WordTiming(word = word, start = cursor, end = cursor + length))
            cursor += length
        }
        return timings
    }

    /**
     * Extends each supplied word interval through its release and the silence that follows.
     *
     * After the scan encounters quiet, the end stops when sound resumes. If sound
     * continues without reaching quiet, the underlying hold limit or next interval
     * bounds the extension.
     *
     * [timings] must be sorted in passage and non-overlapping time order, and
     * [sampleRate] must be positive.
     */
    fun heldThroughSilence(
        timings: List<WordTiming>,
        samples: FloatArray,
        sampleRate: Double,
    ): List<WordTiming> {
        val held =
            SilenceHold.held(
                timings.map { WordSpan(start = it.start, end = it.end) },
                samples = samples,
                sampleRate = sampleRate,
            )
        return timings.zip(held) { timing, span -> WordTiming(word = timing.word, start = span.start, end = span.end) }
    }

    /**
     * Moves a line boundary out of speech and into the first resting silence in reach.
     *
     * Both sides of the boundary move together. If no genuine rest is found, the
     * measured boundary is preserved rather than replaced with a guess. The following
     * word keeps at least 0.04 seconds and its start moves with the shared boundary.
     *
     * [timings] must be sorted in passage and non-overlapping time order, and
     * [sampleRate] must be positive.
     */
    fun settledBetweenLines(
        timings: List<WordTiming>,
        samples: FloatArray,
        sampleRate: Double,
        reach: Double = 0.25,
    ): List<WordTiming> {
        val frames = SilenceHold.energyFrames(samples, sampleRate)
        if (frames.isEmpty()) return timings
        val speech = SilenceHold.speechLevel(frames)
        val frameLength = SilenceHold.frameSeconds

        fun frame(seconds: Double): Int = roundedAwayFromZero(seconds / frameLength).toInt()

        fun level(index: Int): Double = if (index in frames.indices) frames[index] / speech else 0.0

        val settled = timings.toMutableList()
        for (index in 0 until settled.size - 1) {
            val word = settled[index]
            val following = settled[index + 1]
            if (word.word.lineIndex == following.word.lineIndex) continue
            val from = frame(word.end)
            if (level(from) <= INSIDE_A_SOUND) continue

            val room = frame(min(word.end + reach, following.end - SHORTEST_WORD))
            if (room <= from) continue

            val chosen = ((from + 1)..room).firstOrNull { level(it) <= RESTING_QUIET } ?: continue
            val best = chosen * frameLength
            if (best <= word.end) continue
            settled[index] = WordTiming(word = word.word, start = word.start, end = best)
            settled[index + 1] =
                WordTiming(
                    word = following.word,
                    start = max(following.start, best),
                    end = following.end,
                )
        }
        return settled
    }

    /**
     * Extends each word toward the next word, without passing it or exceeding [limit].
     *
     * [timings] must be sorted in non-overlapping time order.
     */
    fun heldToTheNextWord(
        timings: List<WordTiming>,
        duration: Double,
        limit: Double = 0.4,
    ): List<WordTiming> =
        timings.indices.map { index ->
            val timing = timings[index]
            val next = if (index + 1 < timings.size) timings[index + 1].start else duration
            WordTiming(
                word = timing.word,
                start = timing.start,
                end = max(timing.end, min(next, timing.end + limit)),
            )
        }

    /**
     * Returns the part of the recording occupied by one line.
     *
     * [timings] must be in passage and non-overlapping time order.
     */
    fun range(
        ofLine: Int,
        timings: List<WordTiming>,
    ): ClosedFloatingPointRange<Double>? = range(ofLines = ofLine..ofLine, timings = timings)

    /**
     * Returns the part of the recording occupied by a consecutive run of lines.
     *
     * [timings] must be in passage and non-overlapping time order.
     */
    fun range(
        ofLines: IntRange,
        timings: List<WordTiming>,
    ): ClosedFloatingPointRange<Double>? {
        val inside = timings.filter { it.word.lineIndex in ofLines }
        val start = inside.firstOrNull()?.start ?: return null
        val end = inside.last().end
        return start..end
    }

    /**
     * Returns the word at a fractional position through one line.
     *
     * This maps progress through an unaligned recording onto the shape of an aligned
     * one; it is an estimate of position, not a measurement of the reader's speech.
     *
     * [timings] must be sorted in passage and non-overlapping time order.
     */
    fun wordAtFraction(
        fraction: Double,
        ofLine: Int,
        timings: List<WordTiming>,
    ): SpokenWord? = wordAtFraction(fraction, ofLines = ofLine..ofLine, timings = timings)

    /**
     * Returns the word at a fractional position through a consecutive run of lines.
     *
     * [timings] must be sorted in passage and non-overlapping time order.
     */
    fun wordAtFraction(
        fraction: Double,
        ofLines: IntRange,
        timings: List<WordTiming>,
    ): SpokenWord? {
        val range = range(ofLines = ofLines, timings = timings) ?: return null
        val clamped = min(max(fraction, 0.0), 1.0)
        val time = range.start + (range.endInclusive - range.start) * clamped
        val index = index(at = time, timings = timings)
        if (index == null) {
            val isExactlyAtTheEnd = clamped >= 1
            return if (isExactlyAtTheEnd) timings.lastOrNull { it.word.lineIndex in ofLines }?.word else null
        }
        return timings[index].word
    }

    /**
     * Returns the index of the word sounding at [at].
     *
     * [timings] must be sorted by non-overlapping start and end times.
     */
    fun index(
        at: Double,
        timings: List<WordTiming>,
    ): Int? {
        val first = timings.firstOrNull() ?: return null
        val last = timings.last()
        if (!(at >= first.start && at <= last.end)) return null
        var low = 0
        var high = timings.size - 1
        while (low <= high) {
            val middle = (low + high) / BINARY_SEARCH_DIVISOR
            val timing = timings[middle]
            when {
                at < timing.start -> high = middle - 1
                at >= timing.end -> low = middle + 1
                else -> return middle
            }
        }
        return null
    }

    /**
     * Returns the word sounding at [at] when it belongs to [ofLines].
     *
     * Constraining the result prevents a final playback tick from highlighting a word
     * in the following piece.
     *
     * [timings] must be sorted in passage and non-overlapping time order.
     */
    fun word(
        at: Double,
        ofLines: IntRange,
        timings: List<WordTiming>,
    ): SpokenWord? {
        val index = index(at = at, timings = timings) ?: return null
        val word = timings[index].word
        return if (word.lineIndex in ofLines) word else null
    }

    private fun roundedAwayFromZero(value: Double): Double {
        val whole = truncate(value)
        return if (abs(value - whole) == 0.5) whole + sign(value) else round(value)
    }
}
