package fm.apakabar.readaloudkit

import com.ibm.icu.lang.UCharacter
import com.ibm.icu.lang.UProperty
import com.ibm.icu.text.BreakIterator

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
): Boolean = UCharacter.hasBinaryProperty(text.codePointAt(offset), UProperty.ALPHABETIC)

internal fun isWhitespaceAt(
    text: String,
    offset: Int,
): Boolean = UCharacter.hasBinaryProperty(text.codePointAt(offset), UProperty.WHITE_SPACE)
