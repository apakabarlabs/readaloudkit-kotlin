package fm.apakabar.readaloudkit

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Reduces audio samples to normalized display amplitudes. */
object WaveformEnvelope {
    private const val DEFAULT_BAR_COUNT = 48
    private const val DECIBELS_PER_BEL = 20.0
    private const val MINIMUM_AMPLITUDE = 0.000001
    private const val NORMALIZATION_FLOOR = 50.0
    private const val NORMALIZATION_RANGE = 45.0

    /** Builds an envelope using the library's default number of bars. */
    fun make(from: FloatArray): List<Double> = make(from = from, bars = DEFAULT_BAR_COUNT)

    /**
     * Builds at most [bars] normalized amplitudes in the range zero through one.
     *
     * Empty samples or a nonpositive bar count produce an empty envelope. More bars than
     * samples, up to [Int.MAX_VALUE], give one bar a sample. Each input sample is a
     * linear floating-point amplitude.
     */
    fun make(
        from: FloatArray,
        bars: Int,
    ): List<Double> {
        if (from.isEmpty() || bars <= 0) return emptyList()
        val count = min(bars, from.size)
        val firstSample = { bar: Int -> (bar.toLong() * from.size / count).toInt() }
        return (0 until count).map { bar ->
            val start = firstSample(bar)
            val end = max(firstSample(bar + 1), start + 1)
            var squares = 0.0
            for (index in start until end) squares += (from[index] * from[index]).toDouble()
            val squareMean = squares / (end - start)
            val decibels = DECIBELS_PER_BEL * log10(max(sqrt(squareMean), MINIMUM_AMPLITUDE))
            min(max((decibels + NORMALIZATION_FLOOR) / NORMALIZATION_RANGE, 0.0), 1.0)
        }
    }
}
