package fm.apakabar.readaloudkit

import java.text.BreakIterator

internal fun characterBounds(text: String): IntArray {
    val bounds = mutableListOf(0)
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    var next = iterator.next()
    while (next != BreakIterator.DONE) {
        bounds.add(next)
        next = iterator.next()
    }
    return bounds.toIntArray()
}

internal fun characters(text: String): List<String> {
    val bounds = characterBounds(text)
    return (0 until bounds.size - 1).map { text.substring(bounds[it], bounds[it + 1]) }
}

private val WHITE_SPACE = Regex("""\p{IsWhite_Space}""")

internal fun isLetterAt(
    text: String,
    offset: Int,
): Boolean = Character.isAlphabetic(text.codePointAt(offset))

internal fun isWhitespaceAt(
    text: String,
    offset: Int,
): Boolean = WHITE_SPACE.matches(Character.toString(text.codePointAt(offset)))
