# Changelog

## 0.3.0

- First release of the Kotlin/JVM port of
  [readaloudkit-swift](https://github.com/apakabarlabs/readaloudkit-swift) 0.3.0, with
  the same names and behaviour: the passage and its words, the check of a spoken
  attempt and the recogniser quirks it applies, narration alignment and timelines,
  verse layout, staged-reading progress, and the playback and waveform envelopes.
- A narration alignment names its piece as the server's narration schema does, by the
  piece's own id as a string, where readaloudkit-swift 0.2.0 carried a sonnet number.

  Before, as readaloudkit-swift 0.2.0 wrote it:

  ```json
  {"sonnet": 18, "duration": 4, "words": [...]}
  ```

  Now:

  ```json
  {"piece": "18", "duration": 4, "words": [...]}
  ```

  Decoding refuses a value of another type than the field declares and word times
  that cannot describe one recording read in order, naming the word and its line.
- Nothing picks a language for the caller: the tracker, `wordsPerLine`,
  `NarrationAlignment.timings` and `NarrationTimeline.estimate` take their tokenizer,
  and the estimate and `TranscriptAligner.timings` their weighting.
  `WordTokenizer.latinScript` is there to be passed by name.
- Words are cut at the user-perceived characters of Unicode, taken from
  `java.text.BreakIterator`, so a zero-width joiner or non-joiner, a conjunct or an
  emoji modifier falls where it does in the Swift library. That needs the extended
  grapheme clusters the JDK splits at from Java 20, so the library is built for Java 21
  and depends on nothing beyond it; on Android the same classes answer from the
  platform's ICU.
- A character is a letter or a space by its base, the code point its combining marks
  sit on, past any sign prepended to it, as readaloudkit-swift 0.3.0 decides it.
- Built on [readalign-kotlin](https://github.com/apakabarlabs/readalign-kotlin)
  0.17.1, as the Swift library is built on readalign-swift 0.17.
- Where Kotlin cannot tell two Swift overloads apart by their argument labels, the
  fractional lookups are named `NarrationTimeline.wordAtFraction` beside
  `NarrationTimeline.word(at, ofLines, timings)`.
- The tests read the cases of readaloudkit-swift, copied byte for byte into
  `src/test/resources/` by `make sync-yaml`, and a test compares each copy with the
  file on that repository's `main`.
