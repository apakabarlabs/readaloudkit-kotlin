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

internal fun isLetterAt(
    text: String,
    offset: Int,
): Boolean = Character.isAlphabetic(text.codePointAt(offset))

internal fun isWhitespaceAt(
    text: String,
    offset: Int,
): Boolean {
    val code = text.codePointAt(offset)
    return Character.isSpaceChar(code) || code in CONTROLS_UNICODE_COUNTS_AS_WHITESPACE
}

private val CONTROLS_UNICODE_COUNTS_AS_WHITESPACE = setOf(0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x85)
