package org.intellij.sdk.action

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.Date
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * A composition data class:
 * contains a [DiffLine] with the corresponding [knowledge] level,
 * with respect to a specific developer.
 *
 * See [KnowledgeState].
 */
data class LineWithKnowledge(val line: DiffLine, val knowledge: Double) {
    fun withKnowledge(newKnowledge: Double): LineWithKnowledge {
        return LineWithKnowledge(line, newKnowledge)
    }
}

/**
 * A description of how much a [developer]
 * knows the file.
 * @property developer
 * @property date the actuality of this [KnowledgeState];
 * @property lines the actual description: a list of [LineWithKnowledge].
 */
data class KnowledgeState(
    val developer: String,
    val date: Date,
    val lines: List<LineWithKnowledge>,
) {
    companion object {
        fun initialState(developer: String) = KnowledgeState(
            developer = developer,
            date = Instant.DISTANT_PAST.toJavaDate(),
            lines = listOf(),
        )
    }

    val totalLineWeight: Int
        get() = lines.sumOf { (line, _) -> line.weight }

    val totalKnowledge: Double
        get() = lines.sumOf { (line, knowledge) -> line.weight * knowledge }

    val totalKnowledgeLevel: Double
        get() = totalKnowledge / totalLineWeight.toDouble()

    fun withLines(newLines: List<LineWithKnowledge>) = KnowledgeState(
        developer = developer,
        date = date,
        lines = newLines,
    )
}

/**
 * An algorithm which describes
 * how fast developers forget their code
 * over large periods of time.
 */
interface OblivionFunction {
    /**
     * The actual function.
     * Calculates the new knowledge level,
     * if at the time of [timePassed] time units ago
     * the knowledge level was equal to [knowledge].
     */
    fun calculateNewKnowledgeLevel(knowledge: Double, timePassed: Duration): Double

    /**
     * Update a given [state] to a given moment of time.
     */
    fun applyToKnowledgeState(state: KnowledgeState, timePassed: Duration) = KnowledgeState (
        developer = state.developer,
        date = (state.date.toKotlinInstant() + timePassed).toJavaDate(),
        lines = state.lines.map { line ->
            val newKnowledge = calculateNewKnowledgeLevel(
                knowledge = line.knowledge,
                timePassed = timePassed,
            )
            line.withKnowledge(newKnowledge)
        }
    )
}

@Suppress("unused")
object ConstantOblivionFunction : OblivionFunction {
    override fun calculateNewKnowledgeLevel(knowledge: Double, timePassed: Duration): Double {
        return knowledge
    }
}

/**
 * Main implementation of [OblivionFunction]:
 * a decreasing exponential function.
 */
object OblivionFunctionImpl : OblivionFunction {
    /**
     * During this period, a developer forgets half of the code he currently remembers.
     */
    private const val HALF_LIFE_IN_DAYS = 500
    private val halfLife: Duration = with(Duration) { HALF_LIFE_IN_DAYS.days }

    override fun calculateNewKnowledgeLevel(knowledge: Double, timePassed: Duration): Double {
        val exp = (timePassed / halfLife).toFloat()
        return knowledge * (0.5f).pow(exp).toDouble()
    }
}


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
