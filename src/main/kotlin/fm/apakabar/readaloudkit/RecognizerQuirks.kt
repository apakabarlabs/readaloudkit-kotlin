package fm.apakabar.readaloudkit

import fm.apakabar.readalign.normalize
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Explicit spellings that one recognizer may return for particular written words.
 *
 * Quirks repair a named build's repeatable transcription behavior; they are not
 * general rules of pronunciation or language.
 */
class RecognizerQuirks(
    allowances: Map<String, List<Allowance>>,
) {
    /**
     * One permitted heard spelling, optionally limited by the preceding written word.
     *
     * Both spellings are normalized when the allowance is created.
     */
    class Allowance(
        heard: String,
        after: String? = null,
    ) {
        /** Spelling returned by the recognizer. */
        val heard: String = normalize(heard)

        /** Optional preceding written word required for this allowance. */
        val after: String? = after?.let(::normalize)

        override fun equals(other: Any?): Boolean = other is Allowance && other.heard == heard && other.after == after

        override fun hashCode(): Int = 31 * heard.hashCode() + (after?.hashCode() ?: 0)

        override fun toString(): String = "Allowance(heard=$heard, after=$after)"
    }

    private val variants: Map<String, Set<Allowance>> =
        buildMap<String, MutableSet<Allowance>> {
            for ((written, allowed) in allowances) {
                getOrPut(normalize(written)) { mutableSetOf() }.addAll(allowed)
            }
        }

    /** Whether this set permits no recognizer substitutions. */
    val isEmpty: Boolean get() = variants.isEmpty()

    /**
     * A published hearing table belongs to another recognizer build.
     *
     * @property requested The build the caller asked for.
     * @property published The build the table was published for.
     */
    data class WrongBuild(
        val requested: String,
        val published: String,
    ) : Exception("the hearing table was published for $published, not $requested")

    /** Reports whether [heard] is an explicit allowance for [forWritten] after the written word [after]. */
    fun allows(
        heard: String,
        forWritten: String,
        after: String? = null,
    ): Boolean {
        if (variants.isEmpty()) return false
        val allowed = variants[normalize(forWritten)] ?: return false
        val said = normalize(heard)
        val company = after?.let(::normalize)
        return allowed.any { it.heard == said && (it.after == null || it.after == company) }
    }

    companion object {
        /** A quirk set that permits no substitutions. */
        val none = RecognizerQuirks(emptyMap<String, List<Allowance>>())

        /** Creates context-free quirks keyed by written spelling. */
        @JvmName("ofSpellings")
        operator fun invoke(allowances: Map<String, List<String>>): RecognizerQuirks =
            RecognizerQuirks(allowances.mapValues { (_, heard) -> heard.map { Allowance(it) } })

        /**
         * Decodes the hearing table the server publishes for one recognizer build.
         *
         * The document is `{"build": ..., "version": ..., "words": {written: [allowance]}}`,
         * each allowance `{"heard": ..., "after": ...}` with `after` optional. A missing
         * field, another shape or another type is refused, and so is a table published for
         * another build. A field the table does not know is read past, at any depth, so that
         * a field the server adds later does not stop a build already installed. A key
         * repeated within one object keeps one of its values; which one is not promised and
         * may differ between ports.
         *
         * @throws WrongBuild when the table was published for another build than [build].
         * @throws SerializationException when [data] is not such a table.
         */
        fun decode(
            data: ByteArray,
            build: String,
        ): RecognizerQuirks {
            val table = objectOf(Json.parseToJsonElement(utf8(data)), "the hearing table")
            val published = string(table["build"], "build") ?: throw SerializationException("the hearing table names no build")
            string(table["version"], "version") ?: throw SerializationException("the hearing table has no version")
            val words = objectOf(table["words"] ?: throw SerializationException("the hearing table has no words"), "words")
            val allowances =
                words.mapValues { (written, listed) ->
                    val entries = listed as? JsonArray ?: throw SerializationException("$written is not an array")
                    entries.map { allowance(it, written) }
                }
            if (published != build) throw WrongBuild(requested = build, published = published)
            return RecognizerQuirks(allowances)
        }

        private fun objectOf(
            element: JsonElement,
            name: String,
        ): JsonObject = element as? JsonObject ?: throw SerializationException("$name is not an object")

        private fun allowance(
            element: JsonElement,
            written: String,
        ): Allowance {
            val fields = element as? JsonObject ?: throw SerializationException("an allowance for $written is not an object")
            val heard =
                string(fields["heard"], "heard for $written")
                    ?: throw SerializationException("an allowance for $written has no heard spelling")
            return Allowance(heard = heard, after = string(fields["after"], "after for $written"))
        }

        private fun string(
            element: JsonElement?,
            name: String,
        ): String? =
            when {
                element == null || element is JsonNull -> null
                element is JsonPrimitive && element.isString -> element.content
                else -> throw SerializationException("$name is not a string: $element")
            }
    }
}

internal fun utf8(data: ByteArray): String =
    try {
        Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(data))
            .toString()
    } catch (error: java.nio.charset.CharacterCodingException) {
        throw SerializationException("the data is not UTF-8", error)
    }
