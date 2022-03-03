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
 * by given [DiffHistory] of the file.
 */
interface CodeOwnerFinder {
    /**
     * Calculate [CodeOwnerResult] by given [DiffHistory] of the file.
     */
    fun find(history: DiffHistory): CodeOwnerResult
}

/**
 * A [CodeOwnerFinder], which calculates
 * authors' knowledge levels independently of each other,
 * and then groups them into [CodeOwnerResult].
 */
abstract class AuthorIndependentCodeOwnerFinder : CodeOwnerFinder {
    abstract fun calculateKnowledgeLevelOf(author: String, history: DiffHistory): Double

    override fun find(history: DiffHistory): CodeOwnerResult {
        val authors = history.revisions.map { it.author }.toSet()
        val authorToKnowledgeLevel = authors.associateWith { author ->
            calculateKnowledgeLevelOf(author, history)
        }
        return CodeOwnerResult(authorToKnowledgeLevel)
    }
}

/**
 * Main implementation of [CodeOwnerFinder].
 */
object MainCodeOwnerFinderImpl : AuthorIndependentCodeOwnerFinder() {
    override fun calculateKnowledgeLevelOf(author: String, history: DiffHistory): Double {
//        val summarizedChanges = history.changes.map { it.toSummarizedRevisionChange() }
//        val authors = summarizedChanges.filter { it.author == author }
//        return sumOfChanges(authors).toDouble() / sumOfChanges(summarizedChanges).toDouble()
        TODO()
    }

//    private fun sumOfChanges(changes: List<SummarizedRevisionChange>): Int {
//        return changes.sumOf { it.sumChange.inserted + it.sumChange.deleted }
//    }
}