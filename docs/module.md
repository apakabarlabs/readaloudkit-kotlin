# ReadAloudKit for Kotlin

Tokenize printed passages, compare them with speech-recognizer output, and map
recorded audio back to the words on the page.

## Start with a reading check

Create a `SpokenLineTracker` from the printed lines and from what the work's data
says about its language, then pass it the complete recognized transcript. A piece is
complete only when every printed word is faithful; alignment may be loose enough to
pair a near miss without crediting it.

```kotlin
val tokenizer = WordTokenizer(interiorMarks = work.interiorMarks)
val elisions = Elisions(fullForms = work.elisions)
val tracker =
    SpokenLineTracker(
        lines = listOf("Will be a tatter’d weed", "of small worth held"),
        elisions = elisions,
        tokenizer = tokenizer,
    )
val progress = tracker.progress(heard = "will be a tattered weed of small worth held")
val completed = progress.isComplete
```

`work` stands for the work's data: the marks its script keeps inside a word, and the
full form of each elided spelling it prints. `WordTokenizer` and `Elisions` hold no
language of their own.

Use `RecognizerQuirks` only for repeatable output of a named recognizer build.
Quirks are not pronunciation rules, and a table published for another build is
refused rather than taken for this one.

## Map audio to print

`PublishedAlignment` reads the word timings the server publishes for a recording,
and `NarrationAlignment` refuses a passage whose words no longer match them. `NarrationTimeline` estimates a temporary
timeline when no alignment exists, adjusts measured boundaries using audio, and
queries the word or line at a playback position.

## Preserve the printed page

`WordTokenizer` returns words and drawable `LineSegment` values without losing
punctuation or spacing. Where a character ends and whether it is a letter follow the
Unicode data of the platform the code runs on. `VerseLayoutPlanner` chooses turnovers for all lines
together so one awkward line does not determine the page by itself.

## Track a staged reading

`PieceProgress` gives each piece of a stage a `PieceProgressState`, and
`StageState.read` derives where the stage stands from them.

## Display audio

`PlaybackEnvelope` fades the edges of a playback buffer, and `WaveformEnvelope`
reduces samples to display amplitudes.
