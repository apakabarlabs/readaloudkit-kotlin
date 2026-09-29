package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaveformEnvelopeTests {
    @Test
    fun `silence stays flat and sound keeps its shape`() {
        val envelope = WaveformEnvelope.make(from = FloatArray(8) + FloatArray(8) { 0.5f }, bars = 4)

        assertEquals(4, envelope.size)
        assertEquals(0.0, envelope[0])
        assertEquals(0.0, envelope[1])
        assertTrue(envelope[2] > 0.8)
        assertTrue(envelope[3] > 0.8)
    }

    @Test
    fun `a short recording does not invent empty bars`() {
        assertEquals(2, WaveformEnvelope.make(from = floatArrayOf(0.1f, 0.2f), bars = 48).size)
        assertTrue(WaveformEnvelope.make(from = FloatArray(0), bars = 48).isEmpty())
    }
}
