package fm.apakabar.readaloudkit

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CaseCopyTests {
    private fun fetch(address: String): ByteArray {
        val request =
            HttpRequest
                .newBuilder(URI.create(address))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build()
        val answer = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(OK, answer.statusCode(), "$address answered ${answer.statusCode()}")
        return answer.body()
    }

    private fun isShared(name: String): Boolean = name.endsWith(".yaml") || name.endsWith(".json")

    @TestFactory
    fun `every shared case file is the leading port's own, byte for byte`(): List<DynamicTest> =
        Cases.shared.map { name ->
            DynamicTest.dynamicTest(name) {
                assertContentEquals(
                    fetch("$LEAD_FILES/$name"),
                    Cases.bytes(name),
                    "$name differs from the leading port: run `make sync-yaml` in readaloudkit-swift",
                )
            }
        }

    @Test
    fun `the list of shared files is the leading port's directory on main`() {
        val listing = Json.parseToJsonElement(fetch(LEAD_DIRECTORY).decodeToString()) as JsonArray
        val lead =
            listing
                .map { entry -> ((entry as JsonObject)["name"] as JsonPrimitive).content }
                .filter(::isShared)

        assertEquals(
            lead.sorted(),
            Cases.shared.sorted(),
            "the leading port's cases differ from the list: run `make sync-yaml` in readaloudkit-swift",
        )
    }

    @Test
    fun `every case file copied from the leading port is in the list`() {
        val copied = File("src/test/resources").list { _, name -> isShared(name) }
        assertEquals(Cases.shared.sorted(), checkNotNull(copied) { "src/test/resources is not a directory" }.sorted())
    }

    companion object {
        private const val RESOURCES = "Tests/ReadAloudKitTests/Resources"
        private const val LEAD_FILES = "https://raw.githubusercontent.com/apakabarlabs/readaloudkit-swift/main/$RESOURCES"
        private const val LEAD_DIRECTORY = "https://api.github.com/repos/apakabarlabs/readaloudkit-swift/contents/$RESOURCES?ref=main"
        private const val OK = 200
        private const val TIMEOUT_SECONDS = 10L
        private val CLIENT: HttpClient = HttpClient.newHttpClient()
    }
}
