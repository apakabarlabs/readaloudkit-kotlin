package fm.apakabar.readaloudkit

import kotlinx.serialization.SerializationException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * A served document that is not UTF-8 text, with or without a byte order mark.
 *
 * Every port refuses such a document with this error, before reading any JSON.
 */
class NotUTF8 : Exception("the document is not UTF-8") {
    override fun equals(other: Any?): Boolean = other is NotUTF8

    override fun hashCode(): Int = NotUTF8::class.hashCode()
}

private val BYTE_ORDER_MARK = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
private const val NUL_NO_JSON_TEXT_HOLDS: Byte = 0
private const val DECODED_BYTE_ORDER_MARK = '\uFEFF'
private const val FIRST_PRINTABLE = '\u0020'
private const val ESCAPED_CODE_UNIT_LENGTH = 6
private const val HEX_RADIX = 16

internal fun utf8(data: ByteArray): String {
    val marked = data.size >= BYTE_ORDER_MARK.size && BYTE_ORDER_MARK.indices.all { data[it] == BYTE_ORDER_MARK[it] }
    val start = if (marked) BYTE_ORDER_MARK.size else 0
    if ((start until data.size).any { data[it] == NUL_NO_JSON_TEXT_HOLDS }) throw NotUTF8()
    val text =
        try {
            Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(data, start, data.size - start))
                .toString()
        } catch (error: CharacterCodingException) {
            throw NotUTF8().initCause(error)
        }
    refuseWhatJsonForbids(text)
    return text
}

private fun refuseWhatJsonForbids(text: String) {
    if (text.startsWith(DECODED_BYTE_ORDER_MARK)) throw SerializationException("a second byte order mark opens the document")
    var index = 0
    var inString = false
    while (index < text.length) {
        val char = text[index]
        when {
            !inString -> inString = char == '"'
            char == '"' -> inString = false
            char == '\\' -> {
                index = endOfEscape(text, index)
                continue
            }
            char < FIRST_PRINTABLE -> throw SerializationException("a control character is written raw inside a string")
        }
        index++
    }
}

private fun endOfEscape(
    text: String,
    index: Int,
): Int {
    val unit = codeUnit(text, index) ?: return index + 2
    if (unit.isLowSurrogate()) throw SerializationException("a lone low surrogate is escaped inside a string")
    if (!unit.isHighSurrogate()) return index + ESCAPED_CODE_UNIT_LENGTH
    val low = codeUnit(text, index + ESCAPED_CODE_UNIT_LENGTH)
    if (low == null || !low.isLowSurrogate()) throw SerializationException("a lone high surrogate is escaped inside a string")
    return index + 2 * ESCAPED_CODE_UNIT_LENGTH
}

private fun codeUnit(
    text: String,
    index: Int,
): Char? {
    val end = index + ESCAPED_CODE_UNIT_LENGTH
    if (end > text.length || text[index] != '\\' || text[index + 1] != 'u') return null
    return text.substring(index + 2, end).toIntOrNull(HEX_RADIX)?.toChar()
}
