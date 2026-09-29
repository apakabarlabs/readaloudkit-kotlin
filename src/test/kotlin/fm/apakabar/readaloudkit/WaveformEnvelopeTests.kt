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
    val envelope: List<Double>,
) {
    val built: FloatArray get() = samples.flatMap { run -> List(run.count) { run.value } }.toFloatArray()
}

class WaveformEnvelopeTests {
    @TestFactory
    fun `keeps the shape`(): List<DynamicTest> =
        Cases.tests(AudioCases.all.waveform, { it.name }) { case ->
            val envelope = WaveformEnvelope.make(from = case.built, bars = case.bars)

            assertEquals(case.envelope.size, envelope.size)
            for ((bar, pair) in envelope.zip(case.envelope).withIndex()) {
                assertTrue(Cases.close(pair.first, pair.second), "bar $bar is ${pair.first}")
            }
        }
}
