package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerseLayoutTests {
    private val space = 1.0
    private val indent = 8.0

    private fun breakLine(
        words: List<Double>,
        width: Double,
    ): List<Int> = VerseLayoutPlanner.breakLine(words = words, spaceWidth = space, width = width, indent = indent).starts

    @Test
    fun `a line that fits is never broken`() {
        assertEquals(listOf(0), breakLine(listOf(10.0, 10.0, 10.0), width = 100.0))
    }

    @Test
    fun `a line that does not fit is broken once`() {
        val starts = breakLine(listOf(20.0, 20.0, 20.0, 20.0), width = 50.0)

        assertEquals(2, starts.size)
        assertEquals(0, starts[0])
    }

    @Test
    fun `a break that leaves one word alone is refused when another fits`() {
        val words = listOf(30.0, 30.0, 30.0, 6.0)
        val starts = breakLine(words, width = 70.0)

        assertEquals(2, starts.size)
        assertTrue(starts[1] < words.size - 1)
    }

    @Test
    fun `a stranded word is accepted only when nothing else is possible`() {
        val starts = breakLine(listOf(40.0, 40.0), width = 50.0)

        assertEquals(listOf(0, 1), starts)
    }

    @Test
    fun `the two parts come out balanced rather than lopsided`() {
        val words = List(8) { 10.0 }
        val starts = breakLine(words, width = 60.0)
        val split = starts[1]
        val head = VerseLayoutPlanner.run(words, from = 0, to = split, spaceWidth = space)
        val tail = VerseLayoutPlanner.run(words, from = split, to = words.size, spaceWidth = space)

        assertTrue(abs(head - (tail + indent)) < 20)
    }

    @Test
    fun `a turnover too short to read as a continuation is avoided`() {
        val words = listOf(15.0, 15.0, 15.0, 15.0, 6.0, 6.0)
        val starts = breakLine(words, width = 70.0)
        val split = starts[1]
        val tail = VerseLayoutPlanner.run(words, from = split, to = words.size, spaceWidth = space)

        assertTrue(tail >= (70 - indent) * VerseLayoutPlanner.MINIMUM_TURNOVER_FRACTION)
    }

    @Test
    fun `a line too long for two rows keeps wrapping with the same indent`() {
        val starts = breakLine(List(9) { 20.0 }, width = 45.0)

        assertTrue(starts.size >= 3)
        assertEquals(starts.sorted(), starts)
    }

    @Test
    fun `the poem picks one column width for all its lines`() {
        val plan =
            VerseLayoutPlanner.plan(
                lines =
                    listOf(
                        listOf(10.0, 10.0, 10.0),
                        listOf(20.0, 20.0, 20.0, 20.0),
                        listOf(10.0, 10.0),
                    ),
                spaceWidth = space,
                candidateWidths = listOf(100.0, 92.0, 84.0),
                indent = indent,
            )

        assertEquals(3, plan.rowStarts.size)
        assertEquals(listOf(0), plan.rowStarts[0])
        assertEquals(listOf(0), plan.rowStarts[2])
        assertTrue(plan.columnWidth in listOf(100.0, 92.0, 84.0))
    }

    @Test
    fun `a slightly narrower column is taken when it saves a stranded word`() {
        val lines = listOf(listOf(30.0, 30.0, 30.0, 5.0))
        val plan =
            VerseLayoutPlanner.plan(
                lines = lines,
                spaceWidth = space,
                candidateWidths = listOf(66.0, 60.0),
                indent = indent,
            )
        val split = plan.rowStarts[0][1]

        assertTrue(split < 3)
    }

    @Test
    fun `widths are the planner's only input about the text`() {
        assertEquals(34.0, VerseLayoutPlanner.run(listOf(10.0, 10.0, 10.0), from = 0, to = 3, spaceWidth = 2.0))
        assertEquals(10.0, VerseLayoutPlanner.run(listOf(10.0, 10.0, 10.0), from = 1, to = 2, spaceWidth = 2.0))
        assertEquals(0.0, VerseLayoutPlanner.run(listOf(10.0, 10.0, 10.0), from = 2, to = 2, spaceWidth = 2.0))
    }
}
