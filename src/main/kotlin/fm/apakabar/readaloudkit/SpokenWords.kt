package fm.apakabar.readaloudkit

import fm.apakabar.readalign.TranscriptAligner
import fm.apakabar.readalign.WordMatch
import fm.apakabar.readalign.normalize

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
     * faithful spelling, the full form [elisions] gives for an elided one, or an explicit
     * quirk. A listed full form or quirk also pairs its words however unlike they look.
     */
    fun check(
        expected: List<String>,
        heard: List<String>,
        quirks: RecognizerQuirks,
        elisions: Elisions,
        threshold: Double = SpokenLineTracker.closeSimilarityThreshold,
    ): Check {
        val matches =
            TranscriptAligner.pair(
                expected = expected,
                heard = heard,
                threshold = threshold,
                equivalent = { written, said, preceding ->
                    quirks.allows(said, forWritten = written, after = preceding) || normalize(said) in elisions.fullForms(of = written)
                },
            )
        val faithful = mutableSetOf<Int>()
        for ((index, match) in matches.withIndex()) {
            val said = match.heard.joinToString("") { heard[it] }
            val written = match.expected.joinToString("") { expected[it] }
            val before = if (match.expected.first > 0) expected[match.expected.first - 1] else null
            if (SpokenLineTracker.isFaithful(said, to = written, elisions = elisions) ||
                quirks.allows(said, forWritten = written, after = before)
            ) {
                faithful.add(index)
            }
        }
        return Check(matches = matches, faithful = faithful)
    }
}
