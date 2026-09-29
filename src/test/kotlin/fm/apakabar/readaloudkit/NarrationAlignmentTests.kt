package fm.apakabar.readaloudkit

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NarrationAlignmentTests {
    private val passage = Passage(lines = listOf("From fairest creatures", "we desire increase,"))

    private fun alignment(words: List<NarrationAlignment.Word>): NarrationAlignment =
        NarrationAlignment(sonnet = 1, duration = 10.0, words = words)

    private val measured =
        listOf(
            NarrationAlignment.Word(line = 0, text = "From", start = 0.0, end = 0.3),
            NarrationAlignment.Word(line = 0, text = "fairest", start = 0.3, end = 0.9),
            NarrationAlignment.Word(line = 0, text = "creatures", start = 0.9, end = 1.5),
            NarrationAlignment.Word(line = 1, text = "we", start = 1.8, end = 2.0),
            NarrationAlignment.Word(line = 1, text = "desire", start = 2.0, end = 2.5),
            NarrationAlignment.Word(line = 1, text = "increase", start = 2.5, end = 3.2),
        )

    @Test
    fun `measured times land on the words of the passage`() {
        val timings = alignment(measured).timings(passage)

        assertEquals(6, timings.size)
        assertEquals("From", timings[0].word.text)
        assertEquals(0.0, timings[0].start)
        assertEquals(1, timings[3].word.lineIndex)
        assertEquals(1.8, timings[3].start)
    }

    @Test
    fun `a text edited after the markup was made is refused, not slid by one word`() {
        val edited = Passage(lines = listOf("From fairest creatures", "we desire increase, and more"))

        val error = assertFailsWith<NarrationAlignment.AlignmentError> { alignment(measured).timings(edited) }
        assertEquals(NarrationAlignment.AlignmentError.WordCountMismatch(expected = 8, found = 6), error)
    }

    @Test
    fun `a word swapped in the text is caught by name`() {
        val edited = Passage(lines = listOf("From fairest creatures", "we desire increases,"))

        val error = assertFailsWith<NarrationAlignment.AlignmentError> { alignment(measured).timings(edited) }
        assertEquals(
            NarrationAlignment.AlignmentError.WordMismatch(index = 5, expected = "increases", found = "increase"),
            error,
        )
    }

    @Test
    fun `alignment survives a round trip through json`() {
        val original = alignment(measured)
        val data = Json.encodeToString(NarrationAlignment.serializer(), original).toByteArray()

        assertEquals(original, NarrationAlignment.decode(data))
    }
}
