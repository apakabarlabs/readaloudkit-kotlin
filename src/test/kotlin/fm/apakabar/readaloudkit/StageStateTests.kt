package fm.apakabar.readaloudkit

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StageStateTests {
    @TestFactory
    fun `reads the stage from its pieces`(): List<DynamicTest> =
        Cases.tests(ProgressCases.all.stage, { it.name }) { case ->
            assertEquals(stageState(case.stage), StageState.read(case.pieces.map(::pieceProgressState)))
        }

    @TestFactory
    fun `restores a stored stage`(): List<DynamicTest> =
        Cases.tests(ProgressCases.all.stored, { it.name }) { case ->
            if (case.unknown == true) {
                assertEquals(UnknownStageState(case.raw), assertFailsWith<UnknownStageState> { StageState.stored(case.raw) })
                return@tests
            }
            val stage = checkNotNull(case.stage) { "a readable case pins the stage" }
            assertEquals(stageState(stage), StageState.stored(case.raw))
        }
}
