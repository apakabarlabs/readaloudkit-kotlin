package fm.apakabar.readaloudkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

@Serializable
data class AudioCases(
    val gain: List<GainCase>,
    val waveform: List<WaveformCase>,
) {
    companion object {
        val all: AudioCases by lazy { Cases.load("audio_tests.yaml", serializer()) }
    }
}

@Serializable
data class GainCase(
    val name: String,
    @SerialName("frame_count") val frameCount: Long,
    @SerialName("fade_frame_count") val fadeFrameCount: Long,
    @SerialName("fades_in") val fadesIn: Boolean,
    @SerialName("fades_out") val fadesOut: Boolean,
    val gains: List<List<Double>>? = null,
    @SerialName("every_frame") val everyFrame: Float? = null,
) {
    fun gain(at: Long): Float =
        PlaybackEnvelope.gain(
            at = at,
            frameCount = frameCount,
            fadeFrameCount = fadeFrameCount,
            fadesIn = fadesIn,
            fadesOut = fadesOut,
        )
}

class PlaybackEnvelopeTests {
    @TestFactory
    fun `fades the edges`(): List<DynamicTest> =
        Cases.tests(AudioCases.all.gain, { it.name }) { case ->
            for ((frame, gain) in case.gains ?: emptyList()) {
                assertEquals(gain.toFloat(), case.gain(at = frame.toLong()), "frame $frame")
            }
            case.everyFrame?.let { every ->
                for (frame in 0 until case.frameCount) assertEquals(every, case.gain(at = frame), "frame $frame")
            }
        }
}
