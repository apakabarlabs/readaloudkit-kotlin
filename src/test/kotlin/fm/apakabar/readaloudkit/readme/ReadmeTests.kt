package fm.apakabar.readaloudkit.readme

import fm.apakabar.readaloudkit.Elisions
import fm.apakabar.readaloudkit.RecognizerQuirks
import fm.apakabar.readaloudkit.SpokenLineTracker
import fm.apakabar.readaloudkit.WordTokenizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
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

    private fun completedReading(): Boolean {
        val work = Json.decodeFromString(Work.serializer(), SONNETS)
        val tokenizer = WordTokenizer(interiorMarks = work.interiorMarks)
        val elisions = Elisions(fullForms = work.elisions)
        val tracker =
            SpokenLineTracker(
                lines = listOf("Will be a tatter’d weed", "of small worth held"),
                elisions = elisions,
                tokenizer = tokenizer,
            )
        val progress = tracker.progress(heard = "will be a tattered weed of small worth held")
        val completed = progress.isComplete
        return completed
    }

    private val readme = File("README.md").readText()

    @TestFactory
    fun `every Kotlin example in a document is code inside the function a test runs for it`(): List<DynamicTest> =
        DOCUMENTS.map { (path, runBy) ->
            DynamicTest.dynamicTest(path) {
                val examples = fencedBlocks(File(path).readText(), language = "kotlin")
                val source = File(SOURCE).readLines()
                val body = checkNotNull(body(of = runBy, source)) { "$runBy is not a function of this file" }
                val imports = source.filter { it.startsWith("import ") }

                assertTrue(examples.isNotEmpty(), "$path shows no Kotlin")
                for (paragraph in examples.flatMap(::paragraphs)) {
                    val runs = if (paragraph.all { it.startsWith("import ") }) imports else body
                    assertTrue(
                        runs.windowed(paragraph.size).contains(paragraph),
                        "$path shows code $runBy does not run:\n${paragraph.joinToString("\n")}",
                    )
                }
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

    @Test
    fun `the module docs' reading check completes a reading said whole`() {
        assertTrue(completedReading())
    }

    private fun trimmedLines(text: String): List<String> = text.lines().map { it.trim() }

    private fun body(
        of: String,
        source: List<String>,
    ): List<String>? {
        val start = source.indexOfFirst { it.startsWith("    private fun $of(") }.takeIf { it >= 0 } ?: return null
        val open = (start until source.size).firstOrNull { source[it].endsWith("{") } ?: return null
        val close = (open until source.size).firstOrNull { source[it] == "    }" } ?: return null
        return source.subList(open + 1, close).map { it.trim() }
    }

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
        private val DOCUMENTS = listOf("README.md" to "saidEveryWord", "docs/module.md" to "completedReading")
        private val RELEASED = Regex("""^## (\d+\.\d+\.\d+)$""", RegexOption.MULTILINE)
        private val INSTALLED = Regex(""""fm\.apakabar:readaloudkit-kotlin:([^"]+)"""")
    }
}
