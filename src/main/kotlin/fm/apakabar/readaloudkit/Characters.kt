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

private const val CHARACTER_TABULATION = 0x09
private const val CARRIAGE_RETURN = 0x0D
private const val NEXT_LINE = 0x85

internal fun isLetter(character: String): Boolean = Character.isAlphabetic(base(character))

internal fun isWhitespace(character: String): Boolean {
    val base = base(character)
    return Character.isSpaceChar(base) || base in CHARACTER_TABULATION..CARRIAGE_RETURN || base == NEXT_LINE
}

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
