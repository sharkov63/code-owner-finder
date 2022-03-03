package org.intellij.sdk.action

/**
 * Any object which can calculate the weight of a [DiffLine].
 */
interface LineWeightCalculator {
    fun calculate(line: String): Int
}

/**
 * Simplest implementation of [LineWeightCalculator],
 * which assigns weight to be line length.
 */
object LengthLineWeightCalculator : LineWeightCalculator {
    override fun calculate(line: String) = line.length
}
