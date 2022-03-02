package org.intellij.sdk.action

/**
 * The final result of code owner calculation.
 * It contains authors, assigned with their knowledge level.
 *
 * "Knowledge level" is a measure of
 * how much of the file an author understands
 * at the current moment of time.
 * Authors with higher knowledge level
 * should be better candidates for "code owner" than
 * authors with lower knowledge level.
 *
 * Knowledge level boundaries might depend on implementation,
 * but are usually between 0 and 1.
 */
data class CodeOwnerResult(
    val authorToKnowledgeLevel: Map<String, Double>
)

/**
 * An algorithm, which calculates code owner
 * by given [History] of the file.
 */
interface CodeOwnerFinder {
    /**
     * Calculate [CodeOwnerResult] by given [History] of the file.
     */
    fun find(history: History): CodeOwnerResult
}

/**
 * A [CodeOwnerFinder], which calculates
 * authors' knowledge levels independently of each other,
 * and then groups them into [CodeOwnerResult].
 */
abstract class AuthorIndependentCodeOwnerFinder : CodeOwnerFinder {
    abstract fun calculateKnowledgeLevelOf(author: String, history: History): Double

    override fun find(history: History): CodeOwnerResult {
        val authors = history.changes.map { it.author }.toSet()
        val authorToKnowledgeLevel = authors.map { author ->
            author to calculateKnowledgeLevelOf(author, history)
        }.toMap()
        return CodeOwnerResult(authorToKnowledgeLevel)
    }
}

/**
 * Main implementation of [CodeOwnerFinder].
 */
object MainCodeOwnerFinderImpl : CodeOwnerFinder {
    override fun find(history: History): CodeOwnerResult {
        TODO("Not yet implemented")
    }
}