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
) {
    companion object {
        val all: List<TrackerCase> by lazy { Cases.load("tracker_tests.yaml", serializer()).tests }
    }
}

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
    @SerialName("interior_marks") val interiorMarks: String? = null,
    val heard: String? = null,
    val checks: List<WordCheck>? = null,
    @SerialName("checks_at") val checksAt: Map<String, WordCheck>? = null,
    val rest: WordCheck? = null,
    @SerialName("states_at") val statesAt: Map<String, String>? = null,
    val complete: Boolean? = null,
    @SerialName("all_wrong") val allWrong: Boolean? = null,
    val attempts: List<ExpectedAttempt>? = null,
    @SerialName("line_lengths") val lineLengths: List<Int>? = null,
    val untried: List<String>? = null,
) {
    val tracker: SpokenLineTracker
        get() = SpokenLineTracker(lines = lines, quirks = Cases.quirks(quirks), tokenizer = Cases.tokenizer(interiorMarks))
}

class SpokenLineTrackerTests {
    @TestFactory
    fun `checks the reading`(): List<DynamicTest> =
        Cases.tests(TrackerCases.all, { it.name }) { case ->
            val tracker = case.tracker
            case.lineLengths?.let { assertEquals(it, tracker.lineLengths) }
            case.untried?.let { untried -> assertEquals(untried.map(::wordReadingState), tracker.untriedWordStates) }
            case.heard?.let { heard -> check(tracker.progress(heard = heard), tracker, case) }
        }

    private fun check(
        progress: SpokenLineTracker.Progress,
        tracker: SpokenLineTracker,
        case: TrackerCase,
    ) {
        case.checks?.let { assertEquals(it, progress.checks) }
        val pinned = (case.checksAt ?: emptyMap()).mapKeys { (key, _) -> Cases.index(key, case.name) }
        for ((index, check) in pinned) assertEquals(check, progress.checks[index], "word $index")
        case.rest?.let { rest ->
            for (index in progress.checks.indices.filter { it !in pinned }) {
                assertEquals(rest, progress.checks[index], "word $index")
            }
        }
        for ((key, state) in case.statesAt ?: emptyMap()) {
            val index = Cases.index(key, case.name)
            assertEquals(wordReadingState(state), progress.wordStates[index], "word $index")
        }
        case.complete?.let { assertEquals(it, progress.isComplete) }
        case.allWrong?.let { assertEquals(it, progress.isAllWrong) }
        case.attempts?.let { attempts ->
            assertEquals(attempts.map { WordAttempt(word = it.word, check = it.check) }, tracker.attempts(progress))
        }
    }
}
