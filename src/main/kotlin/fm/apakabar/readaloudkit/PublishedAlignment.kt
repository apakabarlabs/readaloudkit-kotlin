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
        /**
         * Decodes `{"version": ..., "alignment": {...}}`, the document the server serves.
         *
         * A missing field or a value of another type is refused. A field the document does
         * not know is read past, at any depth, so that a field the server adds later does
         * not stop a build already installed. A key repeated within one object keeps one of
         * its values; which one is not promised and may differ between ports. The document
         * is read as UTF-8, with or without a byte order mark.
         *
         * @throws NotUTF8 when [data] is text in another encoding.
         * @throws NarrationAlignment.TimingError naming the first word whose times cannot stand.
         * @throws SerializationException when [data] is not such a document.
         */
        fun decode(data: ByteArray): PublishedAlignment {
            val fields =
                Json.parseToJsonElement(utf8(data)) as? JsonObject
                    ?: throw SerializationException("a published alignment is not an object")
            val version =
                NarrationAlignment.string(fields["version"], "version")
                    ?: throw SerializationException("the published alignment has no version")
            val alignment = fields["alignment"] ?: throw SerializationException("the published alignment has no alignment")
            return PublishedAlignment(version = version, alignment = NarrationAlignment.read(alignment))
        }
    }
}
