# Changelog

## 0.3.0

Unreleased. Follows readaloudkit-swift 0.3.0.

### Changed

- `NarrationAlignment.sonnet: Int` is now `piece: String`, the field and type the
  server's narration schema publishes. The JSON key changes with it.

  Before:

  ```kotlin
  NarrationAlignment(sonnet = 18, duration = 4.0, words = words, recording = recording)
  ```

  After:

  ```kotlin
  NarrationAlignment(piece = "18", duration = 4.0, words = words, recording = recording)
  ```

- `SpokenLineTracker` keeps the tokenizer it was created with, as `tokenizer`, and
  `progress(heard)` splits the transcript with it. The `tokenizer` argument of
  `progress` is gone.

  Before:

  ```kotlin
  tracker.progress(heard = transcript, tokenizer = tokenizer)
  ```

  After:

  ```kotlin
  tracker.progress(heard = transcript)
  ```

### Internal

- The tests read the cases of readaloudkit-swift, copied byte for byte into
  `src/test/resources/` by `make sync-yaml`, and a test compares each copy with the
  file on that repository's `main`.
- `VerseLayoutPlanner.plan` no longer carries a branch no input can reach.
- The Kotlin JVM plugin is 2.4.20, the same as the serialization plugin.

## 0.2.0

- First release of the Kotlin/JVM port of
  [readaloudkit-swift](https://github.com/apakabarlabs/readaloudkit-swift) 0.2.0,
  with the same names and behaviour: the passage and its words, the check of a
  spoken attempt and the recogniser quirks it applies, narration alignment and
  timelines, verse layout, staged-reading progress, and the playback and
  waveform envelopes.
- Built on [readalign-kotlin](https://github.com/apakabarlabs/readalign-kotlin)
  0.17.1. The Swift 0.2.0 resolves readalign-swift 0.13.1; the releases between
  change only how a long recording is cut into pieces and joined, which nothing
  here calls.
- Where Kotlin cannot tell two Swift overloads apart by their argument labels,
  the fractional lookups are named `NarrationTimeline.wordAtFraction` beside
  `NarrationTimeline.word(at, ofLines, timings)`.
