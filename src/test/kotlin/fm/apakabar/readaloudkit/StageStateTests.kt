package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StageStateTests {
    @Test
    fun aStageOfNoPiecesIsUntouchedRatherThanComplete() {
        assertEquals(StageState.UNTOUCHED, StageState.read(emptyList()))
    }

    @Test
    fun everyPieceClearedMakesTheStageComplete() {
        assertEquals(StageState.COMPLETE, StageState.read(List(3) { PieceProgressState.CLEARED }))
    }

    @Test
    fun oneTriedPieceMakesTheStageStarted() {
        assertEquals(StageState.STARTED, StageState.read(listOf(PieceProgressState.TRIED, PieceProgressState.UNTOUCHED)))
    }

    @Test
    fun untouchedPiecesLeaveTheStageUntouched() {
        assertEquals(StageState.UNTOUCHED, StageState.read(List(3) { PieceProgressState.UNTOUCHED }))
    }
}
