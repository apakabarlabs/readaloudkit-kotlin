package fm.apakabar.readaloudkit

/**
 * The printed lines read as one continuous passage.
 *
 * Creating a passage does not change its spelling, punctuation, or spacing.
 *
 * @property lines Printed lines in reading order.
 */
data class Passage(
    val lines: List<String>,
)

/**
 * One drawable part of a line, preserving the marks and spacing around a word.
 *
 * @property wordIndex Index of the word in its line, or `null` for a wordless segment.
 * @property openingMarks Punctuation that opens the word.
 * @property word The spoken letters of the word.
 * @property closingMarks Punctuation that closes the word.
 * @property space Whitespace following the segment.
 */
@ConsistentCopyVisibility
data class LineSegment internal constructor(
    val wordIndex: Int?,
    val openingMarks: String,
    val word: String,
    val closingMarks: String,
    val space: String,
) {
    /** Stable identity of the segment within its line. */
    val id: String get() = "${wordIndex ?: -1}-$word"

    /** The word with its opening and closing punctuation, but without trailing space. */
    val wordWithMarks: String get() = openingMarks + word + closingMarks

    /** The complete original text represented by this segment. */
    val text: String get() = wordWithMarks + space
}

/**
 * One spoken word together with its position and source range in the passage.
 *
 * @property lineIndex Zero-based printed-line index.
 * @property indexInPassage Zero-based word index across the complete passage.
 * @property indexInLine Zero-based word index within the printed line.
 * @property range UTF-16 offsets occupied by the spoken letters in their line.
 * @property text Printed spelling inside [range].
 */
@ConsistentCopyVisibility
data class SpokenWord internal constructor(
    val lineIndex: Int,
    val indexInPassage: Int,
    val indexInLine: Int,
    val range: IntRange,
    val text: String,
) {
    /** Stable identity of the word within its passage. */
    val id: Int get() = indexInPassage

    /** Returns the same word associated with another line index. */
    fun movedToLine(lineIndex: Int): SpokenWord =
        SpokenWord(
            lineIndex = lineIndex,
            indexInPassage = indexInPassage,
            indexInLine = indexInLine,
            range = range,
            text = text,
        )
}
