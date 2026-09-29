package fm.apakabar.readaloudkit

import fm.apakabar.readalign.normalize

/**
 * The full form of each elided spelling a work prints, from the work's own data.
 *
 * A recogniser writes out an elision in full: it hears `tattered` where the page has
 * `tatter’d`. Which spellings are elisions, and what they stand for, belongs to the
 * language and the edition, so the library holds no rule of its own and credits only
 * the full forms it is given. Spellings are compared as ReadAlign normalizes them.
 */
class Elisions(
    fullForms: Map<String, String>,
) {
    private val fullForms: Map<String, String> = fullForms.entries.associate { (elided, full) -> normalize(elided) to normalize(full) }

    /** The normalized full form the work gives for [written], if it is a listed elision. */
    fun fullForm(of: String): String? = fullForms[normalize(of)]

    override fun equals(other: Any?): Boolean = other is Elisions && other.fullForms == fullForms

    override fun hashCode(): Int = fullForms.hashCode()

    override fun toString(): String = "Elisions($fullForms)"

    companion object {
        /** No elided spelling is restored. */
        val none = Elisions(emptyMap())
    }
}
