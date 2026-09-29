package fm.apakabar.readaloudkit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PlaybackEnvelopeTests {
    @Test
    fun `a cut segment reaches silence at both edges`() {
        assertEquals(0f, gain(at = 0))
        assertEquals(0.5f, gain(at = 5))
        assertEquals(1f, gain(at = 10))
        assertEquals(1f, gain(at = 89))
        assertEquals(0.5f, gain(at = 94))
        assertEquals(0f, gain(at = 99))
    }

    @Test
    fun `an uncut recording keeps every sample`() {
        for (frame in 0L until 100L) {
            assertEquals(1f, gain(at = frame, fadesIn = false, fadesOut = false))
        }
    }

    private fun gain(
        at: Long,
        fadesIn: Boolean = true,
        fadesOut: Boolean = true,
    ): Float =
        PlaybackEnvelope.gain(
            at = at,
            frameCount = 100,
            fadeFrameCount = 10,
            fadesIn = fadesIn,
            fadesOut = fadesOut,
        )
}
