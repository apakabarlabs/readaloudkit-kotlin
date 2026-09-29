package fm.apakabar.readaloudkit

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
    private fun fetch(name: String): ByteArray {
        val address = "$LEAD/$name"
        val request =
            HttpRequest
                .newBuilder(URI.create(address))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build()
        val answer = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(OK, answer.statusCode(), "$address answered ${answer.statusCode()}")
        return answer.body()
    }

    @TestFactory
    fun `every shared case file is the leading port's own, byte for byte`(): List<DynamicTest> =
        Cases.shared.map { name ->
            DynamicTest.dynamicTest(name) {
                assertContentEquals(
                    fetch(name),
                    Cases.bytes(name),
                    "$name differs from the leading port: run `make sync-yaml`",
                )
            }
        }

    @Test
    fun `every case file copied from the leading port is in the list`() {
        val copied = File("src/test/resources").list { _, name -> name.endsWith(".yaml") || name.endsWith(".json") }
        assertEquals(Cases.shared.sorted(), checkNotNull(copied) { "src/test/resources is not a directory" }.sorted())
    }

    companion object {
        private const val LEAD =
            "https://raw.githubusercontent.com/apakabarlabs/readaloudkit-swift/main/Tests/ReadAloudKitTests/Resources"
        private const val OK = 200
        private const val TIMEOUT_SECONDS = 10L
        private val CLIENT: HttpClient = HttpClient.newHttpClient()
    }
}
