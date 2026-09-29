package fm.apakabar.readaloudkit

import com.charleskorn.kaml.YamlNode
import fm.apakabar.readalign.WordMatch
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

@Serializable
data class SpokenWordsCases(
    val tests: List<SpokenWordsCase>,
    val faithful: List<FaithfulCase>,
) {
    companion object {
        val all: SpokenWordsCases by lazy { Cases.load("spoken_words_tests.yaml", serializer()) }
    }
}

@Serializable
data class ExpectedMatch(
    val expected: List<Int>,
    val heard: List<Int>,
) {
    companion object {
        fun of(match: WordMatch): ExpectedMatch =
            ExpectedMatch(
                expected = listOf(match.expected.first, match.expected.last + 1),
                heard = listOf(match.heard.first, match.heard.last + 1),
            )
    }
}

@Serializable
data class SpokenWordsCase(
    val name: String,
    val expected: List<String>,
    val heard: List<String>,
    val quirks: Map<String, List<YamlNode>>? = null,
    val elisions: Map<String, List<String>>? = null,
    val matches: List<ExpectedMatch>,
    val faithful: Set<Int>,
)

@Serializable
data class FaithfulCase(
    val name: String,
    val heard: String,
    val written: String,
    val elisions: Map<String, List<String>>? = null,
    val faithful: Boolean,
)

class SpokenWordsTests {
    @TestFactory
    fun `accepts only faithful words`(): List<DynamicTest> =
        Cases.tests(SpokenWordsCases.all.tests, { it.name }) { case ->
            val checked =
                SpokenWords.check(
                    expected = case.expected,
                    heard = case.heard,
                    quirks = Cases.quirks(case.quirks),
                    elisions = Cases.elisions(case.elisions),
                )

            assertEquals(case.matches, checked.matches.map(ExpectedMatch::of))
            assertEquals(case.faithful, checked.faithful)
        }

    @TestFactory
    fun `tells a faithful spelling`(): List<DynamicTest> =
        Cases.tests(SpokenWordsCases.all.faithful, { it.name }) { case ->
            val elisions = Cases.elisions(case.elisions)

            assertEquals(case.faithful, SpokenLineTracker.isFaithful(case.heard, to = case.written, elisions = elisions))
        }

    @Test
    fun `the reader's word checks are that same pass`() {
        val quirks = RecognizerQuirks(allowances = mapOf("heir" to listOf("air"), "O" to listOf("oh")))
        val tracker =
            SpokenLineTracker(
                line = "O heir of love",
                quirks = quirks,
                elisions = Elisions.none,
                tokenizer = Cases.sonnetsTokenizer,
            )
        val progress = tracker.progress(heard = "oh air of dove")
        val checked =
            SpokenWords.check(
                expected = listOf("O", "heir", "of", "love"),
                heard = listOf("oh", "air", "of", "dove"),
                quirks = quirks,
                elisions = Elisions.none,
            )
        val faithful = progress.checks.indices.filter { progress.checks[it] == WordCheck.CORRECT }
        val byCheck =
            checked.matches.indices
                .filter { it in checked.faithful }
                .flatMap { checked.matches[it].expected }
        assertEquals(byCheck.sorted(), faithful)
    }
}
