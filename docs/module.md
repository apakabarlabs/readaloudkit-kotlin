# ReadAloudKit for Kotlin

Tokenize printed passages, compare them with speech-recognizer output, and map
recorded audio back to the words on the page.

## Start with a reading check

Create a `SpokenLineTracker` from the printed lines, then pass it the complete
recognized transcript. A piece is complete only when every printed word is
faithful; alignment may be loose enough to pair a near miss without crediting it.

```kotlin
val tracker =
    SpokenLineTracker(
        lines = listOf("From fairest creatures", "we desire increase"),
        tokenizer = WordTokenizer.latinScript,
    )
val progress = tracker.progress(heard = "From fairest creatures we desire increase")
val completed = progress.isComplete
```

Use `RecognizerQuirks` only for repeatable output of a named recognizer build.
Quirks are not pronunciation rules and an unknown model is refused rather than
treated as a model with no quirks.

## Map audio to print

`NarrationAlignment` imports word timings supplied with a recording and refuses
a passage whose words no longer match. `NarrationTimeline` estimates a temporary
timeline when no alignment exists, adjusts measured boundaries using audio, and
queries the word or line at a playback position.

## Preserve the printed page

`WordTokenizer` returns words and drawable `LineSegment` values without losing
punctuation or spacing. `VerseLayoutPlanner` chooses turnovers for all lines
together so one awkward line does not determine the page by itself.

## Track a staged reading

`PieceProgress` gives each piece of a stage a `PieceProgressState`, and
`StageState.read` derives where the stage stands from them.

## Display audio

`PlaybackEnvelope` fades the edges of a playback buffer, and `WaveformEnvelope`
reduces samples to display amplitudes.
