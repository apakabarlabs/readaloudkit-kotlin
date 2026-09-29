# Changelog

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
