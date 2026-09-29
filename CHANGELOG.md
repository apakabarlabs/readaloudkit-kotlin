# Changelog

## 0.3.0

### Added

- The Kotlin/JVM port of
  [readaloudkit-swift](https://github.com/apakabarlabs/readaloudkit-swift) 0.3.0, with
  the same names, held to the same shared cases: the passage and its words, the check
  of a spoken attempt and the recogniser quirks it applies, narration alignment and
  timelines, verse layout, staged-reading progress, and the playback and waveform
  envelopes.
- `PublishedAlignment.decode` reads an alignment as the server publishes it,
  `{"version": ..., "alignment": {...}}`, and `RecognizerQuirks.decode(data, build)`
  reads the hearing table published for one recogniser build,
  `{"build": ..., "version": ..., "words": {written: [{"heard": ..., "after": ...}]}}`,
  refusing one published for another build with `RecognizerQuirks.WrongBuild`. Both
  refuse a field the document does not have, a value of another type than the field
  declares, and word times that cannot describe one recording read in order, naming
  the word and its line.
- Nothing picks a language for the caller: the tracker, `wordsPerLine`,
  `NarrationAlignment.timings` and `NarrationTimeline.estimate` take their tokenizer,
  and the estimate and `TranscriptAligner.timings` their weighting.
  `WordTokenizer.latinScript` is there to be passed by name.
- Words are cut at the user-perceived characters `java.text.BreakIterator` finds, so
  the library needs Java 21 and no other library for it. A character is a letter or a
  space by its base, the code point its combining marks sit on, past any sign prepended
  to it; a private-use character is no letter.
- Built on [readalign-kotlin](https://github.com/apakabarlabs/readalign-kotlin)
  0.17.1, as the Swift library is built on readalign-swift 0.17.
- Where Kotlin cannot tell two Swift overloads apart by their argument labels, the
  fractional lookups are named `NarrationTimeline.wordAtFraction` beside
  `NarrationTimeline.word(at, ofLines, timings)`.
