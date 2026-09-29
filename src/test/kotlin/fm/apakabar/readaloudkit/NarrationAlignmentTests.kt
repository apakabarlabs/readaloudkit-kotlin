package fm.apakabar.readaloudkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
data class AlignmentCases(
    val tests: List<AlignmentCase>,
) {
    companion object {
        val all: List<AlignmentCase> by lazy { Cases.load("alignment_tests.yaml", serializer()).tests }
    }
}

@Serializable
data class ExpectedTiming(
    val index: Int,
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
)

@Serializable
data class AlignmentCase(
    val name: String,
    val lines: List<String>,
    val words: List<NarrationAlignment.Word>,
    val count: Int? = null,
    @SerialName("timings_at") val timingsAt: List<ExpectedTiming>? = null,
    @SerialName("word_count_mismatch") val wordCountMismatch: CountMismatch? = null,
    @SerialName("word_mismatch") val wordMismatch: WordMismatch? = null,
) {
    val refusal: NarrationAlignment.AlignmentError?
        get() =
            wordCountMismatch?.let { NarrationAlignment.AlignmentError.WordCountMismatch(expected = it.expected, found = it.found) }
                ?: wordMismatch?.let {
                    NarrationAlignment.AlignmentError.WordMismatch(index = it.index, expected = it.expected, found = it.found)
                }
}

class NarrationAlignmentTests {
    @TestFactory
    fun `marries times to words`(): List<DynamicTest> =
        Cases.tests(AlignmentCases.all, { it.name }) { case ->
            val alignment = NarrationAlignment(piece = "1", duration = 10.0, words = case.words)
            val passage = Passage(lines = case.lines)
            val refusal = case.refusal
            if (refusal != null) {
                assertEquals(refusal, assertFailsWith<NarrationAlignment.AlignmentError> { alignment.timings(passage) })
                return@tests
            }
            val timings = alignment.timings(passage)
            case.count?.let { assertEquals(it, timings.size) }
            for (expected in case.timingsAt ?: emptyList()) {
                val timing = timings[expected.index]
                assertEquals(expected.text, timing.word.text)
                assertEquals(expected.line, timing.word.lineIndex)
                assertEquals(expected.start, timing.start)
                assertEquals(expected.end, timing.end)
            }
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
    fun `the alignment a server publishes is read as it is served`() {
        val served =
            """
            {"piece": "18", "duration": 4.5, "recording": "narration-018.mp3",
             "words": [{"line": 0, "text": "Shall", "start": 0.5, "end": 0.8}]}
            """.trimIndent().toByteArray()
        val alignment = NarrationAlignment.decode(served)

        assertEquals("18", alignment.piece)
        assertEquals("narration-018.mp3", alignment.recording)
        assertEquals(listOf(NarrationAlignment.Word(line = 0, text = "Shall", start = 0.5, end = 0.8)), alignment.words)
    }
}
