package fm.apakabar.readaloudkit

import fm.apakabar.readalign.RecognizedWord
import fm.apakabar.readalign.SpeechWeighting
import fm.apakabar.readalign.TranscriptAligner

/**
 * Aligns recognized words with printed words and returns passage-aware timings.
 *
 * [weighting] shares time among words the recognizer missed and belongs to the
 * language of the passage.
 */
fun TranscriptAligner.timings(
    words: List<SpokenWord>,
    heard: List<RecognizedWord>,
    duration: Double,
    weighting: SpeechWeighting,
): List<WordTiming> {
    val spans = align(expected = words.map { it.text }, heard = heard, duration = duration, weighting = weighting)
    return words.zip(spans) { word, span -> WordTiming(word = word, start = span.start, end = span.end) }
}
