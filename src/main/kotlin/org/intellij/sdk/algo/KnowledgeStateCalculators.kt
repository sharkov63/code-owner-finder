package org.intellij.sdk.algo

import kotlinx.datetime.Clock
import org.intellij.sdk.diff.DiffExecutor
import org.intellij.sdk.diff.DiffExecutorHandler
import org.intellij.sdk.diff.DiffRevision
import org.intellij.sdk.toKotlinInstant

/**
 * An object which can update [KnowledgeState],
 * after certain events happened:
 * * a new commit has taken place: [nextKnowledgeState]
 * * some time passed: [updateKnowledgeStateToPresent]
 */
interface KnowledgeStateCalculator {
    fun nextKnowledgeState(state: KnowledgeState, nextRevision: DiffRevision): KnowledgeState
    fun updateKnowledgeStateToPresent(state: KnowledgeState): KnowledgeState
}

/**
 * A [KnowledgeStateCalculator], which is based on a given [oblivionFunction].
 */
abstract class OblivionKnowledgeStateCalculator(
    private val oblivionFunction: OblivionFunction = OblivionFunctionImpl,
) : KnowledgeStateCalculator {

    override fun nextKnowledgeState(state: KnowledgeState, nextRevision: DiffRevision): KnowledgeState {
        val instant1 = state.date.toKotlinInstant()
        val instant2 = nextRevision.date.toKotlinInstant()
        val timePassed = instant2 - instant1
        val upToDateState = oblivionFunction.applyToKnowledgeState(state, timePassed)
        return instantNextKnowledgeState(upToDateState, nextRevision)
    }

    override fun updateKnowledgeStateToPresent(state: KnowledgeState): KnowledgeState {
        val timePassed = Clock.System.now() - state.date.toKotlinInstant()
        return oblivionFunction.applyToKnowledgeState(state, timePassed)
    }

    /**
     * The only abstract method:
     * we need to define how [KnowledgeState]
     * changes when a commit happens.
     */
    abstract fun instantNextKnowledgeState(
        upToDateState: KnowledgeState,
        nextRevision: DiffRevision,
    ): KnowledgeState
}


/**
 * Main implementation of [KnowledgeStateCalculator].
 *
 * Works as an [OblivionKnowledgeStateCalculator] with given [oblivionFunction].
 * A commit change is handled by [adder].
 */
class KnowledgeStateCalculatorImpl(
    oblivionFunction: OblivionFunction = OblivionFunctionImpl,
    private val adder: LineKnowledgeAdder = LineKnowledgeAdderImpl,
) : OblivionKnowledgeStateCalculator(oblivionFunction) {

    override fun instantNextKnowledgeState(
        upToDateState: KnowledgeState,
        nextRevision: DiffRevision,
    ): KnowledgeState {
        val lines: MutableList<LineWithKnowledge> = mutableListOf()
        val insertedLineIndices: MutableList<Int> = mutableListOf()
        val handler = object : DiffExecutorHandler {
            override fun onStayedLine(line1: Int) {
                lines.add(upToDateState.lines[line1])
            }
            override fun onInsertedLine(line2: Int) {
                val line = nextRevision.content[line2]
                lines.add(LineWithKnowledge(line, 0.0)) // initially knowledge is zero on an inserted line
                insertedLineIndices.add(lines.size - 1)
            }
        }
        DiffExecutor(handler).execute(upToDateState.lines.size, nextRevision.differenceWithPrevious)

        val stateWithInsertedLines = upToDateState.withLines(lines)
        return adder.add(stateWithInsertedLines, insertedLineIndices)
    }
}

