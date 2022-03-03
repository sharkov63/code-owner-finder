package org.intellij.sdk.action

/**
 * Represents a single line (row) in a specific version of a file.
 *
 * We are only interested in the [author] of the line,
 * and it's [weight] --- a quantity which tells us how much
 * "useful information" this line holds.
 *
 * A [weight] can be defined in many ways,
 * see [LineWeightCalculator].
 */
data class RevisionLine(val author: String, val weight: Int)


/**
 * Any object which can calculate [RevisionLine] weight.
 */
interface LineWeightCalculator {
    fun calculateLineWeight(line: String): Int
}

/**
 * Simplest implementation of [LineWeightCalculator],
 * which assigns weight to be line length.
 */
object LengthLineWeightCalculator : LineWeightCalculator {
    override fun calculateLineWeight(line: String) = line.length
}
