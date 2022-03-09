package org.intellij.sdk.algo

import org.intellij.sdk.diff.*
import org.intellij.sdk.*


/**
 * A [CodeOwnerFinder], which calculates
 * developers' knowledge levels independently of each other,
 * and then groups them into [CodeOwnerResult].
 */
abstract class DeveloperIndependentCodeOwnerFinder : CodeOwnerFinder {
    abstract fun calculateKnowledgeLevelOf(developer: String, history: DiffHistory): Double

    override fun find(history: DiffHistory): CodeOwnerResult {
        val developers = history.revisions.map { it.author }.toSet()
        val developerToKnowledgeLevel = developers.associateWith { developer ->
            val knowledgeLevel = calculateKnowledgeLevelOf(developer, history)
            if (knowledgeLevel.isInfinite() || knowledgeLevel.isNaN() || knowledgeLevel < 0 || knowledgeLevel > 1) {
                throw InternalError(
                    "Bad CodeOwnerFinder: returned knowledge level of developer $developer is '$knowledgeLevel', should be a real between 0 and 1."
                )
            }
            knowledgeLevel
        }
        return CodeOwnerResult(developerToKnowledgeLevel)
    }
}

/**
 * Simple implementation of [CodeOwnerFinder]:
 * It calculates developer's knowledge level
 * as the proportion of changes that belong to him.
 */
@Suppress("unused")
object SummarizedCodeOwnerFinder : DeveloperIndependentCodeOwnerFinder() {
    data class SummarizedRevisionChange(val author: String, val totalChangedLines: Int)

    private fun DiffRevision.toSummarizedRevisionChange(): SummarizedRevisionChange {
        return SummarizedRevisionChange(
            author = author,
            totalChangedLines = changes.sumOf { it.deleted + it.inserted },
        )
    }

    override fun calculateKnowledgeLevelOf(developer: String, history: DiffHistory): Double {
        val summarizedChanges = history.revisions.map { it.toSummarizedRevisionChange() }
        val byDeveloper = summarizedChanges.filter { it.author == developer }
        return sumOfChanges(byDeveloper).toDouble() / sumOfChanges(summarizedChanges).toDouble()
    }

    private fun sumOfChanges(changes: List<SummarizedRevisionChange>): Int {
        return changes.sumOf { it.totalChangedLines }
    }
}

/**
 * An implementation of [CodeOwnerFinder],
 * which calculates the knowledge level
 * of each developer independently,
 * by given [knowledgeStateCalculator].
 */
class KnowledgeStateCodeOwnerFinder(
    private val knowledgeStateCalculator: KnowledgeStateCalculator = KnowledgeStateCalculatorImpl(),
) : DeveloperIndependentCodeOwnerFinder() {

    override fun calculateKnowledgeLevelOf(developer: String, history: DiffHistory): Double {
        val knowledgeState = history.revisions.fold(KnowledgeState.initialState(developer)) { knowledgeState, diffRevision ->
            knowledgeStateCalculator.nextKnowledgeState(knowledgeState, diffRevision)
        }
        val upToDateState = knowledgeStateCalculator.updateKnowledgeStateToPresent(knowledgeState)
        return upToDateState.totalKnowledgeLevel
    }
}
