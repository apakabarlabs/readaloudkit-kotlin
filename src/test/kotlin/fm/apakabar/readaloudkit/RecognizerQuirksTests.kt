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
data class WrongBuildCase(
    val requested: String,
    val published: String,
)

@Serializable
data class QuirksCase(
    val name: String,
    val served: String? = null,
    val table: String? = null,
    val build: String? = null,
    val allowances: Map<String, List<YamlNode>>? = null,
    @SerialName("wrong_build") val wrongBuild: WrongBuildCase? = null,
    val malformed: Boolean? = null,
    val empty: Boolean? = null,
    val queries: List<QuirkQuery>? = null,
) {
    fun quirks(): RecognizerQuirks {
        val data = served?.let(Cases::bytes) ?: table?.toByteArray() ?: return Cases.quirks(allowances)
        val build = checkNotNull(build) { "$name: a table is read for a build" }
        return RecognizerQuirks.decode(data, build = build)
    }
}

class RecognizerQuirksTests {
    @TestFactory
    fun `allows what the table says`(): List<DynamicTest> =
        Cases.tests(QuirksCases.all, { it.name }) { case ->
            case.wrongBuild?.let { expected ->
                assertEquals(
                    RecognizerQuirks.WrongBuild(requested = expected.requested, published = expected.published),
                    assertFailsWith<RecognizerQuirks.WrongBuild> { case.quirks() },
                )
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
