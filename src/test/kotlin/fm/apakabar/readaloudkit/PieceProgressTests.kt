package fm.apakabar.readaloudkit

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

@Serializable
data class ProgressCases(
    val pieces: List<PiecesCase>,
    val stage: List<StageCase>,
    val stored: List<StoredCase>,
) {
    companion object {
        val all: ProgressCases by lazy { Cases.load("progress_tests.yaml", serializer()) }
    }
}

@Serializable
data class PiecesCase(
    val name: String,
    val total: Int,
    val tried: Set<Int>,
    val cleared: Set<Int>,
    val states: List<String>,
)

@Serializable
data class StageCase(
    val name: String,
    val pieces: List<String>,
    val stage: String,
)

@Serializable
data class StoredCase(
    val name: String,
    val raw: Int,
    val stage: String? = null,
    val unknown: Boolean? = null,
)

class PieceProgressTests {
    @TestFactory
    fun `keeps pieces in their positions`(): List<DynamicTest> =
        Cases.tests(ProgressCases.all.pieces, { it.name }) { case ->
            val states = PieceProgress.states(total = case.total, tried = case.tried, cleared = case.cleared)

            assertEquals(case.states.map(::pieceProgressState), states)
        }
}
