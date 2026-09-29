package fm.apakabar.readaloudkit

import kotlin.math.abs
import kotlin.math.max

/**
 * The chosen column and visual row starts for every line of verse.
 *
 * @property columnWidth Chosen column width in the caller's measurement units.
 * @property rowStarts Per line, word indices at which visual rows begin. An empty line is
 * represented by `[0]`, the sentinel for its single empty row.
 */
@ConsistentCopyVisibility
data class VerseLayoutPlan internal constructor(
    val columnWidth: Double,
    val rowStarts: List<List<Int>>,
) {
    /**
     * Returns the word indices at which visual rows begin for one line.
     *
     * @throws IllegalArgumentException when [forLine] is not a line of this plan.
     */
    fun rows(forLine: Int): List<Int> {
        require(forLine in rowStarts.indices) {
            "Line $forLine was laid out against a plan made for ${rowStarts.size} lines."
        }
        return rowStarts[forLine]
    }
}

/** Plans line turnovers for a whole passage rather than letting each line wrap alone. */
object VerseLayoutPlanner {
    internal const val LONELY_WORD_PENALTY = 10_000.0
    internal const val MINIMUM_TURNOVER_FRACTION = 0.33
    internal const val SHORT_TURNOVER_PENALTY = 4_000.0
    internal const val NARROWING_PENALTY_PER_POINT = 3.0

    /**
     * Chooses a column width and balanced row starts for the supplied word widths.
     *
     * @param lines Nonnegative word widths, in order, for each printed line.
     * @param spaceWidth Nonnegative width of the gap between adjacent words, in the same units.
     * @param candidateWidths Positive column widths worth trying. An empty list yields width zero.
     * @param indent Nonnegative indentation applied to continuation rows.
     */
    fun plan(
        lines: List<List<Double>>,
        spaceWidth: Double,
        candidateWidths: List<Double>,
        indent: Double,
    ): VerseLayoutPlan {
        val widest = candidateWidths.maxOrNull() ?: return VerseLayoutPlan(columnWidth = 0.0, rowStarts = lines.map { listOf(0) })

        var best: Candidate? = null
        for (width in candidateWidths.sortedDescending()) {
            var total = (widest - width) * NARROWING_PENALTY_PER_POINT
            val rows = mutableListOf<List<Int>>()
            for (words in lines) {
                val line = breakLine(words = words, spaceWidth = spaceWidth, width = width, indent = indent)
                total += line.cost
                rows.add(line.starts)
            }
            if (best == null || total < best.cost) {
                best = Candidate(width = width, cost = total, rows = rows)
            }
        }

        val chosen = best ?: return VerseLayoutPlan(columnWidth = widest, rowStarts = lines.map { listOf(0) })
        return VerseLayoutPlan(columnWidth = chosen.width, rowStarts = chosen.rows)
    }

    private class Candidate(
        val width: Double,
        val cost: Double,
        val rows: List<List<Int>>,
    )

    internal data class Break(
        val starts: List<Int>,
        val cost: Double,
    )

    internal fun breakLine(
        words: List<Double>,
        spaceWidth: Double,
        width: Double,
        indent: Double,
    ): Break {
        if (words.size <= 1) return Break(listOf(0), 0.0)
        if (run(words, from = 0, to = words.size, spaceWidth = spaceWidth) <= width) {
            return Break(listOf(0), 0.0)
        }

        val turnoverWidth = width - indent
        var bestBreak: Pair<Int, Double>? = null
        var fallback: Pair<Int, Double>? = null

        for (split in 1 until words.size) {
            val head = run(words, from = 0, to = split, spaceWidth = spaceWidth)
            val tail = run(words, from = split, to = words.size, spaceWidth = spaceWidth)

            if (!(head <= width)) break
            if (!(tail <= turnoverWidth)) {
                val overflow = tail - turnoverWidth
                if (fallback == null || overflow < fallback.second) {
                    fallback = split to overflow
                }
                continue
            }

            var cost = abs(head - (tail + indent))
            if (words.size - split == 1) cost += LONELY_WORD_PENALTY
            if (tail < turnoverWidth * MINIMUM_TURNOVER_FRACTION) cost += SHORT_TURNOVER_PENALTY

            if (bestBreak == null || cost < bestBreak.second) {
                bestBreak = split to cost
            }
        }

        val chosen = bestBreak
        if (chosen == null) {
            val split = fallback?.first ?: max(words.size - 1, 1)
            val rest =
                breakLine(
                    words = words.subList(split, words.size),
                    spaceWidth = spaceWidth,
                    width = turnoverWidth,
                    indent = 0.0,
                )
            val starts = listOf(0, split) + rest.starts.drop(1).map { it + split }
            return Break(starts, LONELY_WORD_PENALTY + rest.cost)
        }
        return Break(listOf(0, chosen.first), chosen.second)
    }

    internal fun run(
        words: List<Double>,
        from: Int,
        to: Int,
        spaceWidth: Double,
    ): Double {
        if (to <= from) return 0.0
        val text = words.subList(from, to).sum()
        return text + (to - from - 1) * spaceWidth
    }
}
