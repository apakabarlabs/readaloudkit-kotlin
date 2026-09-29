package fm.apakabar.readaloudkit

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
         * @throws IllegalStateException when [raw] is not `0`, `1`, or `2`, because this
         * build cannot interpret the persisted state.
         */
        fun stored(raw: Int): StageState =
            entries.firstOrNull { it.rawValue == raw }
                ?: error("A staged reading holds stage state $raw, which this build cannot read.")

        /** Derives the stage state from the states of all its pieces. */
        fun read(states: List<PieceProgressState>): StageState {
            if (states.isEmpty()) return UNTOUCHED
            if (states.all { it == PieceProgressState.CLEARED }) return COMPLETE
            return if (states.any { it != PieceProgressState.UNTOUCHED }) STARTED else UNTOUCHED
        }
    }
}
