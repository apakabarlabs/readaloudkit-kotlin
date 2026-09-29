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
data class ExpectedAlignment(
    val piece: String,
    val duration: Double,
    val words: List<NarrationAlignment.Word>,
    val recording: String? = null,
) {
    val alignment: NarrationAlignment
        get() = NarrationAlignment(piece = piece, duration = duration, words = words, recording = recording)
}

@Serializable
data class DecodeCase(
    val name: String,
    val json: String,
    val alignment: ExpectedAlignment? = null,
    val malformed: Boolean? = null,
    @SerialName("timing_error") val timingError: ExpectedTimingError? = null,
)

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
    val words: List<NarrationAlignment.Word>,
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
            val data = case.json.toByteArray()
            case.timingError?.let { expected ->
                assertEquals(expected.error, assertFailsWith<NarrationAlignment.TimingError> { NarrationAlignment.decode(data) })
                return@tests
            }
            if (case.malformed == true) {
                val error = assertFailsWith<SerializationException> { NarrationAlignment.decode(data) }
                assertFalse(error is NarrationAlignment.TimingError, "$error is a timing error, not another shape")
                return@tests
            }
            val expected = checkNotNull(case.alignment) { "a readable case pins the alignment" }
            assertEquals(expected.alignment, NarrationAlignment.decode(data))
        }

    @TestFactory
    fun `marries times to words`(): List<DynamicTest> =
        Cases.tests(AlignmentCases.all.timings, { it.name }) { case ->
            val alignment = NarrationAlignment(piece = "1", duration = 10.0, words = case.words)
            val passage = Passage(lines = case.lines)
            val refusal = case.refusal
            if (refusal != null) {
                assertEquals(
                    refusal,
                    assertFailsWith<NarrationAlignment.AlignmentError> { alignment.timings(passage, WordTokenizer.latinScript) },
                )
                return@tests
            }
            val timings = alignment.timings(passage, WordTokenizer.latinScript)
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
        val data = Json.encodeToString(NarrationAlignment.serializer(), original).toByteArray()

        assertEquals(original, NarrationAlignment.decode(data))
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
