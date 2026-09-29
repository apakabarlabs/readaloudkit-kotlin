package fm.apakabar.readaloudkit

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject

internal fun refuseFields(
    fields: JsonObject,
    otherThan: Set<String>,
    of: String,
) {
    val stray = fields.keys.firstOrNull { it !in otherThan } ?: return
    throw SerializationException("$stray is not a field of $of")
}
