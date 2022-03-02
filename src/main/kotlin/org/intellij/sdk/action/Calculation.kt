package org.intellij.sdk.action

class CodeOwnerResult(val _temp: String = "")

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
 * Main implementation of [CodeOwnerFinder].
 */
object MainCodeOwnerFinderImpl : CodeOwnerFinder {
    override fun find(history: History): CodeOwnerResult {
        TODO("Not yet implemented")
    }
}