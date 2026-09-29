# readaloudkit-kotlin

[![Tests](https://github.com/apakabarlabs/readaloudkit-kotlin/actions/workflows/tests.yml/badge.svg)](https://github.com/apakabarlabs/readaloudkit-kotlin/actions/workflows/tests.yml)
[![Documentation](https://github.com/apakabarlabs/readaloudkit-kotlin/actions/workflows/documentation.yml/badge.svg)](https://apakabarlabs.github.io/readaloudkit-kotlin/)

Decides whether a person reading a printed text aloud said what is written, and
holds that decision in one place so that every side of a product answers the
same way.

A recogniser does not return the text that was read. It returns today's spelling
for an old one, a word boundary in the wrong place, and now and then a different
word for the same sound. What counts as having said a word is therefore a rule,
not a comparison, and a rule that lives in two places drifts: the phone clears a
line the server holds, and nobody can say which is right.

This is a Kotlin/JVM port of [readaloudkit-swift](https://github.com/apakabarlabs/readaloudkit-swift),
with the same names, held to the same shared cases. It is built on
[readalign-kotlin](https://github.com/apakabarlabs/readalign-kotlin), as the Swift
library is built on readalign-swift.

## What it decides

- **Whether a word was said.** The recogniser's output is lined up against the
  text, and each pair is the written word itself or a spelling written down for
  the build that misheard it.
- **What a build is allowed to mishear.** `RecognizerQuirks` carries those
  spellings, narrowed where a word is only misheard in one turn of phrase.
- **Where the reader is.** Which line is being read, which words of it are
  already behind, and which word the narration is on.
- **Whether a piece is finished**, and what a stage of a drill still owes.

## Use

```kotlin
import fm.apakabar.readaloudkit.Elisions
import fm.apakabar.readaloudkit.RecognizerQuirks
import fm.apakabar.readaloudkit.SpokenLineTracker
import fm.apakabar.readaloudkit.WordTokenizer

val tokenizer = WordTokenizer(interiorMarks = work.interiorMarks)
val elisions = Elisions(fullForms = work.elisions)
val tracker = SpokenLineTracker(lines = lines, quirks = RecognizerQuirks.none, elisions = elisions, tokenizer = tokenizer)
val saidEveryWord = tracker.progress(heard = transcript).isComplete
```

`work` stands for the data that comes with the work, not with this library: the marks
its script keeps inside a word, such as an apostrophe or a hyphen, and the full forms of
each elided spelling it prints, such as `tattered` for `tatter’d`. Nothing here knows a
language or picks one for you, and an elision the work does not list is not restored.

A written word nothing was heard for is left out of `SpokenWords.check(...).matches`
altogether, so comparing how many matches were faithful with how many there were
does not show that every word was said; `isComplete` does.

Where a character ends and whether it is a letter follow the Unicode data of the
platform the code runs on: the JDK's own tables on the JVM, the system's ICU on Android.
Two runtimes of different ages can cut a character Unicode has since changed in
different places, and nothing here promises otherwise.

Every Kotlin example in this README is code the tests run, and a test fails when one is
not.

## Cases

What the library answers for a given input is written down once, in YAML under
`Tests/ReadAloudKitTests/Resources/` in readaloudkit-swift, and copied into
`src/test/resources/` here with `make sync-yaml`. A test fetches each listed file from
that repository's `main` and fails when the copy differs, so the ports cannot quietly
drift apart.

## Reading what the server publishes

`PublishedAlignment.decode` and `RecognizerQuirks.decode` refuse a document that lacks a
field or holds a value of another type, and read past a field they do not know. An app
already installed cannot learn a field the server adds later, so such a field must not
stop it. A key repeated within one object keeps one of its values; which one is not
promised and may differ between ports.

That leniency rests on a contract with the server. It may add a field, but never one
that changes the meaning of a field the library already reads, such as a field that
narrows an allowance, and it never renames or drops a field. The library cannot tell a
break of that contract from an added field: an allowance whose `after` is misspelt is
read as an allowance with no `after`, allowed after any word.

## Install

The library is published to Maven Central, and a test holds the version below to the
latest one in the CHANGELOG:

```kts
repositories {
    mavenCentral()
}

dependencies {
    implementation("fm.apakabar:readaloudkit-kotlin:0.3.0")
}
```

It runs on Java 21 or later, whose `java.text.BreakIterator` splits text into extended
grapheme clusters, and needs no other library for it.

The API is not settled before 1.0 and may change between minor versions.

## Documentation

The [Dokka API reference](https://apakabarlabs.github.io/readaloudkit-kotlin/) is generated from the public Kotlin API and deployed by GitHub Actions.

## Develop

```bash
make test
make lint
make docs
make build
make sync-yaml   # after the cases change in readaloudkit-swift
```

Releases are published by the [Release workflow](https://github.com/apakabarlabs/readaloudkit-kotlin/actions/workflows/release.yml).

## Lines of Code

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/apakabarlabs/readaloudkit-kotlin/main/.github/loc-history-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/apakabarlabs/readaloudkit-kotlin/main/.github/loc-history-light.svg">
  <img alt="Lines of Code graph" src="https://raw.githubusercontent.com/apakabarlabs/readaloudkit-kotlin/main/.github/loc-history-light.svg">
</picture>
