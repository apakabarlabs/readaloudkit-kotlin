package fm.apakabar.readaloudkit

import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CasesTests {
    @Test
    fun `a key no runner reads fails the case file`() {
        val misspelt = """{name: "a typo", line: "rose", interior_mark: "-", interior_marks: "-", words: ["rose"]}"""

        assertFailsWith<SerializationException> { Cases.decode(misspelt, WordsCase.serializer()) }
    }

    @Test
    fun `a key no runner reads fails the case file inside an allowance too`() {
        val misspelt =
            """
            {name: "a typo", lines: ["in sense"], interior_marks: "-", line_lengths: [2],
             quirks: {"in sense": [{heard: "incense", before: "bring"}]}}
            """.trimIndent()

        assertFailsWith<IllegalStateException> { Cases.decode(misspelt, TrackerCase.serializer()).tracker }
    }
}
