# Changelog

ReadAloudKit for Kotlin checks a reading aloud of a printed text against that text:
which written words were said, where in the text the reader is, and when each word
sounds in a recorded narration.

## 0.3.0

### Added

- The first release: the Kotlin/JVM port of
  [readaloudkit-swift](https://github.com/apakabarlabs/readaloudkit-swift) 0.3.0,
  numbered 0.3.0 to match the Swift release it ports. It is published on Maven Central
  as `fm.apakabar:readaloudkit-kotlin`, needs Java 21 or later, and is built on
  [readalign-kotlin](https://github.com/apakabarlabs/readalign-kotlin) 0.17.1. It gives
  the answers the Swift library gives, checked against the same shared test cases: the
  passage and its words, the check of a spoken attempt, narration alignment and
  timelines, verse layout, staged-reading progress, and the playback and waveform
  envelopes.
- Names follow the Swift library, written the Kotlin way: enum cases in upper case
  (`StageState.COMPLETE` for `.complete`), line ranges as `IntRange`, times as `Double`
  seconds, and malformed JSON refused with `SerializationException` where Swift throws
  `DecodingError`. Two names differ:
  - `StageState.stored(raw)` for Swift's `StageState(stored:)`. It throws
    `UnknownStageState` for a stored number other than 0, 1 or 2, such as one a newer
    version of your app wrote.
  - `NarrationTimeline.wordAtFraction(fraction, ofLine, timings)` and
    `wordAtFraction(fraction, ofLines, timings)` for Swift's
    `NarrationTimeline.word(atFraction:ofLine:in:)` and `word(atFraction:ofLines:in:)`,
    since Kotlin cannot tell them from `word(at, ofLines, timings)` by argument labels.
- Two JSON documents your server may publish are read: a *narration alignment*, the
  start and end time of every word in a recorded reading, by `PublishedAlignment.decode`
  in the shape `{"version": ..., "alignment": {...}}`; and a *hearing table*, the
  spellings one speech-recognition model build is known to write for particular words,
  by `RecognizerQuirks.decode(data, build)` in the shape
  `{"build": ..., "version": ..., "words": {written: [{"heard": ..., "after": ...}]}}`.
  `build` is the name of the recogniser build your app ships, the same name the table
  was published under; a table published for another build throws
  `RecognizerQuirks.WrongBuild`.

  Both throw for a missing field, a value of the wrong type, or a word's `line` outside
  `Int`, and skip a field they do not know, so that a field added to the documents later
  does not break an app already installed. When a key repeats within one JSON object,
  one of its values is kept; which one is not promised and may differ from the Swift
  library. An alignment whose word times cannot describe one recording read in order (a
  negative start, an end before its start, or a start before the previous word's)
  throws `NarrationAlignment.TimingError`, naming the word and its line. Decoding
  `NarrationAlignment` or `NarrationAlignment.Word` with your own `Json` instance applies
  these same rules whatever its settings, such as `ignoreUnknownKeys`.

  Both documents are read as UTF-8, with or without a byte order mark at the start;
  other text throws `NotUTF8`. A second byte order mark, a lone surrogate escaped in a
  string, and a control character written raw inside a string throw
  `SerializationException`, as JSON forbids them.
- Nothing defaults to English or to Latin script, so the same code serves texts in other
  languages. `SpokenLineTracker` takes `quirks`, `elisions` and `tokenizer` with no
  default: pass the `RecognizerQuirks` read from your recogniser's hearing table, or
  `RecognizerQuirks.none` if you have none. `SpokenLineTracker.wordsPerLine`,
  `NarrationAlignment.timings` and `NarrationTimeline.estimate` take a tokenizer, and
  `NarrationTimeline.estimate` and `TranscriptAligner.timings` a weighting, the
  language's rule for how long each word takes to say, such as readalign-kotlin's
  `EnglishSyllableWeighting()`.

  These come from your text's own data, which this library does not supply.
  `WordTokenizer(interiorMarks = "'’-")` keeps inside a word the marks its script keeps
  there, given as a `String` of those characters or as a `Set<Int>` of their code
  points. `Elisions(fullForms = mapOf("tatter’d" to listOf("tattered")))` lists each
  elided spelling your text prints, a word with a letter left out, with the full forms a
  recogniser writes for it; a full form not listed does not count as saying it.
  `SpokenLineTracker`, `SpokenWords.check` and `SpokenLineTracker.isFaithful` take it.
- Words are split into the user-perceived characters the runtime's
  `java.text.BreakIterator` finds, and a character's being a letter follows the
  runtime's Unicode data, the JDK's on the JVM and the system's on Android. For a
  character Unicode has changed, that can differ from the Swift library on the same text.
