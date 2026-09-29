package fm.apakabar.readaloudkit

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * A narration alignment as the server publishes it: the alignment and the version it
 * was published under.
 *
 * @property version The version the server published the alignment under.
 * @property alignment The published word timings.
 */
data class PublishedAlignment(
    val version: String,
    val alignment: NarrationAlignment,
) {
    companion object {
        private val FIELDS = setOf("version", "alignment")

        /**
         * Decodes `{"version": ..., "alignment": {...}}`, the document the server serves,
         * refusing a field it does not have.
         *
         * @throws NarrationAlignment.TimingError naming the first word whose times cannot stand.
         * @throws SerializationException when [data] is not such a document.
         */
        fun decode(data: ByteArray): PublishedAlignment {
            val fields =
                Json.parseToJsonElement(utf8(data)) as? JsonObject
                    ?: throw SerializationException("a published alignment is not an object")
            refuseFields(fields, otherThan = FIELDS, of = "a published alignment")
            val version =
                NarrationAlignment.string(fields["version"], "version")
                    ?: throw SerializationException("the published alignment has no version")
            val alignment = fields["alignment"] ?: throw SerializationException("the published alignment has no alignment")
            return PublishedAlignment(version = version, alignment = NarrationAlignment.read(alignment))
        }
    }
}
