package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecognizerQuirksTests {
    private val table =
        """
        {
            "parakeet": {
                "tatter'd": ["tattered"],
                "in": [{"heard": "and", "after": "each"}]
            },
            "spotless": {}
        }
        """.trimIndent().toByteArray()

    @Test
    fun `a model's patches are read from its own section`() {
        val quirks = RecognizerQuirks.decode(table, model = "parakeet")
        assertTrue(quirks.allows("tattered", forWritten = "tatter'd"))
        assertFalse(quirks.allows("tattered", forWritten = "battered"))
    }

    @Test
    fun `a patch tied to a phrase fires there and nowhere else`() {
        val quirks = RecognizerQuirks.decode(table, model = "parakeet")
        assertTrue(quirks.allows("and", forWritten = "in", after = "each"))
        assertFalse(quirks.allows("and", forWritten = "in", after = "delights"))
        assertFalse(quirks.allows("and", forWritten = "in"))
    }

    @Test
    fun `a model with an empty section needs no patches`() {
        val quirks = RecognizerQuirks.decode(table, model = "spotless")
        assertTrue(quirks.isEmpty)
        assertFalse(quirks.allows("tattered", forWritten = "tatter'd"))
    }

    @Test
    fun `two spellings of one key are added together, not one over the other`() {
        val quirks =
            RecognizerQuirks(
                allowances =
                    mapOf(
                        "Whate’er" to listOf("whatever"),
                        "whate’er" to listOf("what’er"),
                    ),
            )
        assertTrue(quirks.allows("whatever", forWritten = "Whate’er"))
        assertTrue(quirks.allows("whater", forWritten = "whate’er"))
        assertFalse(quirks.allows("whenever", forWritten = "whate’er"))
    }

    @Test
    fun `a model the table says nothing about is refused, not treated as spotless`() {
        assertFailsWith<RecognizerQuirks.UnknownModel> {
            RecognizerQuirks.decode(table, model = "parakeet-v4")
        }
    }
}
