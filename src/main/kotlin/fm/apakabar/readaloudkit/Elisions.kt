package fm.apakabar.readaloudkit

import fm.apakabar.readalign.normalize

/**
 * The full forms of each elided spelling a work prints, from the work's own data.
 *
 * A recogniser writes out an elision in full: it hears `tattered` where the page has
 * `tatter’d`. Which spellings are elisions, and what they stand for, belongs to the
 * language and the edition, so the library holds no rule of its own and credits only
 * the full forms it is given. One spelling may stand for several full forms, any of
 * which counts as said. Spellings are compared as ReadAlign normalizes them, and
 * spellings that normalize alike are merged into one.
 */
class Elisions(
    fullForms: Map<String, List<String>>,
) {
    private val fullForms: Map<String, Set<String>> =
        buildMap<String, MutableSet<String>> {
            for ((elided, full) in fullForms) {
                getOrPut(normalize(elided)) { mutableSetOf() }.addAll(full.map(::normalize))
            }
        }

    /** The normalized full forms the work gives for [of]; empty if it lists none. */
    fun fullForms(of: String): Set<String> = fullForms[normalize(of)] ?: emptySet()

    override fun equals(other: Any?): Boolean = other is Elisions && other.fullForms == fullForms

    override fun hashCode(): Int = fullForms.hashCode()

    override fun toString(): String = "Elisions($fullForms)"

    companion object {
        /** No elided spelling is restored. */
        val none = Elisions(emptyMap())
    }
}
