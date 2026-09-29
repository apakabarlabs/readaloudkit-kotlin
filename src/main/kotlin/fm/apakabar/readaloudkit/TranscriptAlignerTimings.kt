package fm.apakabar.readaloudkit

import fm.apakabar.readalign.RecognizedWord
import fm.apakabar.readalign.TranscriptAligner

/** Aligns recognized words with printed words and returns passage-aware timings. */
fun TranscriptAligner.timings(
    words: List<SpokenWord>,
    heard: List<RecognizedWord>,
    duration: Double,
): List<WordTiming> {
    val spans = align(expected = words.map { it.text }, heard = heard, duration = duration)
    return words.zip(spans) { word, span -> WordTiming(word = word, start = span.start, end = span.end) }
}
