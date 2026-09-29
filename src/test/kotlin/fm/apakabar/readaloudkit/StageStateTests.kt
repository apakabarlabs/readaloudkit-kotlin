package fm.apakabar.readaloudkit

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

class StageStateTests {
    @TestFactory
    fun `reads the stage from its pieces`(): List<DynamicTest> =
        Cases.tests(ProgressCases.all.stage, { it.name }) { case ->
            assertEquals(stageState(case.stage), StageState.read(case.pieces.map(::pieceProgressState)))
        }
}
