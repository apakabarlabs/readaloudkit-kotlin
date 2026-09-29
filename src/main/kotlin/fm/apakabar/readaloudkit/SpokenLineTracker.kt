package fm.apakabar.readaloudkit

import fm.apakabar.readalign.normalize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How one printed word is presented while a reading is in progress. */
enum class WordReadingState {
    /** A later word not yet reached. */
    AHEAD,

    /** A near miss aligned with this printed word. */
    CLOSE,

    /** The next word expected before an attempt begins. */
    EXPECTED,

    /** A printed word not faithfully heard. */
    MISSED,

    /** A printed word faithfully heard. */
    SAID,
}

/**
 * The persisted result of checking one printed word.
 *
 * These raw spellings are part of the stored-data contract.
 *
 * @property rawValue The stored spelling of the result.
 */
@Serializable
enum class WordCheck(
    val rawValue: String,
) {
    /** A near miss was aligned with the word. */
    @SerialName("close")
    CLOSE("close"),

    /** The word was faithfully heard. */
    @SerialName("correct")
    CORRECT("correct"),

    /** No faithful or near matching word was heard. */
    @SerialName("wrong")
    WRONG("wrong"),
}

/**
 * A printed word paired with the result of one reading attempt.
 *
 * @property word Printed spelling from the passage.
 * @property check Result assigned to the printed word.
 */
data class WordAttempt(
    val word: String,
    val check: WordCheck,
)

/**
 * Checks a complete spoken attempt against one or more printed lines.
 *
 * The whole transcript is aligned at once so one misrecognized word does not shift
 * every word that follows it.
 *
 * [quirks], [elisions] and [tokenizer] have no default: each comes from the
 * recogniser's or the work's data, and `RecognizerQuirks.none` is passed by name.
 *
 * @property quirks Model-specific transcription allowances applied while checking.
 * @property elisions The full forms of the elided spellings the work prints, from the work's data.
 * @property tokenizer Splits both the printed lines and every transcript checked against them.
 */
class SpokenLineTracker(
    lines: List<String>,
    val quirks: RecognizerQuirks,
    val elisions: Elisions,
    val tokenizer: WordTokenizer,
) {
    constructor(
        line: String,
        quirks: RecognizerQuirks,
        elisions: Elisions,
        tokenizer: WordTokenizer,
    ) : this(listOf(line), quirks, elisions, tokenizer)

    /**
     * The result for every expected word in one attempt.
     *
     * @property checks One result per expected word, in passage order.
     */
    data class Progress(
        val checks: List<WordCheck>,
    ) {
        /** Whether every expected word was said faithfully. */
        val isComplete: Boolean get() = checks.isNotEmpty() && checks.all { it == WordCheck.CORRECT }

        /** Whether no expected word could be credited or aligned as a near miss. */
        val isAllWrong: Boolean get() = checks.isNotEmpty() && checks.all { it == WordCheck.WRONG }

        /** The display state corresponding to each check. */
        val wordStates: List<WordReadingState>
            get() =
                checks.map { check ->
                    when (check) {
                        WordCheck.CORRECT -> WordReadingState.SAID
                        WordCheck.CLOSE -> WordReadingState.CLOSE
                        WordCheck.WRONG -> WordReadingState.MISSED
                    }
                }
    }

    private val words: List<List<String>> = lines.map { line -> tokenizer.wordRanges(line).map { line.substring(it) } }

    /** Printed words in passage order. */
    val expected: List<String> = words.flatten()

    /** The number of expected words in each printed line. */
    val lineLengths: List<Int> = words.map { it.size }

    /** Returns the part of a passage-wide state array belonging to one tracked line. */
    fun wordStates(
        states: List<WordReadingState>,
        forLineAt: Int,
    ): List<WordReadingState> = wordStates(states, forLineAt = forLineAt, wordsPerLine = lineLengths)

    /** States shown before an attempt: the first word expected and later words ahead. */
    val untriedWordStates: List<WordReadingState>
        get() = expected.indices.map { if (it == 0) WordReadingState.EXPECTED else WordReadingState.AHEAD }

    /**
     * Checks a complete recognized transcript against the tracked printed words.
     *
     * The transcript is split by the tracker's own [tokenizer], so printed and heard
     * words are cut by one rule. Extra words outside the best alignment do not count
     * against the printed words.
     */
    fun progress(heard: String): Progress {
        val said = tokenizer.wordRanges(heard).map { heard.substring(it) }
        val checked = SpokenWords.check(expected = expected, heard = said, quirks = quirks, elisions = elisions)
        val checks = MutableList(expected.size) { WordCheck.WRONG }
        for ((index, match) in checked.matches.withIndex()) {
            val check = if (index in checked.faithful) WordCheck.CORRECT else WordCheck.CLOSE
            for (word in match.expected) checks[word] = check
        }
        return Progress(checks)
    }

    /** Pairs the original printed spellings with their check results. */
    fun attempts(progress: Progress): List<WordAttempt> =
        expected.zip(progress.checks) { word, check -> WordAttempt(word = word, check = check) }

    companion object {
        /**
         * The minimum similarity used to align a near miss with a printed word.
         *
         * This threshold decides which words are compared. A word is credited only when
         * its spelling is faithful or an explicit recognizer quirk permits it.
         */
        val closeSimilarityThreshold = 0.6

        /** Counts spoken words in each line using [tokenizer]. */
        fun wordsPerLine(
            of: List<String>,
            tokenizer: WordTokenizer,
        ): List<Int> = of.map { tokenizer.wordRanges(it).size }

        /** Returns the part of a passage-wide state array belonging to one line. */
        fun wordStates(
            states: List<WordReadingState>,
            forLineAt: Int,
            wordsPerLine: List<Int>,
        ): List<WordReadingState> {
            if (forLineAt !in wordsPerLine.indices) return emptyList()
            val start = wordsPerLine.take(forLineAt).sum()
            val end = minOf(start + wordsPerLine[forLineAt], states.size)
            if (start >= end) return emptyList()
            return states.subList(start, end).toList()
        }

        /**
         * Reports whether a heard spelling faithfully represents a written word.
         *
         * Case and punctuation are ignored. An elided spelling also counts as said when
         * the heard word is the full form [elisions] gives for it; nothing else is restored.
         */
        fun isFaithful(
            heard: String,
            to: String,
            elisions: Elisions,
        ): Boolean {
            val said = normalize(heard)
            return said == normalize(to) || said in elisions.fullForms(of = to)
        }
    }
}
