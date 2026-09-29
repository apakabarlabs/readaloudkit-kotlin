package fm.apakabar.readaloudkit

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Word-timing metadata supplied for a recorded narration.
 *
 * Unlike an estimated timeline, an alignment preserves the producer's supplied start
 * and end for every listed word. The type trusts the producer to validate the spans.
 *
 * The representation is shared by the tool that measures a recording and the client
 * that presents it.
 *
 * @property piece Identifier of the aligned piece, as the work names it.
 * @property duration Duration of the recording in seconds.
 * @property words Supplied word intervals in passage order.
 * @property recording An application-defined identifier for the recording these timings describe.
 */
@Serializable
data class NarrationAlignment(
    val piece: String,
    val duration: Double,
    val words: List<Word>,
    val recording: String? = null,
) {
    /**
     * One word and its supplied interval in the recording, not validated against its bounds.
     *
     * @property line Zero-based index of the printed line containing the word.
     * @property text Printed spelling used when the alignment was produced.
     * @property start Start time in seconds from the beginning of the recording.
     * @property end End time in seconds from the beginning of the recording.
     */
    @Serializable
    data class Word(
        val line: Int,
        val text: String,
        val start: Double,
        val end: Double,
    )

    /** A supplied alignment no longer describes the requested passage. */
    sealed class AlignmentError(
        message: String,
    ) : Exception(message) {
        /** The passage and alignment contain different numbers of spoken words. */
        data class WordCountMismatch(
            val expected: Int,
            val found: Int,
        ) : AlignmentError("The alignment lists $found words, the text has $expected.")

        /** The word at [index] or its line differs between the passage and alignment. */
        data class WordMismatch(
            val index: Int,
            val expected: String,
            val found: String,
        ) : AlignmentError("Word $index is \"$found\" in the alignment and \"$expected\" in the text.")
    }

    /**
     * Associates the supplied times with the words of a passage.
     *
     * The alignment carries the words it was built from. If the passage changes after
     * timing, this method refuses it instead of shifting every later highlight.
     *
     * @throws AlignmentError when the passage's words are not the aligned words.
     */
    fun timings(
        passage: Passage,
        tokenizer: WordTokenizer = WordTokenizer.latinScript,
    ): List<WordTiming> {
        val spoken = tokenizer.words(passage)
        if (spoken.size != words.size) {
            throw AlignmentError.WordCountMismatch(expected = spoken.size, found = words.size)
        }
        return spoken.zip(words).mapIndexed { index, (word, measured) ->
            if (word.text != measured.text || word.lineIndex != measured.line) {
                throw AlignmentError.WordMismatch(index = index, expected = word.text, found = measured.text)
            }
            WordTiming(word = word, start = measured.start, end = measured.end)
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Decodes an alignment from its JSON representation.
         *
         * @throws SerializationException when [data] is not an alignment.
         */
        fun decode(data: ByteArray): NarrationAlignment = json.decodeFromString(serializer(), utf8(data))
    }
}
