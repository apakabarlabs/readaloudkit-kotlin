package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordTokenizerTests {
    private val tokenizer = WordTokenizer.latinScript

    private fun words(line: String): List<String> = tokenizer.wordRanges(line).map { line.substring(it) }

    @Test
    fun `apostrophes hold elisions together, in either shape`() {
        assertEquals(
            listOf("That", "thereby", "beauty’s", "rose", "might", "never", "die"),
            words("That thereby beauty’s rose might never die,"),
        )
        assertEquals(listOf("Feed'st", "thy", "light's", "flame"), words("Feed'st thy light's flame"))
    }

    @Test
    fun `hyphens hold compounds together`() {
        assertEquals(listOf("with", "self-substantial", "fuel"), words("with self-substantial fuel,"))
    }

    @Test
    fun `punctuation between words is dropped`() {
        assertEquals(
            listOf("Thy", "self", "thy", "foe", "to", "thy", "sweet", "self", "too", "cruel"),
            words("Thy self thy foe, to thy sweet self too cruel:"),
        )
    }

    @Test
    fun `quotation marks stay outside the words they wrap`() {
        assertEquals(
            listOf("If", "thou", "couldst", "answer", "This", "fair", "child", "of", "mine"),
            words("If thou couldst answer ‘This fair child of mine"),
        )
        assertEquals(
            listOf("Shall", "sum", "my", "count", "and", "make", "my", "old", "excuse"),
            words("Shall sum my count, and make my old excuse,’"),
        )
    }

    @Test
    fun `a line with no letters yields no words`() {
        assertTrue(words("").isEmpty())
        assertTrue(words("...").isEmpty())
    }

    @Test
    fun `words carry their line and their order`() {
        val passage = Passage(lines = listOf("From fairest creatures", "we desire increase,"))
        val spoken = tokenizer.words(passage)

        assertEquals(listOf("From", "fairest", "creatures", "we", "desire", "increase"), spoken.map { it.text })
        assertEquals(listOf(0, 1, 2, 3, 4, 5), spoken.map { it.indexInPassage })
        assertEquals(listOf(0, 0, 0, 1, 1, 1), spoken.map { it.lineIndex })
    }

    @Test
    fun `a line splits into pieces that put it back together unchanged`() {
        val line = "Thy self thy foe, to thy sweet self too cruel:"
        val segments = tokenizer.segments(line)

        assertEquals(line, segments.joinToString("") { it.text })
        assertEquals((0 until 10).toList(), segments.mapNotNull { it.wordIndex })
        assertEquals("foe", segments[3].word)
        assertEquals(",", segments[3].closingMarks)
        assertEquals(" ", segments[3].space)
        assertEquals(":", segments.last().closingMarks)
    }

    @Test
    fun `a mark that opens a word goes with that word, not with the one before it`() {
        val line = "she said: ‘go on’"
        val segments = tokenizer.segments(line)

        assertEquals(line, segments.joinToString("") { it.text })
        assertEquals("said", segments[1].word)
        assertEquals(":", segments[1].closingMarks)
        assertEquals(" ", segments[1].space)
        assertEquals("‘", segments[2].openingMarks)
        assertEquals("go", segments[2].word)
        assertEquals("’", segments.last().closingMarks)
    }

    @Test
    fun `a line that opens with a quotation mark gives it to its first word`() {
        val line = "‘go on’ she said"
        val segments = tokenizer.segments(line)

        assertEquals(line, segments.joinToString("") { it.text })
        assertEquals(0, segments[0].wordIndex)
        assertEquals("‘", segments[0].openingMarks)
        assertEquals("go", segments[0].word)
    }

    @Test
    fun `marks with no space around them stand with the word before`() {
        val segments = tokenizer.segments("here,—there")

        assertEquals("here", segments[0].word)
        assertEquals(",—", segments[0].closingMarks)
        assertTrue(segments[1].openingMarks.isEmpty())
        assertEquals("there", segments[1].word)
    }

    @Test
    fun `a letter written with a combining mark is one character of its word`() {
        val line = "café, au lait"

        assertEquals(listOf("café", "au", "lait"), words(line))
        assertEquals(",", tokenizer.segments(line)[0].closingMarks)
    }

    @Test
    fun `a language without apostrophe elisions can say so`() {
        val plain = WordTokenizer(interiorMarks = "-")

        assertEquals(
            listOf("beauty", "s", "rose"),
            plain.wordRanges("beauty's rose").map { "beauty's rose".substring(it) },
        )
    }
}
