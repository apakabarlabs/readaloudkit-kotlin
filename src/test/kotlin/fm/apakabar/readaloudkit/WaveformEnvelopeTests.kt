package fm.apakabar.readaloudkit

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
data class SampleRun(
    val value: Float,
    val count: Int,
)

@Serializable
data class BarRun(
    val value: Double,
    val count: Int,
)

@Serializable
data class WaveformCase(
    val name: String,
    val samples: List<SampleRun>,
    val bars: Int,
    val envelope: List<BarRun>,
) {
    val built: FloatArray get() = samples.flatMap { run -> List(run.count) { run.value } }.toFloatArray()

    val expected: List<Double> get() = envelope.flatMap { run -> List(run.count) { run.value } }
}

class WaveformEnvelopeTests {
    @TestFactory
    fun `keeps the shape`(): List<DynamicTest> =
        Cases.tests(AudioCases.all.waveform, { it.name }) { case ->
            val envelope = WaveformEnvelope.make(from = case.built, bars = case.bars)
            val expected = case.expected

            assertEquals(expected.size, envelope.size)
            val wrong =
                envelope
                    .zip(expected)
                    .withIndex()
                    .filter { (_, pair) -> !Cases.close(pair.first, pair.second) }
                    .map { (bar, pair) -> "bar $bar is ${pair.first}" }
            assertTrue(wrong.isEmpty(), wrong.take(3).joinToString(", "))
        }
}
