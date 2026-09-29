package fm.apakabar.readaloudkit

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

internal fun utf8(data: ByteArray): String {
    val marked = data.size >= BYTE_ORDER_MARK.size && BYTE_ORDER_MARK.indices.all { data[it] == BYTE_ORDER_MARK[it] }
    val start = if (marked) BYTE_ORDER_MARK.size else 0
    if ((start until data.size).any { data[it] == NUL_NO_JSON_TEXT_HOLDS }) throw NotUTF8()
    return try {
        Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(data, start, data.size - start))
            .toString()
    } catch (error: CharacterCodingException) {
        throw NotUTF8().initCause(error)
    }
}
