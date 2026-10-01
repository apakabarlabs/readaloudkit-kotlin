package fm.apakabar.readaloudkit

import com.charleskorn.kaml.YamlNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

@Serializable
data class TrackerCases(
    val tests: List<TrackerCase>,
    val corrections: List<CorrectionCase>,
) {
    companion object {
        private val file: TrackerCases by lazy { Cases.load("tracker_tests.yaml", serializer()) }
        val all: List<TrackerCase> get() = file.tests
        val corrections: List<CorrectionCase> get() = file.corrections
    }
}

@Serializable
data class CorrectionCase(
    val name: String,
    val lines: List<String>,
    val quirks: Map<String, List<YamlNode>>? = null,
    @SerialName("interior_marks") val interiorMarks: String,
    val elisions: Map<String, List<String>>? = null,
    val heard: String,
    val corrected: String,
)

@Serializable
data class ExpectedAttempt(
    val word: String,
    val check: WordCheck,
)

@Serializable
data class TrackerCase(
    val name: String,
    val lines: List<String>,
    val quirks: Map<String, List<YamlNode>>? = null,
    @SerialName("interior_marks") val interiorMarks: String,
    val elisions: Map<String, List<String>>? = null,
    @SerialName("line_lengths") val lineLengths: List<Int>,
    val heard: String? = null,
    val checks: List<WordCheck>? = null,
    val complete: Boolean? = null,
    @SerialName("all_wrong") val allWrong: Boolean? = null,
    val attempts: List<ExpectedAttempt>? = null,
    val untried: List<String>? = null,
) {
    val tracker: SpokenLineTracker
        get() =
            SpokenLineTracker(
                lines = lines,
                quirks = Cases.quirks(quirks),
                elisions = Cases.elisions(elisions),
                tokenizer = Cases.tokenizer(interiorMarks),
            )
}

class SpokenLineTrackerTests {
    @TestFactory
    fun `checks the reading`(): List<DynamicTest> =
        Cases.tests(TrackerCases.all, { it.name }) { case ->
            val tracker = case.tracker
            assertEquals(case.lineLengths, tracker.lineLengths)
            case.untried?.let { untried -> assertEquals(untried.map(::wordReadingState), tracker.untriedWordStates) }
            val heard = case.heard ?: return@tests
            val progress = tracker.progress(heard = heard)

            assertEquals(checkNotNull(case.checks) { "a heard case pins its checks" }, progress.checks)
            assertEquals(checkNotNull(case.complete) { "and whether it is complete" }, progress.isComplete)
            assertEquals(checkNotNull(case.allWrong) { "and whether it is all wrong" }, progress.isAllWrong)
            case.attempts?.let { attempts ->
                assertEquals(attempts.map { WordAttempt(word = it.word, check = it.check) }, tracker.attempts(progress))
            }
        }

    @TestFactory
    fun `answers with the table applied`(): List<DynamicTest> =
        Cases.tests(TrackerCases.corrections, { it.name }) { case ->
            val tracker =
                SpokenLineTracker(
                    lines = case.lines,
                    quirks = Cases.quirks(case.quirks),
                    elisions = Cases.elisions(case.elisions),
                    tokenizer = Cases.tokenizer(case.interiorMarks),
                )

            assertEquals(case.corrected, tracker.corrected(case.heard))
        }
}
