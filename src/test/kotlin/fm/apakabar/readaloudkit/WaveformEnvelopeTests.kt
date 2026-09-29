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
data class WaveformCase(
    val name: String,
    val samples: List<SampleRun>,
    val bars: Int,
    val count: Int,
    val exactly: List<List<Double>>? = null,
    val above: List<List<Double>>? = null,
) {
    val built: FloatArray get() = samples.flatMap { run -> List(run.count) { run.value } }.toFloatArray()
}

class WaveformEnvelopeTests {
    @TestFactory
    fun `keeps the shape`(): List<DynamicTest> =
        Cases.tests(AudioCases.all.waveform, { it.name }) { case ->
            val envelope = WaveformEnvelope.make(from = case.built, bars = case.bars)

            assertEquals(case.count, envelope.size)
            for ((bar, value) in case.exactly ?: emptyList()) assertEquals(value, envelope[bar.toInt()], "bar $bar")
            for ((bar, value) in case.above ?: emptyList()) assertTrue(envelope[bar.toInt()] > value, "bar $bar")
        }
}
