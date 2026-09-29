package fm.apakabar.readaloudkit

import java.text.BreakIterator

private const val CHARACTER_TABULATION = 0x09
private const val CARRIAGE_RETURN = 0x0D
private const val NEXT_LINE = 0x85

internal class Characters(
    val text: String,
) {
    private val iterator: BreakIterator = BreakIterator.getCharacterInstance()

    val bounds: IntArray =
        run {
            val bounds = mutableListOf(0)
            iterator.setText(text)
            var next = iterator.next()
            while (next != BreakIterator.DONE) {
                bounds.add(next)
                next = iterator.next()
            }
            bounds.toIntArray()
        }

    val count: Int get() = bounds.size - 1

    fun isLetter(index: Int): Boolean = Character.isAlphabetic(base(index))

    fun isWhitespace(index: Int): Boolean {
        val base = base(index)
        return Character.isSpaceChar(base) || base in CHARACTER_TABULATION..CARRIAGE_RETURN || base == NEXT_LINE
    }

    fun isAll(
        index: Int,
        marks: Set<Int>,
    ): Boolean = text.substring(bounds[index], bounds[index + 1]).codePoints().allMatch { it in marks }

    private fun base(index: Int): Int {
        val start = bounds[index]
        val end = bounds[index + 1]
        val first = text.codePointAt(start)
        if (Character.charCount(first) == end - start) return first
        var offset = start
        while (offset < end) {
            val codePoint = text.codePointAt(offset)
            if (!isPrependedToWhatFollows(codePoint)) return codePoint
            offset += Character.charCount(codePoint)
        }
        return first
    }

    private fun isPrependedToWhatFollows(codePoint: Int): Boolean {
        val aCharacterThatStandsAlone = " "
        val probe = Character.toString(codePoint) + aCharacterThatStandsAlone
        iterator.setText(probe)
        iterator.first()
        return iterator.next() == probe.length
    }
}
