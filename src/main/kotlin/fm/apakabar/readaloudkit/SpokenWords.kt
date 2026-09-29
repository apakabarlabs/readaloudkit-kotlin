package fm.apakabar.readaloudkit

import fm.apakabar.readalign.TranscriptAligner
import fm.apakabar.readalign.WordMatch

/** Decides whether recognized words faithfully represent the printed words. */
object SpokenWords {
    /**
     * The alignment between written and heard words and the aligned pairs accepted.
     *
     * Every index in [faithful] must exist in [matches].
     *
     * @property matches Every pair made by the aligner, including refused pairs.
     * @property faithful Indices into [matches] that count as faithfully spoken.
     */
    data class Check(
        val matches: List<WordMatch>,
        val faithful: Set<Int>,
    ) {
        /** Maps each accepted written-word index to the first heard index in its match. */
        val pairs: Map<Int, Int>
            get() =
                buildMap {
                    for (index in faithful) {
                        val match = matches[index]
                        for (word in match.expected) put(word, match.heard.first)
                    }
                }
    }

    /**
     * Aligns heard words with expected words and applies explicit recognizer quirks.
     *
     * [threshold] decides which words may be compared; acceptance still requires a
     * faithful spelling or an explicit quirk.
     */
    fun check(
        expected: List<String>,
        heard: List<String>,
        quirks: RecognizerQuirks,
        threshold: Double = SpokenLineTracker.closeSimilarityThreshold,
    ): Check {
        val matches =
            TranscriptAligner.pair(
                expected = expected,
                heard = heard,
                threshold = threshold,
                equivalent = { written, said, preceding -> quirks.allows(said, forWritten = written, after = preceding) },
            )
        val faithful = mutableSetOf<Int>()
        for ((index, match) in matches.withIndex()) {
            val said = match.heard.joinToString("") { heard[it] }
            val written = match.expected.joinToString("") { expected[it] }
            val before = if (match.expected.first > 0) expected[match.expected.first - 1] else null
            if (SpokenLineTracker.isFaithful(said, to = written) ||
                quirks.allows(said, forWritten = written, after = before)
            ) {
                faithful.add(index)
            }
        }
        return Check(matches = matches, faithful = faithful)
    }
}
