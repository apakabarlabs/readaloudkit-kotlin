package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PieceProgressTests {
    @Test
    fun `progress keeps non-consecutive pieces in their positions`() {
        val states = PieceProgress.states(total = 7, tried = setOf(1, 3, 5), cleared = setOf(1, 5))

        assertEquals(
            listOf(
                PieceProgressState.UNTOUCHED,
                PieceProgressState.CLEARED,
                PieceProgressState.UNTOUCHED,
                PieceProgressState.TRIED,
                PieceProgressState.UNTOUCHED,
                PieceProgressState.CLEARED,
                PieceProgressState.UNTOUCHED,
            ),
            states,
        )
    }
}
