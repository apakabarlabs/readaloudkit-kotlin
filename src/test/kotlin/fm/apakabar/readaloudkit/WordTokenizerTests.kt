package fm.apakabar.readaloudkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

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
    @SerialName("interior_marks") val interiorMarks: String? = null,
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
    val segments: List<ExpectedSegment>,
)

@Serializable
data class PassageCase(
    val name: String,
    val lines: List<String>,
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
            val segments = WordTokenizer.latinScript.segments(case.line)

            assertEquals(case.segments.map { it.segment }, segments)
            assertEquals(case.line, segments.joinToString("") { it.text })
        }

    @TestFactory
    fun `numbers the passage`(): List<DynamicTest> =
        Cases.tests(TokenizerCases.all.passage, { it.name }) { case ->
            val spoken = WordTokenizer.latinScript.words(Passage(lines = case.lines))

            assertEquals(case.words, spoken.map { it.text })
            assertEquals(case.words.indices.toList(), spoken.map { it.indexInPassage })
            assertEquals(case.lineOfEach, spoken.map { it.lineIndex })
            assertEquals(case.indexInLine, spoken.map { it.indexInLine })
        }
}
