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

internal fun characterAt(
    text: String,
    bounds: IntArray,
    offset: Int,
): String = text.substring(offset, bounds[bounds.binarySearch(offset) + 1])

private val WHITE_SPACE = Regex("""\p{IsWhite_Space}""")

internal fun isLetter(character: String): Boolean = Character.isAlphabetic(base(character))

internal fun isWhitespace(character: String): Boolean = WHITE_SPACE.matches(Character.toString(base(character)))

private fun base(character: String): Int {
    val first = character.codePointAt(0)
    if (Character.charCount(first) == character.length) return first
    return character
        .codePoints()
        .filter { !isPrependedToWhatFollows(it) }
        .findFirst()
        .orElse(first)
}

private fun isPrependedToWhatFollows(codePoint: Int): Boolean {
    val aCharacterThatStandsAlone = " "
    return characterBounds(Character.toString(codePoint) + aCharacterThatStandsAlone).size == 2
}
