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
 * Quirks repair a named model's repeatable transcription behavior; they are not
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
     * The requested model has no entry in a decoded quirk table.
     *
     * @property model Requested model identifier.
     * @property known Model identifiers present in the table.
     */
    class UnknownModel(
        val model: String,
        val known: List<String>,
    ) : Exception("no patches listed for $model; the table has ${known.sorted().joinToString(", ")}")

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
         * Decodes the allowances for [model], refusing a table that does not name it.
         *
         * The JSON root maps model identifiers to written words. Each written word maps
         * to an array containing either a heard string or `{ "heard": ..., "after": ... }`.
         * A pair with any other field is refused, as is a value of another type. Every
         * model's section is read, so a table malformed anywhere is refused.
         *
         * @throws UnknownModel when the table has no section for [model].
         * @throws SerializationException when [data] is not such a table.
         */
        fun decode(
            data: ByteArray,
            model: String,
        ): RecognizerQuirks {
            val table =
                objectOf(Json.parseToJsonElement(utf8(data)), "the table").mapValues { (name, section) ->
                    objectOf(section, name).mapValues { (written, listed) ->
                        val entries = listed as? JsonArray ?: throw SerializationException("$written in $name is not an array")
                        entries.map { allowance(it, written) }
                    }
                }
            val allowances = table[model] ?: throw UnknownModel(model = model, known = table.keys.toList())
            return RecognizerQuirks(allowances)
        }

        private val ALLOWANCE_FIELDS = setOf("heard", "after")

        private fun objectOf(
            element: JsonElement,
            name: String,
        ): JsonObject = element as? JsonObject ?: throw SerializationException("$name is not an object")

        private fun allowance(
            element: JsonElement,
            written: String,
        ): Allowance {
            if (element is JsonObject) {
                refuseFields(element, otherThan = ALLOWANCE_FIELDS, of = "an allowance for $written")
                val heard =
                    string(element["heard"], "heard for $written")
                        ?: throw SerializationException("an allowance for $written has no heard spelling")
                return Allowance(heard = heard, after = string(element["after"], "after for $written"))
            }
            val heard =
                string(element, "an allowance for $written")
                    ?: throw SerializationException("an allowance for $written is null")
            return Allowance(heard = heard)
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
