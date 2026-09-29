package fm.apakabar.readaloudkit

import com.charleskorn.kaml.YamlNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
data class QuirksCases(
    val tests: List<QuirksCase>,
) {
    companion object {
        val all: List<QuirksCase> by lazy { Cases.load("quirks_tests.yaml", serializer()).tests }
    }
}

@Serializable
data class QuirkQuery(
    val heard: String,
    val written: String,
    val after: String? = null,
    val allowed: Boolean,
)

@Serializable
data class UnknownModelCase(
    val model: String,
    val known: List<String>,
)

@Serializable
data class QuirksCase(
    val name: String,
    val table: String? = null,
    val model: String? = null,
    val allowances: Map<String, List<YamlNode>>? = null,
    @SerialName("unknown_model") val unknownModel: UnknownModelCase? = null,
    val malformed: Boolean? = null,
    val empty: Boolean? = null,
    val queries: List<QuirkQuery>? = null,
) {
    fun quirks(): RecognizerQuirks {
        val table = table ?: return Cases.quirks(allowances)
        val model = checkNotNull(model) { "$name: a table is read for a model" }
        return RecognizerQuirks.decode(table.toByteArray(), model = model)
    }
}

class RecognizerQuirksTests {
    @TestFactory
    fun `allows what the table says`(): List<DynamicTest> =
        Cases.tests(QuirksCases.all, { it.name }) { case ->
            case.unknownModel?.let { expected ->
                val error = assertFailsWith<RecognizerQuirks.UnknownModel> { case.quirks() }
                assertEquals(expected.model, error.model)
                assertEquals(expected.known, error.known.sorted())
                return@tests
            }
            if (case.malformed == true) {
                assertFailsWith<SerializationException> { case.quirks() }
                return@tests
            }
            val quirks = case.quirks()
            assertEquals(checkNotNull(case.empty) { "a readable case pins emptiness" }, quirks.isEmpty)
            for (query in case.queries ?: emptyList()) {
                assertEquals(
                    query.allowed,
                    quirks.allows(query.heard, forWritten = query.written, after = query.after),
                    "${query.heard} for ${query.written}",
                )
            }
        }
}
