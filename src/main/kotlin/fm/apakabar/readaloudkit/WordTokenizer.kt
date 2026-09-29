package fm.apakabar.readaloudkit

/**
 * Splits printed lines into the words a reader actually says.
 *
 * Which marks may live inside a word is supplied by the language. A writing system
 * without spaces needs another tokenizer rather than a different set of marks.
 *
 * A character, as Unicode clusters it, is a letter or a space by its base: the
 * code point its combining marks sit on, past any sign prepended to it. A private-use
 * character is never a letter, whatever a font draws for it.
 *
 * Where a character ends and whether it is a letter follow the Unicode data of the
 * runtime, the JDK's or Android's, so an older runtime can cut a newer character
 * differently.
 *
 * @property interiorMarks Code points that remain part of a word after the word has begun,
 * the marks the work's data names for its script, such as an apostrophe or a hyphen.
 */
class WordTokenizer(
    val interiorMarks: Set<Int>,
) {
    constructor(interiorMarks: String) : this(interiorMarks.codePoints().toArray().toSet())

    /** Returns all spoken words with their positions in the passage. */
    fun words(passage: Passage): List<SpokenWord> {
        val words = mutableListOf<SpokenWord>()
        for ((lineIndex, line) in passage.lines.withIndex()) {
            for ((indexInLine, range) in wordRanges(line).withIndex()) {
                words.add(
                    SpokenWord(
                        lineIndex = lineIndex,
                        indexInPassage = words.size,
                        indexInLine = indexInLine,
                        range = range,
                        text = line.substring(range),
                    ),
                )
            }
        }
        return words
    }

    /** Splits every line into drawable word-and-punctuation segments. */
    fun segments(passage: Passage): List<List<LineSegment>> = passage.lines.map { segments(it) }

    /** Splits one line while preserving every original character and space. */
    fun segments(line: String): List<LineSegment> {
        val characters = Characters(line)
        val words = wordCharacters(characters)
        if (words.isEmpty()) {
            val wordless =
                LineSegment(
                    wordIndex = null,
                    openingMarks = "",
                    word = "",
                    closingMarks = line,
                    space = "",
                )
            return if (line.isEmpty()) emptyList() else listOf(wordless)
        }

        val offset = characters.bounds
        val openings =
            words.mapIndexed { index, word ->
                val gapStart = if (index == 0) 0 else words[index - 1].last + 1
                openingMarkStart(characters, gapStart = gapStart, gapEnd = word.first, isFirstWord = index == 0)
            }

        val segments = mutableListOf<LineSegment>()
        val runIn = line.substring(0, offset[openings[0]])
        if (runIn.isNotEmpty()) {
            segments.add(
                LineSegment(
                    wordIndex = null,
                    openingMarks = "",
                    word = "",
                    closingMarks = "",
                    space = runIn,
                ),
            )
        }
        for ((index, word) in words.withIndex()) {
            val untilNextWordsMarks = if (index + 1 < words.size) openings[index + 1] else characters.count
            val afterWord = word.last + 1
            var closingEnd = afterWord
            while (closingEnd < untilNextWordsMarks && !characters.isWhitespace(closingEnd)) closingEnd++
            segments.add(
                LineSegment(
                    wordIndex = index,
                    openingMarks = line.substring(offset[openings[index]], offset[word.first]),
                    word = line.substring(offset[word.first], offset[afterWord]),
                    closingMarks = line.substring(offset[afterWord], offset[closingEnd]),
                    space = line.substring(offset[closingEnd], offset[untilNextWordsMarks]),
                ),
            )
        }
        return segments
    }

    private fun openingMarkStart(
        characters: Characters,
        gapStart: Int,
        gapEnd: Int,
        isFirstWord: Boolean,
    ): Int {
        for (index in gapEnd - 1 downTo gapStart) {
            if (characters.isWhitespace(index)) return index + 1
        }
        val marksStandWithTheWordBefore = !isFirstWord
        return if (marksStandWithTheWordBefore) gapEnd else gapStart
    }

    /** Returns the UTF-16 offsets of words in one line. */
    fun wordRanges(line: String): List<IntRange> {
        val characters = Characters(line)
        val offset = characters.bounds
        return wordCharacters(characters).map { offset[it.first] until offset[it.last + 1] }
    }

    private fun wordCharacters(characters: Characters): List<IntRange> {
        val words = mutableListOf<IntRange>()
        var start = -1
        for (index in 0 until characters.count) {
            if (isWordCharacter(characters, index, hasStarted = start >= 0)) {
                if (start < 0) start = index
            } else if (start >= 0) {
                words.add(trimInteriorMarks(characters, start, index))
                start = -1
            }
        }
        if (start >= 0) words.add(trimInteriorMarks(characters, start, characters.count))
        return words
    }

    private fun isWordCharacter(
        characters: Characters,
        index: Int,
        hasStarted: Boolean,
    ): Boolean {
        if (characters.isLetter(index)) return true
        return hasStarted && characters.isAll(index, interiorMarks)
    }

    private fun trimInteriorMarks(
        characters: Characters,
        start: Int,
        end: Int,
    ): IntRange {
        var last = end
        while (last > start && !characters.isLetter(last - 1)) last--
        return start until last
    }
}
