package fm.apakabar.readaloudkit

/**
 * A stored stage state this build does not know, perhaps written by a newer one.
 *
 * Every port refuses such a value with this error; what to show instead is the
 * caller's decision.
 *
 * @property raw The stored value that was read.
 */
data class UnknownStageState(
    val raw: Int,
) : Exception("a staged reading holds stage state $raw, which this build cannot read")

/**
 * Where one stage of a staged reading stands.
 *
 * @property rawValue The stored value of the state.
 */
enum class StageState(
    val rawValue: Int,
) {
    /** No piece in the stage has been attempted. */
    UNTOUCHED(0),

    /** At least one piece was attempted and the stage is not complete. */
    STARTED(1),

    /** Every piece in the stage was cleared. */
    COMPLETE(2),
    ;

    companion object {
        /**
         * Restores a persisted state.
         *
         * @throws UnknownStageState when [raw] is not `0`, `1` or `2`.
         */
        fun stored(raw: Int): StageState = entries.firstOrNull { it.rawValue == raw } ?: throw UnknownStageState(raw)

        /** Derives the stage state from the states of all its pieces. */
        fun read(states: List<PieceProgressState>): StageState {
            if (states.isEmpty()) return UNTOUCHED
            if (states.all { it == PieceProgressState.CLEARED }) return COMPLETE
            return if (states.any { it != PieceProgressState.UNTOUCHED }) STARTED else UNTOUCHED
        }
    }
}
