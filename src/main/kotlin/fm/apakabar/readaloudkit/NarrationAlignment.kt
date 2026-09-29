package fm.apakabar.readaloudkit

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.Normalizer

/**
 * Word-timing metadata supplied for a recorded narration.
 *
 * Unlike an estimated timeline, an alignment preserves the producer's supplied start
 * and end for every listed word. Decoding refuses times that cannot describe one
 * recording read in order: a negative start, an end before its start, or a word that
 * starts before the word listed ahead of it. It also refuses a field neither the
 * alignment nor its words have, and a value of another type than the field's. Values
 * created in code are not checked.
 *
 * The representation is shared by the tool that measures a recording and the client
 * that presents it.
 *
 * @property piece Identifier of the aligned piece, as the work names it.
 * @property duration Duration of the recording in seconds.
 * @property words Supplied word intervals in passage order.
 * @property recording An application-defined identifier for the recording these timings describe.
 */
@Serializable(with = NarrationAlignment.Serializer::class)
data class NarrationAlignment(
    val piece: String,
    val duration: Double,
    val words: List<Word>,
    val recording: String? = null,
) {
    /**
     * One word and its supplied interval in the recording, not validated against its bounds.
     *
     * Read from JSON under any `Json` configuration, it refuses a field the word does not
     * have, a value of another type than the field's, and a line outside `Int`.
     *
     * @property line Zero-based index of the printed line containing the word.
     * @property text Printed spelling used when the alignment was produced.
     * @property start Start time in seconds from the beginning of the recording.
     * @property end End time in seconds from the beginning of the recording.
     */
    @Serializable(with = Word.Serializer::class)
    data class Word(
        val line: Int,
        val text: String,
        val start: Double,
        val end: Double,
    ) {
        @Serializable
        @SerialName("fm.apakabar.readaloudkit.NarrationAlignment.Word")
        private class Written(
            val line: Int,
            val text: String,
            val start: Double,
            val end: Double,
        )

        /** Writes a word as JSON, and reads one only from JSON, strictly. */
        object Serializer : KSerializer<Word> {
            override val descriptor: SerialDescriptor = Written.serializer().descriptor

            override fun serialize(
                encoder: Encoder,
                value: Word,
            ) = encoder.encodeSerializableValue(
                Written.serializer(),
                Written(line = value.line, text = value.text, start = value.start, end = value.end),
            )

            override fun deserialize(decoder: Decoder): Word {
                val json = decoder as? JsonDecoder ?: throw SerializationException("a word is read from JSON")
                return word(json.decodeJsonElement(), "a word")
            }
        }
    }

    /** Supplied word times that cannot describe one recording read in order. */
    sealed class TimingError(
        message: String,
    ) : SerializationException(message) {
        /** The word at [word], on printed line [line], starts before the recording does. */
        data class NegativeStart(
            val word: Int,
            val line: Int,
        ) : TimingError("Word $word on line $line starts before the recording.")

        /** The word at [word], on printed line [line], ends before it starts. */
        data class EndBeforeStart(
            val word: Int,
            val line: Int,
        ) : TimingError("Word $word on line $line ends before it starts.")

        /** The word at [word], on printed line [line], starts before the word ahead of it. */
        data class StartBeforePrevious(
            val word: Int,
            val line: Int,
        ) : TimingError("Word $word on line $line starts before the word ahead of it.")
    }

    /** A supplied alignment no longer describes the requested passage. */
    sealed class AlignmentError(
        message: String,
    ) : Exception(message) {
        /** The passage and alignment contain different numbers of spoken words. */
        data class WordCountMismatch(
            val expected: Int,
            val found: Int,
        ) : AlignmentError("The alignment lists $found words, the text has $expected.")

        /** The word at [index] or its printed line differs between the passage and alignment. */
        data class WordMismatch(
            val index: Int,
            val expected: String,
            val found: String,
            val expectedLine: Int,
            val foundLine: Int,
        ) : AlignmentError(
                "Word $index is \"$found\" on line $foundLine in the alignment " +
                    "and \"$expected\" on line $expectedLine in the text.",
            )
    }

    /**
     * Associates the supplied times with the words of a passage.
     *
     * The alignment carries the words it was built from. If the passage changes after
     * timing, this method refuses it instead of shifting every later highlight. Words
     * are compared by canonical equivalence, so a letter written with a combining mark
     * matches its precomposed spelling, and each timing carries the passage's spelling.
     *
     * @throws AlignmentError when the passage's words are not the aligned words.
     */
    fun timings(
        passage: Passage,
        tokenizer: WordTokenizer,
    ): List<WordTiming> {
        val spoken = tokenizer.words(passage)
        if (spoken.size != words.size) {
            throw AlignmentError.WordCountMismatch(expected = spoken.size, found = words.size)
        }
        return spoken.zip(words).mapIndexed { index, (word, measured) ->
            if (composed(word.text) != composed(measured.text) || word.lineIndex != measured.line) {
                throw AlignmentError.WordMismatch(
                    index = index,
                    expected = word.text,
                    found = measured.text,
                    expectedLine = word.lineIndex,
                    foundLine = measured.line,
                )
            }
            WordTiming(word = word, start = measured.start, end = measured.end)
        }
    }

    @Serializable
    @SerialName("fm.apakabar.readaloudkit.NarrationAlignment")
    private class Written(
        val piece: String,
        val duration: Double,
        val words: List<Word>,
        val recording: String? = null,
    )

    /**
     * Writes an alignment as JSON, and reads one only from JSON, refusing a field the
     * alignment or a word does not have, a value of another type than the field declares
     * and times that are out of order or bounds.
     */
    object Serializer : KSerializer<NarrationAlignment> {
        override val descriptor: SerialDescriptor = Written.serializer().descriptor

        override fun serialize(
            encoder: Encoder,
            value: NarrationAlignment,
        ) = encoder.encodeSerializableValue(
            Written.serializer(),
            Written(piece = value.piece, duration = value.duration, words = value.words, recording = value.recording),
        )

        override fun deserialize(decoder: Decoder): NarrationAlignment {
            val json = decoder as? JsonDecoder ?: throw SerializationException("an alignment is read from JSON")
            return read(json.decodeJsonElement())
        }
    }

    companion object {
        private val ALIGNMENT_FIELDS = setOf("piece", "duration", "words", "recording")
        private val WORD_FIELDS = setOf("line", "text", "start", "end")

        private fun composed(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC)

        internal fun read(element: JsonElement): NarrationAlignment {
            val fields = element as? JsonObject ?: throw SerializationException("an alignment is not an object")
            refuseFields(fields, otherThan = ALIGNMENT_FIELDS, of = "an alignment")
            val listed = fields["words"] as? JsonArray ?: throw SerializationException("the alignment has no list of words")
            val words = listed.mapIndexed { index, word -> word(word, "word $index") }
            check(words)
            return NarrationAlignment(
                piece = string(fields["piece"], "piece") ?: throw SerializationException("the alignment names no piece"),
                duration = number(fields["duration"], "duration"),
                words = words,
                recording = string(fields["recording"], "recording"),
            )
        }

        private fun word(
            element: JsonElement,
            name: String,
        ): Word {
            val fields = element as? JsonObject ?: throw SerializationException("$name is not an object")
            refuseFields(fields, otherThan = WORD_FIELDS, of = name)
            return Word(
                line = integer(fields["line"], "line of $name"),
                text = string(fields["text"], "text of $name") ?: throw SerializationException("$name has no text"),
                start = number(fields["start"], "start of $name"),
                end = number(fields["end"], "end of $name"),
            )
        }

        private fun check(words: List<Word>) {
            for ((index, word) in words.withIndex()) {
                if (!(word.start >= 0)) throw TimingError.NegativeStart(word = index, line = word.line)
                if (!(word.end >= word.start)) throw TimingError.EndBeforeStart(word = index, line = word.line)
                if (index > 0 && !(word.start >= words[index - 1].start)) {
                    throw TimingError.StartBeforePrevious(word = index, line = word.line)
                }
            }
        }

        internal fun string(
            element: JsonElement?,
            name: String,
        ): String? =
            when {
                element == null || element is JsonNull -> null
                element is JsonPrimitive && element.isString -> element.content
                else -> throw SerializationException("$name is not a string: $element")
            }

        private fun number(
            element: JsonElement?,
            name: String,
        ): Double {
            val literal =
                (element as? JsonPrimitive)
                    ?.takeUnless { it.isString }
                    ?.content
                    ?.takeIf { JSON_NUMBER.matches(it) }
            val value = literal?.toDouble()
            if (value == null || !value.isFinite()) throw SerializationException("$name is not a number: $element")
            return value
        }

        private val JSON_NUMBER = Regex("""-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?""")

        private fun integer(
            element: JsonElement?,
            name: String,
        ): Int {
            val value = number(element, name)
            if (value != Math.rint(value) || value < Int.MIN_VALUE || value > Int.MAX_VALUE) {
                throw SerializationException("$name is not an integer: $element")
            }
            return value.toInt()
        }
    }
}
