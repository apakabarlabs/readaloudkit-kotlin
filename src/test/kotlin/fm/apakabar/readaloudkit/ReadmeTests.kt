package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadmeTests {
    private val usage =
        """
        val tracker = SpokenLineTracker(lines = printedLines, quirks = RecognizerQuirks.none, tokenizer = WordTokenizer.latinScript)
        val saidEveryWord = tracker.progress(heard = transcript).isComplete
        """.trimIndent()

    private fun saidEveryWord(
        printedLines: List<String>,
        transcript: String,
    ): Boolean {
        val tracker = SpokenLineTracker(lines = printedLines, quirks = RecognizerQuirks.none, tokenizer = WordTokenizer.latinScript)
        val saidEveryWord = tracker.progress(heard = transcript).isComplete
        return saidEveryWord
    }

    @Test
    fun `the README shows the completeness check these tests run`() {
        assertTrue(File("README.md").readText().contains("\n$usage\n```"))
    }

    @Test
    fun `the README's completeness check tells a dropped word from a reading said whole`() {
        val lines = listOf("From fairest creatures", "we desire increase")

        assertTrue(saidEveryWord(lines, "From fairest creatures we desire increase"))
        assertFalse(saidEveryWord(lines, "From fairest creatures desire increase"))
    }
}
