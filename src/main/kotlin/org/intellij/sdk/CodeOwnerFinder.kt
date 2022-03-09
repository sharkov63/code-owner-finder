package org.intellij.sdk

import org.intellij.sdk.diff.DiffHistory

/**
 * The final result of code owner calculation.
 * It contains developers, assigned with their knowledge level.
 *
 * "Knowledge level" is a measure of
 * how much of the file a developer understands
 * at the current moment of time.
 * Developers with higher knowledge level
 * should be better candidates for "code owner" than
 * developers with lower knowledge level.
 *
 * Knowledge level boundaries should be between 0 and 1.
 */
data class CodeOwnerResult(
    val developerToKnowledgeLevel: Map<String, Double>
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