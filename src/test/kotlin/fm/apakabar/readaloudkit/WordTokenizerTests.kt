package fm.apakabar.readaloudkit

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
data class TokenizerCases(
    val words: List<WordsCase>,
    val segments: List<SegmentsCase>,
    val passage: List<PassageCase>,
) {
    companion object {
        val all: TokenizerCases by lazy { Cases.load("tokenizer_tests.yaml", serializer()) }
    }
}

@Serializable
data class WordsCase(
    val name: String,
    val line: String,
    @SerialName("interior_marks") val interiorMarks: String,
    val words: List<String>,
)

@Serializable
data class ExpectedSegment(
    val index: Int? = null,
    val opening: String = "",
    val word: String = "",
    val closing: String = "",
    val space: String = "",
) {
    val segment: LineSegment
        get() = LineSegment(wordIndex = index, openingMarks = opening, word = word, closingMarks = closing, space = space)
}

@Serializable
data class SegmentsCase(
    val name: String,
    val line: String,
    @SerialName("interior_marks") val interiorMarks: String,
    val segments: List<ExpectedSegment>,
)

@Serializable
data class PassageCase(
    val name: String,
    val lines: List<String>,
    @SerialName("interior_marks") val interiorMarks: String,
    val words: List<String>,
    @SerialName("line_of_each") val lineOfEach: List<Int>,
    @SerialName("index_in_line") val indexInLine: List<Int>,
)

class WordTokenizerTests {
    @TestFactory
    fun `finds the words`(): List<DynamicTest> =
        Cases.tests(TokenizerCases.all.words, { it.name }) { case ->
            val tokenizer = Cases.tokenizer(case.interiorMarks)

            assertEquals(case.words, tokenizer.wordRanges(case.line).map { case.line.substring(it) })
        }

    @TestFactory
    fun `cuts the segments`(): List<DynamicTest> =
        Cases.tests(TokenizerCases.all.segments, { it.name }) { case ->
            val segments = Cases.tokenizer(case.interiorMarks).segments(case.line)

            assertEquals(case.segments.map { it.segment }, segments)
            assertEquals(case.line, segments.joinToString("") { it.text })
        }

    @TestFactory
    fun `numbers the passage`(): List<DynamicTest> =
        Cases.tests(TokenizerCases.all.passage, { it.name }) { case ->
            val spoken = Cases.tokenizer(case.interiorMarks).words(Passage(lines = case.lines))

            assertEquals(case.words, spoken.map { it.text })
            assertEquals(case.words.indices.toList(), spoken.map { it.indexInPassage })
            assertEquals(case.lineOfEach, spoken.map { it.lineIndex })
            assertEquals(case.indexInLine, spoken.map { it.indexInLine })
        }

    @Test
    fun `a case that does not name its interior marks cannot be read`() {
        val unnamed = """{name: "no language", line: "beauty's rose", words: ["beauty's", "rose"]}"""

        assertFailsWith<SerializationException> { Yaml.default.decodeFromString(WordsCase.serializer(), unnamed) }
    }
}
