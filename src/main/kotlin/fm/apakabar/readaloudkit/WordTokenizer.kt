package fm.apakabar.readaloudkit

/**
 * Splits printed lines into the words a reader actually says.
 *
 * Which marks may live inside a word is supplied by the language. A writing system
 * without spaces needs another tokenizer rather than a different set of marks.
 *
 * @property interiorMarks Code points that remain part of a word after the word has begun.
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
        val ranges = wordRanges(line)
        if (ranges.isEmpty()) {
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

        val bounds = characterBounds(line)
        val openings =
            ranges.mapIndexed { index, range ->
                val gapStart = if (index == 0) 0 else ranges[index - 1].last + 1
                openingMarkStart(
                    line = line,
                    bounds = bounds,
                    gap = gapStart until range.first,
                    isFirstWord = index == 0,
                )
            }

        val segments = mutableListOf<LineSegment>()
        val runIn = line.substring(0, openings[0])
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
        for ((index, range) in ranges.withIndex()) {
            val untilNextWordsMarks = if (index + 1 < ranges.size) openings[index + 1] else line.length
            val afterStart = range.last + 1
            val closingEnd =
                bounds
                    .filter { it in afterStart..untilNextWordsMarks }
                    .firstOrNull { it == untilNextWordsMarks || isWhitespaceAt(line, it) }
                    ?: untilNextWordsMarks
            segments.add(
                LineSegment(
                    wordIndex = index,
                    openingMarks = line.substring(openings[index], range.first),
                    word = line.substring(range),
                    closingMarks = line.substring(afterStart, closingEnd),
                    space = line.substring(closingEnd, untilNextWordsMarks),
                ),
            )
        }
        return segments
    }

    private fun openingMarkStart(
        line: String,
        bounds: IntArray,
        gap: IntRange,
        isFirstWord: Boolean,
    ): Int {
        val lastSpace =
            bounds.indices
                .lastOrNull { index -> bounds[index] in gap && isWhitespaceAt(line, bounds[index]) }
        if (lastSpace == null) {
            val marksStandWithTheWordBefore = !isFirstWord
            return if (marksStandWithTheWordBefore) gap.last + 1 else gap.first
        }
        return bounds[lastSpace + 1]
    }

    /** Returns the UTF-16 offsets of words in one line. */
    fun wordRanges(line: String): List<IntRange> {
        val bounds = characterBounds(line)
        val ranges = mutableListOf<IntRange>()
        var start: Int? = null

        for (index in 0 until bounds.size - 1) {
            val offset = bounds[index]
            val began = start
            if (isWordCharacter(line.substring(offset, bounds[index + 1]), hasStarted = began != null)) {
                if (began == null) start = offset
            } else if (began != null) {
                ranges.add(began until offset)
                start = null
            }
        }
        start?.let { ranges.add(it until line.length) }
        return ranges.map { trimInteriorMarks(line, bounds, it) }
    }

    private fun isWordCharacter(
        character: String,
        hasStarted: Boolean,
    ): Boolean {
        if (isLetterAt(character, 0)) return true
        return hasStarted && character.codePoints().allMatch { it in interiorMarks }
    }

    private fun trimInteriorMarks(
        line: String,
        bounds: IntArray,
        range: IntRange,
    ): IntRange {
        var end = bounds.indexOf(range.last + 1)
        while (bounds[end] > range.first) {
            val previous = end - 1
            if (isLetterAt(line, bounds[previous])) break
            end = previous
        }
        return range.first until bounds[end]
    }

    companion object {
        /** A Latin-script tokenizer that preserves apostrophes, elisions, and hyphens. */
        val latinScript = WordTokenizer(interiorMarks = "'’-")
    }
}
