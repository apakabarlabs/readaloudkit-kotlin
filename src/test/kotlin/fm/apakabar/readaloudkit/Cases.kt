package fm.apakabar.readaloudkit

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.DynamicTest
import kotlin.math.abs

object Cases {
    val shared =
        listOf(
            "alignment_tests.yaml",
            "audio_tests.yaml",
            "layout_tests.yaml",
            "progress_tests.yaml",
            "quirks_tests.yaml",
            "spoken_words_tests.yaml",
            "timeline_tests.yaml",
            "tokenizer_tests.yaml",
            "tracker_tests.yaml",
        )

    fun bytes(name: String): ByteArray =
        checkNotNull(Cases::class.java.getResourceAsStream("/$name")) {
            "$name is missing: run `make sync-yaml`"
        }.use { it.readBytes() }

    fun <T> load(
        name: String,
        serializer: KSerializer<T>,
    ): T {
        check(name in shared) { "$name is not in the list of shared cases" }
        return Yaml.default.decodeFromString(serializer, bytes(name).decodeToString())
    }

    fun tokenizer(interiorMarks: String): WordTokenizer = WordTokenizer(interiorMarks)

    fun quirks(allowances: Map<String, List<YamlNode>>?): RecognizerQuirks =
        allowances?.let { table -> RecognizerQuirks(table.mapValues { (written, entries) -> entries.map { allowance(it, written) } }) }
            ?: RecognizerQuirks.none

    private fun allowance(
        node: YamlNode,
        written: String,
    ): RecognizerQuirks.Allowance =
        when (node) {
            is YamlScalar -> RecognizerQuirks.Allowance(heard = node.content)
            is YamlMap -> {
                val heard = checkNotNull(node.get<YamlScalar>("heard")) { "an allowance for $written has no heard spelling" }
                RecognizerQuirks.Allowance(heard = heard.content, after = node.get<YamlScalar>("after")?.content)
            }
            else -> error("an allowance for $written is neither a spelling nor a {heard, after} pair")
        }

    private const val WITHIN = 0.000_000_001

    fun close(
        actual: Double,
        expected: Double,
    ): Boolean = abs(actual - expected) <= WITHIN

    fun <T> tests(
        cases: List<T>,
        name: (T) -> String,
        run: (T) -> Unit,
    ): List<DynamicTest> = cases.map { case -> DynamicTest.dynamicTest(name(case)) { run(case) } }
}

private val WORD_READING_STATES =
    mapOf(
        "ahead" to WordReadingState.AHEAD,
        "close" to WordReadingState.CLOSE,
        "expected" to WordReadingState.EXPECTED,
        "missed" to WordReadingState.MISSED,
        "said" to WordReadingState.SAID,
    )

private val PIECE_PROGRESS_STATES =
    mapOf(
        "untouched" to PieceProgressState.UNTOUCHED,
        "tried" to PieceProgressState.TRIED,
        "cleared" to PieceProgressState.CLEARED,
    )

private val STAGE_STATES =
    mapOf(
        "untouched" to StageState.UNTOUCHED,
        "started" to StageState.STARTED,
        "complete" to StageState.COMPLETE,
    )

fun wordReadingState(name: String): WordReadingState = checkNotNull(WORD_READING_STATES[name]) { "$name is not a word reading state" }

fun pieceProgressState(name: String): PieceProgressState =
    checkNotNull(PIECE_PROGRESS_STATES[name]) { "$name is not a piece progress state" }

fun stageState(name: String): StageState = checkNotNull(STAGE_STATES[name]) { "$name is not a stage state" }
