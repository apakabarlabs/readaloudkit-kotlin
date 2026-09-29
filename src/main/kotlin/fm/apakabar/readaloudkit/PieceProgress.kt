package fm.apakabar.readaloudkit

/** What a reader has done with one piece of a staged reading. */
enum class PieceProgressState {
    /** The piece has not been attempted. */
    UNTOUCHED,

    /** The piece was attempted but not cleared. */
    TRIED,

    /** Every required word in the piece was cleared. */
    CLEARED,
}

/** Derives ordered piece states from the pieces attempted and cleared. */
object PieceProgress {
    /** Returns one state for each piece index in `0 until total`. */
    fun states(
        total: Int,
        tried: Set<Int>,
        cleared: Set<Int>,
    ): List<PieceProgressState> =
        (0 until total).map { piece ->
            when (piece) {
                in cleared -> PieceProgressState.CLEARED
                in tried -> PieceProgressState.TRIED
                else -> PieceProgressState.UNTOUCHED
            }
        }
}
