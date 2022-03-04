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
            val knowledgeLevel = calculateKnowledgeLevelOf(author, history)
            if (knowledgeLevel.isInfinite() || knowledgeLevel.isNaN() || knowledgeLevel < 0 || knowledgeLevel > 1) {
                throw InternalError("Bad CodeOwnerFinder: return knowledge level is '$knowledgeLevel', should be a real between 0 and 1.")
            }
            knowledgeLevel
        }
        return CodeOwnerResult(authorToKnowledgeLevel)
    }
}

@Suppress("unused")
object SummarizedCodeOwnerFinder : AuthorIndependentCodeOwnerFinder() {
    data class SummarizedRevisionChange(val author: String, val totalChangedLines: Int)

    private fun DiffRevision.toSummarizedRevisionChange(): SummarizedRevisionChange {
        return SummarizedRevisionChange(
            author = author,
            totalChangedLines = changes.sumOf { it.deleted + it.inserted },
        )
    }

    override fun calculateKnowledgeLevelOf(author: String, history: DiffHistory): Double {
        val summarizedChanges = history.revisions.map { it.toSummarizedRevisionChange() }
        val byAuthor = summarizedChanges.filter { it.author == author }
        return sumOfChanges(byAuthor).toDouble() / sumOfChanges(summarizedChanges).toDouble()
    }

    private fun sumOfChanges(changes: List<SummarizedRevisionChange>): Int {
        return changes.sumOf { it.totalChangedLines }
    }
}