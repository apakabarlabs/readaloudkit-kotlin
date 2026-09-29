package fm.apakabar.readaloudkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@Serializable
data class AlignmentCases(
    val decode: List<DecodeCase>,
    val timings: List<TimingsCase>,
) {
    companion object {
        val all: AlignmentCases by lazy { Cases.load("alignment_tests.yaml", serializer()) }
    }
}

@Serializable
data class ExpectedTimingError(
    val kind: String,
    val word: Int,
    val line: Int,
) {
    val error: NarrationAlignment.TimingError
        get() =
            when (kind) {
                "negative_start" -> NarrationAlignment.TimingError.NegativeStart(word = word, line = line)
                "end_before_start" -> NarrationAlignment.TimingError.EndBeforeStart(word = word, line = line)
                "start_before_previous" -> NarrationAlignment.TimingError.StartBeforePrevious(word = word, line = line)
                else -> error("$kind is not a timing error")
            }
}

@Serializable
data class CaseWord(
    val line: Int,
    val text: String,
    val start: Double,
    val end: Double,
) {
    val word: NarrationAlignment.Word get() = NarrationAlignment.Word(line = line, text = text, start = start, end = end)
}

@Serializable
data class ExpectedAlignment(
    val piece: String,
    val duration: Double,
    val words: List<CaseWord>,
    val recording: String? = null,
) {
    val alignment: NarrationAlignment
        get() = NarrationAlignment(piece = piece, duration = duration, words = words.map { it.word }, recording = recording)
}

@Serializable
data class ExpectedPublished(
    val version: String,
    val alignment: ExpectedAlignment,
) {
    val published: PublishedAlignment
        get() = PublishedAlignment(version = version, alignment = alignment.alignment)
}

@Serializable
data class DecodeCase(
    val name: String,
    val served: String? = null,
    val json: String? = null,
    val published: ExpectedPublished? = null,
    val malformed: Boolean? = null,
    @SerialName("timing_error") val timingError: ExpectedTimingError? = null,
) {
    val data: ByteArray
        get() = served?.let(Cases::bytes) ?: checkNotNull(json) { "$name: a case reads served or json" }.toByteArray()
}

@Serializable
data class ExpectedTiming(
    val text: String,
    val line: Int,
    val start: Double,
    val end: Double,
)

@Serializable
data class CountMismatch(
    val expected: Int,
    val found: Int,
)

@Serializable
data class WordMismatch(
    val index: Int,
    val expected: String,
    val found: String,
    @SerialName("expected_line") val expectedLine: Int,
    @SerialName("found_line") val foundLine: Int,
)

@Serializable
data class TimingsCase(
    val name: String,
    val lines: List<String>,
    @SerialName("interior_marks") val interiorMarks: String,
    val words: List<CaseWord>,
    val timings: List<ExpectedTiming>? = null,
    @SerialName("word_count_mismatch") val wordCountMismatch: CountMismatch? = null,
    @SerialName("word_mismatch") val wordMismatch: WordMismatch? = null,
) {
    val refusal: NarrationAlignment.AlignmentError?
        get() =
            wordCountMismatch?.let { NarrationAlignment.AlignmentError.WordCountMismatch(expected = it.expected, found = it.found) }
                ?: wordMismatch?.let {
                    NarrationAlignment.AlignmentError.WordMismatch(
                        index = it.index,
                        expected = it.expected,
                        found = it.found,
                        expectedLine = it.expectedLine,
                        foundLine = it.foundLine,
                    )
                }
}

class NarrationAlignmentTests {
    @TestFactory
    fun `reads what a server publishes`(): List<DynamicTest> =
        Cases.tests(AlignmentCases.all.decode, { it.name }) { case ->
            val data = case.data
            case.timingError?.let { expected ->
                assertEquals(expected.error, assertFailsWith<NarrationAlignment.TimingError> { PublishedAlignment.decode(data) })
                return@tests
            }
            if (case.malformed == true) {
                val error = assertFailsWith<SerializationException> { PublishedAlignment.decode(data) }
                assertFalse(error is NarrationAlignment.TimingError, "$error is a timing error, not another shape")
                return@tests
            }
            val expected = checkNotNull(case.published) { "a readable case pins the document" }
            assertEquals(expected.published, PublishedAlignment.decode(data))
        }

    @TestFactory
    fun `marries times to words`(): List<DynamicTest> =
        Cases.tests(AlignmentCases.all.timings, { it.name }) { case ->
            val alignment = NarrationAlignment(piece = "1", duration = 10.0, words = case.words.map { it.word })
            val passage = Passage(lines = case.lines)
            val tokenizer = Cases.tokenizer(case.interiorMarks)
            val refusal = case.refusal
            if (refusal != null) {
                assertEquals(
                    refusal,
                    assertFailsWith<NarrationAlignment.AlignmentError> { alignment.timings(passage, tokenizer) },
                )
                return@tests
            }
            val timings = alignment.timings(passage, tokenizer)
            assertEquals(
                checkNotNull(case.timings) { "a fitting case pins its timings" },
                timings.map { ExpectedTiming(text = it.word.text, line = it.word.lineIndex, start = it.start, end = it.end) },
            )
        }

    @Test
    fun `alignment survives a round trip through json`() {
        val original =
            NarrationAlignment(
                piece = "1",
                duration = 10.0,
                words = listOf(NarrationAlignment.Word(line = 0, text = "From", start = 0.0, end = 0.3)),
                recording = "narration-001.mp3",
            )
        val json = Json.encodeToString(NarrationAlignment.serializer(), original)

        assertEquals(original, Json.decodeFromString(NarrationAlignment.serializer(), json))
    }

    @Test
    fun `a word is read strictly under a lenient Json too`() {
        val lenient =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
        val serializer = NarrationAlignment.Word.serializer()

        assertFailsWith<SerializationException> {
            lenient.decodeFromString(serializer, """{"line": 0, "start": 0, "end": 0.1}""")
        }
        assertFailsWith<SerializationException> {
            lenient.decodeFromString(serializer, """{"line": 0, "text": null, "start": 0, "end": 0.1}""")
        }
        assertFailsWith<SerializationException> {
            lenient.decodeFromString(serializer, """{"line": "0", "text": "a", "start": 0, "end": 0.1}""")
        }
        assertFailsWith<SerializationException> {
            lenient.decodeFromString(serializer, """{"line": 2147483648, "text": "a", "start": 0, "end": 0.1}""")
        }
        assertEquals(
            NarrationAlignment.Word(line = 2, text = "a", start = 0.0, end = 0.1),
            lenient.decodeFromString(serializer, """{"line": 2, "text": "a", "start": 0, "end": 0.1}"""),
        )
    }

    @Test
    fun `an alignment without a recording is written without one`() {
        val original = NarrationAlignment(piece = "1", duration = 1.0, words = emptyList())

        assertEquals(
            """{"piece":"1","duration":1.0,"words":[]}""",
            Json.encodeToString(NarrationAlignment.serializer(), original),
        )
    }
}
