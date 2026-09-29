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
  refuse a missing field, a value of another type than the field declares, a word's
  line outside `Int`, and word times that cannot describe one recording read in order,
  naming the word and its line. Both read past a field they do not know, so that a
  field the server adds later does not stop a build already installed. A key repeated
  within one object keeps one of its values; which one is not promised and may differ
  from the Swift library. `NarrationAlignment.Word` is read the same way under any
  `Json` configuration. `NarrationAlignment.timings`
  compares the alignment's words with the passage's by canonical equivalence, so a
  letter written with a combining mark matches its precomposed spelling.
- Nothing picks a recogniser's quirks or a language for the caller: the tracker takes
  its quirks with no default, `RecognizerQuirks.none` passed by name, and the tracker,
  `wordsPerLine`,
  `NarrationAlignment.timings` and `NarrationTimeline.estimate` take their tokenizer,
  and the estimate and `TranscriptAligner.timings` their weighting. A tokenizer keeps
  inside a word the marks the work's data names, `WordTokenizer(interiorMarks = ...)`,
  and an elided spelling counts as said only when the heard word is the full form the
  work's data gives for it, passed as `Elisions` to the tracker, `SpokenWords.check` and
  `SpokenLineTracker.isFaithful`.
- Words are cut at the user-perceived characters `java.text.BreakIterator` finds, so
  the library needs Java 21 and no other library for it. A character is a letter or a
  space by its base, the code point its combining marks sit on, past any sign prepended
  to it; a private-use character is no letter. Characters and letters follow the
  Unicode data of the runtime, the JDK's or Android's, which can differ from the Swift
  library's for characters Unicode has changed.
- Built on [readalign-kotlin](https://github.com/apakabarlabs/readalign-kotlin)
  0.17.1, as the Swift library is built on readalign-swift 0.17.
- Where Kotlin cannot tell two Swift overloads apart by their argument labels, the
  fractional lookups are named `NarrationTimeline.wordAtFraction` beside
  `NarrationTimeline.word(at, ofLines, timings)`.
