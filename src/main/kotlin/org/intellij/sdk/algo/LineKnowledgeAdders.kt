package org.intellij.sdk.algo

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * An algorithm, which calculates
 * how knowledge is added to the lines
 * in the developer's [KnowledgeState],
 * when somebody makes a change to the file.
 */
interface LineKnowledgeAdder {
    /**
     * Calculates the knowledge addition
     * based on knowledge [state]
     * and [insertedLineIndices] (enumerated from 0).
     *
     * @return a list of reals from 0 to 1 -
     * the amount of knowledge to be added
     * to each line correspondingly.
     */
    fun calculateAddition(
        state: KnowledgeState,
        insertedLineIndices: List<Int>,
    ): List<Double>

    /**
     * Adds the addition calculated in [calculateAddition]
     * to given knowledge [state].
     *
     * @return new [KnowledgeState] with added knowledge.
     */
    fun add(
        state: KnowledgeState,
        insertedLineIndices: List<Int>,
    ): KnowledgeState {
        val addition = calculateAddition(state, insertedLineIndices)
        if (addition.size != state.lines.size) {
            throw InternalError(
                "Bad LineKnowledgeAdder implementation: calculateAddition() returned addition," +
                        " the size of which (${addition.size}) does not match with the size of the file (${state.lines.size}"
            )
        }
        val lines = state.lines.mapIndexed { index, lineWithKnowledge ->
            val unsafeNewKnowledge = lineWithKnowledge.knowledge + addition[index]
            lineWithKnowledge.withKnowledge(
                min(1.0, max(0.0, unsafeNewKnowledge))
            )
        }
        return state.withLines(lines)
    }
}

/**
 * Main implementation of [LineKnowledgeAdder].
 */
object LineKnowledgeAdderImpl : LineKnowledgeAdder {
    private const val SPREAD_COEFFICIENT = 6 // to one direction
    private const val SAME_AUTHOR_WRITING_KNOWLEDGE = 1.0
    private const val OTHER_AUTHOR_WRITING_KNOWLEDGE = 0.0

    override fun calculateAddition(
        state: KnowledgeState,
        insertedLineIndices: List<Int>
    ) = AdditionBuilder(state, insertedLineIndices).build()

    private class AdditionBuilder(
        state: KnowledgeState,
        private val insertedLineIndices: List<Int>,
    ) {
        private val developer = state.developer
        private val lines = state.lines
        private val size = lines.size

        private val addition = MutableList(size) { 0.0 }

        fun build(): List<Double> {
            insertedLineIndices.forEach { lineIndex ->
                addKnowledge(lineIndex)
            }
            return addition
        }

        private fun addAt(lineIndex: Int, value: Double) {
            // We don't want to add something twice
            // to the same line, so we just take
            // the largest of additions for each line.
            addition[lineIndex] = max(addition[lineIndex], value)
        }

        private fun addKnowledge(lineIndex: Int) {
            // knowledge by writing --- added to this exact line
            val writingKnowledge = if (lines[lineIndex].line.author == developer) {
                SAME_AUTHOR_WRITING_KNOWLEDGE
            } else {
                OTHER_AUTHOR_WRITING_KNOWLEDGE
            }
            addAt(lineIndex, writingKnowledge)

            // knowledge by reading --- add to the lines around it
            val w = lines[lineIndex].line.weight
            if (w <= 0) {
                return // nothing to add
            }
            val spreadWeight = (w * writingKnowledge * SPREAD_COEFFICIENT).roundToInt()
            for (direction in listOf(+1, -1)) {
                spreadReadingKnowledge(lineIndex + direction, direction, spreadWeight)
            }
        }

        /**
         * Spreads knowledge in a given [direction] from [startingIndex].
         */
        private fun spreadReadingKnowledge(
            startingIndex: Int,
            direction: Int,
            spreadWeight: Int,
        ) {
            assert(direction in listOf(+1, -1))
            var index = startingIndex
            var remainingWeight = spreadWeight
            while (index in addition.indices && remainingWeight > 0) {
                spreadReadingKnowledgeAt(index, spreadWeight, remainingWeight)
                remainingWeight -= lines[index].line.weight
                index += direction
            }
        }

        // 1 + 2 + ... + r
        private fun sum(r: Int): Int = r * (r + 1) / 2

        // l + (l + 1) + ... + r
        private fun sum(l: Int, r: Int): Int {
            if (l > r) return 0
            return sum(r) - sum(l - 1)
        }

        private fun spreadReadingKnowledgeAt(
            index: Int,
            spreadWeight: Int,
            remainingWeight: Int,
        ) {
            val weight = lines[index].line.weight
            if (weight <= 0) {
                return // nothing to add
            }
            val absoluteDelta = if (remainingWeight < weight) {
                sum(1, remainingWeight).toDouble() / spreadWeight.toDouble()
            } else {
                sum(remainingWeight - weight + 1, remainingWeight).toDouble() / spreadWeight.toDouble()
            }
            val relativeDelta = absoluteDelta / weight
            addAt(index, relativeDelta)
        }
    }
}

