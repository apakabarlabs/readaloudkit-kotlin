package fm.apakabar.readaloudkit

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
data class LayoutCases(
    val `break`: List<BreakCase>,
    val run: List<RunCase>,
) {
    companion object {
        val all: LayoutCases by lazy { Cases.load("layout_tests.yaml", serializer()) }
    }
}

@Serializable
data class BreakCase(
    val name: String,
    val words: List<Double>,
    val space: Double,
    val width: Double,
    val indent: Double,
    val starts: List<Int>,
)

@Serializable
data class RunCase(
    val name: String,
    val words: List<Double>,
    val from: Int,
    val to: Int,
    val space: Double,
    val width: Double,
)

class VerseLayoutTests {
    private val space = 1.0
    private val indent = 8.0

    private fun breakLine(
        words: List<Double>,
        width: Double,
    ): List<Int> = VerseLayoutPlanner.breakLine(words = words, spaceWidth = space, width = width, indent = indent).starts

    @TestFactory
    fun `breaks where the case says`(): List<DynamicTest> =
        Cases.tests(LayoutCases.all.`break`, { it.name }) { case ->
            val starts =
                VerseLayoutPlanner
                    .breakLine(words = case.words, spaceWidth = case.space, width = case.width, indent = case.indent)
                    .starts

            assertEquals(case.starts, starts)
        }

    @TestFactory
    fun `measures runs with spaces`(): List<DynamicTest> =
        Cases.tests(LayoutCases.all.run, { it.name }) { case ->
            assertEquals(case.width, VerseLayoutPlanner.run(case.words, from = case.from, to = case.to, spaceWidth = case.space))
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
        val plan =
            VerseLayoutPlanner.plan(
                lines = listOf(listOf(30.0, 30.0, 30.0, 5.0)),
                spaceWidth = space,
                candidateWidths = listOf(66.0, 60.0),
                indent = indent,
            )
        val split = plan.rowStarts[0][1]

        assertTrue(split < 3)
    }

    @Test
    fun `no candidate widths lay every line out as one row at width zero`() {
        val plan =
            VerseLayoutPlanner.plan(
                lines = listOf(listOf(10.0, 10.0), listOf(20.0)),
                spaceWidth = space,
                candidateWidths = emptyList(),
                indent = indent,
            )

        assertEquals(VerseLayoutPlan(columnWidth = 0.0, rowStarts = listOf(listOf(0), listOf(0))), plan)
    }
}
