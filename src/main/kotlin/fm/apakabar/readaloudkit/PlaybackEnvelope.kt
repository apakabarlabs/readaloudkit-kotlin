package fm.apakabar.readaloudkit

import kotlin.math.max
import kotlin.math.min

/** Shapes the gain at the edges of a playback buffer. */
object PlaybackEnvelope {
    /**
     * Returns the gain for one frame, applying the requested linear edge fades.
     *
     * A buffer shorter than two frames or a nonpositive fade length returns unity.
     * Frames outside the buffer are clamped to zero when an applicable fade is enabled.
     */
    fun gain(
        at: Long,
        frameCount: Long,
        fadeFrameCount: Long,
        fadesIn: Boolean,
        fadesOut: Boolean,
    ): Float {
        if (frameCount <= 1 || fadeFrameCount <= 0) return 1f
        var gain = 1.0
        if (fadesIn) gain = min(gain, at.toDouble() / fadeFrameCount.toDouble())
        if (fadesOut) {
            gain = min(gain, (frameCount - at - 1).toDouble() / fadeFrameCount.toDouble())
        }
        return max(gain, 0.0).toFloat()
    }
}
