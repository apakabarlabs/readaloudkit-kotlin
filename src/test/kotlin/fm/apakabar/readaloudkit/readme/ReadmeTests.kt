package fm.apakabar.readaloudkit.readme

import fm.apakabar.readaloudkit.Elisions
import fm.apakabar.readaloudkit.RecognizerQuirks
import fm.apakabar.readaloudkit.SpokenLineTracker
import fm.apakabar.readaloudkit.WordTokenizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Serializable
private data class Work(
    @SerialName("interior_marks") val interiorMarks: String,
    val elisions: Map<String, String>,
)

private const val SONNETS = """{"interior_marks": "'’-", "elisions": {"tatter’d": "tattered"}}"""

class ReadmeTests {
    private fun saidEveryWord(
        lines: List<String>,
        transcript: String,
    ): Boolean {
        val work = Json.decodeFromString(Work.serializer(), SONNETS)
        val tokenizer = WordTokenizer(interiorMarks = work.interiorMarks)
        val elisions = Elisions(fullForms = work.elisions)
        val tracker = SpokenLineTracker(lines = lines, quirks = RecognizerQuirks.none, elisions = elisions, tokenizer = tokenizer)
        val saidEveryWord = tracker.progress(heard = transcript).isComplete
        return saidEveryWord
    }

    private val readme = File("README.md").readText()

    @Test
    fun `every Kotlin example in the README is code these tests run`() {
        val examples = fencedBlocks(readme, language = "kotlin")
        val run = trimmedLines(File(SOURCE).readText())

        assertTrue(examples.isNotEmpty(), "the README shows no Kotlin")
        for (paragraph in examples.flatMap(::paragraphs)) {
            assertTrue(
                run.windowed(paragraph.size).contains(paragraph),
                "the README shows code no test runs:\n${paragraph.joinToString("\n")}",
            )
        }
    }

    @Test
    fun `the README installs the version the CHANGELOG releases`() {
        val released = checkNotNull(RELEASED.find(File("CHANGELOG.md").readText())) { "CHANGELOG.md names no version" }
        val installed =
            fencedBlocks(readme, language = "kts")
                .flatten()
                .mapNotNull { INSTALLED.find(it)?.groupValues?.get(1) }

        assertEquals(listOf(released.groupValues[1]), installed)
    }

    @Test
    fun `the README's completeness check tells a dropped word from a reading said whole`() {
        val lines = listOf("Will be a tatter’d weed", "of small worth held")

        assertTrue(saidEveryWord(lines, "will be a tattered weed of small worth held"))
        assertFalse(saidEveryWord(lines, "will be a tattered weed of worth held"))
    }

    private fun trimmedLines(text: String): List<String> = text.lines().map { it.trim() }

    private fun fencedBlocks(
        markdown: String,
        language: String,
    ): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        var open: MutableList<String>? = null
        for (line in trimmedLines(markdown)) {
            val block = open
            when {
                block != null && line == "```" -> {
                    blocks.add(block)
                    open = null
                }
                block != null -> block.add(line)
                line == "```$language" -> open = mutableListOf()
            }
        }
        return blocks
    }

    private fun paragraphs(block: List<String>): List<List<String>> {
        val paragraphs = mutableListOf(mutableListOf<String>())
        for (line in block) {
            if (line.isEmpty()) paragraphs.add(mutableListOf()) else paragraphs.last().add(line)
        }
        return paragraphs.filter { it.isNotEmpty() }
    }

    companion object {
        private const val SOURCE = "src/test/kotlin/fm/apakabar/readaloudkit/readme/ReadmeTests.kt"
        private val RELEASED = Regex("""^## (\d+\.\d+\.\d+)$""", RegexOption.MULTILINE)
        private val INSTALLED = Regex(""""fm\.apakabar:readaloudkit-kotlin:([^"]+)"""")
    }
}
