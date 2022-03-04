package org.intellij.sdk.action

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.Date
import kotlin.math.min
import kotlin.math.pow
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
 * A description of how much a specific developer
 * knows all lines in the file.
 * @property date the actuality of this [KnowledgeState];
 * @property lines the actual description: a list of [LineWithKnowledge].
 */
data class KnowledgeState(
    val date: Date,
    val lines: List<LineWithKnowledge>,
) {
    companion object {
        val INITIAL = KnowledgeState(Instant.DISTANT_PAST.toJavaDate(), listOf())
    }

    val totalLineWeight: Int
        get() = lines.sumOf { (line, _) -> line.weight }

    val totalKnowledge: Double
        get() = lines.sumOf { (line, knowledge) -> line.weight * knowledge }

    val totalKnowledgeLevel: Double
        get() = totalKnowledge / totalLineWeight.toDouble()
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
    fun applyToKnowledgeState(state: KnowledgeState, timePassed: Duration): KnowledgeState {
        return KnowledgeState(
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
}

/**
 * Main implementation of [OblivionFunction]:
 * a decreasing exponential function.
 */
object MainOblivionFunctionImpl : OblivionFunction {
    /**
     * During this period, a developer forgets half of the code he currently remembers.
     */
    private val halfLife: Duration = with(Duration) { 200.days }

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
    fun nextKnowledgeState(
        developer: String,
        state: KnowledgeState,
        nextRevision: DiffRevision,
    ): KnowledgeState

    fun updateKnowledgeStateToPresent(
        developer: String,
        state: KnowledgeState,
    ): KnowledgeState
}

/**
 * A [KnowledgeStateCalculator], which is based on a given [oblivionFunction].
 */
abstract class OblivionKnowledgeStateCalculator(
    private val oblivionFunction: OblivionFunction = MainOblivionFunctionImpl,
) : KnowledgeStateCalculator {

    override fun nextKnowledgeState(
        developer: String,
        state: KnowledgeState,
        nextRevision: DiffRevision,
    ): KnowledgeState {
        val instant1 = state.date.toKotlinInstant()
        val instant2 = nextRevision.date.toKotlinInstant()
        val timePassed = instant2 - instant1
        val upToDateState = oblivionFunction.applyToKnowledgeState(state, timePassed)
        return instantNextKnowledgeState(developer, upToDateState, nextRevision)
    }

    override fun updateKnowledgeStateToPresent(
        developer: String,
        state: KnowledgeState,
    ): KnowledgeState {
        val timePassed = Clock.System.now() - state.date.toKotlinInstant()
        return oblivionFunction.applyToKnowledgeState(state, timePassed)
    }

    /**
     * The only abstract method:
     * we need to define how [KnowledgeState]
     * changes when a commit happens.
     */
    abstract fun instantNextKnowledgeState(
        developer: String,
        upToDateState: KnowledgeState,
        nextRevision: DiffRevision,
    ): KnowledgeState
}

/**
 * An algorithm, which describes
 * how knowledge is added to the developer's [KnowledgeState],
 * when he makes a change to the file.
 *
 * It consists of taking the list of inserted lines,
 * and adding some knowledge to some lines around each inserted line.
 */
class AuthorKnowledgeAdder(
    private val lines: MutableList<LineWithKnowledge>,
) {
    companion object {
        const val SPREAD_COEFFICIENT = 6 // to one direction
        const val READING_KNOWLEDGE_COEFFICIENT = 0.5
    }

    /**
     * Add knowledge for each of [insertedLineIndices].
     */
    fun addKnowledge(insertedLineIndices: List<Int>) {
        insertedLineIndices.forEach { lineIndex ->
            addKnowledge(lineIndex)
        }
    }

    /**
     * Add knowledge around given [lineIndex].
     */
    private fun addKnowledge(lineIndex: Int) {
        // knowledge by writing --- added to this exact line
        lines[lineIndex] = lines[lineIndex].withKnowledge(1.0)

        // knowledge by reading --- add to lines around it
        val w = lines[lineIndex].line.weight
        if (w <= 0) {
            return // nothing to add
        }
        val spreadWeight = w * SPREAD_COEFFICIENT
        for (direction in listOf(+1, -1)) {
            spreadReadingKnowledge(lineIndex + direction, direction, spreadWeight)
        }
    }

    private fun inBounds(index: Int) = index in lines.indices

    /**
     * Spreads knowledge in a given [direction] from [startingIndex].
     */
    private fun spreadReadingKnowledge(
        startingIndex: Int,
        direction: Int, // +1 or -1
        spreadWeight: Int,
    ) {
        var index = startingIndex
        var remainingWeight = spreadWeight
        while (inBounds(index) && remainingWeight > 0) {
            spreadReadingKnowledgeAt(index, spreadWeight, remainingWeight)
            remainingWeight -= lines[index].line.weight
            index += direction
        }
    }

    private fun sum(r: Int): Int = r * (r + 1) / 2

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
        val scaledAbsoluteDelta = absoluteDelta * READING_KNOWLEDGE_COEFFICIENT
        val relativeDelta = scaledAbsoluteDelta / weight
        val newKnowledge = min(1.0, lines[index].knowledge + relativeDelta)
        lines[index] = lines[index].withKnowledge(newKnowledge)
    }
}

/**
 * Main implementation of [KnowledgeStateCalculator].
 *
 * Works as an [OblivionKnowledgeStateCalculator] with given [oblivionFunction].
 * A commit change is handled this way:
 * * if a commit is done by the same developer, add knowledge via [AuthorKnowledgeAdder];
 * * if a commit is done by another developer, knowledge of untouched lines does not change,
 * and knowledge of added lines is zero.
 */
class MainKnowledgeStateCalculatorImpl(
    oblivionFunction: OblivionFunction = MainOblivionFunctionImpl,
) : OblivionKnowledgeStateCalculator(oblivionFunction) {

    override fun instantNextKnowledgeState(
        developer: String,
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
                lines.add(LineWithKnowledge(line, 0.0))
                insertedLineIndices.add(lines.size - 1)
            }
        }
        DiffExecutor(handler).execute(upToDateState.lines.size, nextRevision.differenceWithPrevious)
        if (nextRevision.author == developer) {
            val adder = AuthorKnowledgeAdder(lines)
            adder.addKnowledge(insertedLineIndices)
        }
        return KnowledgeState(nextRevision.date, lines)
    }
}


/**
 * An implementation of [CodeOwnerFinder],
 * which calculates the knowledge level
 * of each developer independently,
 * by given [knowledgeStateCalculator].
 */
class KnowledgeStateCodeOwnerFinder(
    private val knowledgeStateCalculator: KnowledgeStateCalculator = MainKnowledgeStateCalculatorImpl(),
) : DeveloperIndependentCodeOwnerFinder() {

    override fun calculateKnowledgeLevelOf(developer: String, history: DiffHistory): Double {
        val knowledgeState = history.revisions.fold(KnowledgeState.INITIAL) { knowledgeState, diffRevision ->
            knowledgeStateCalculator.nextKnowledgeState(developer, knowledgeState, diffRevision)
        }
        val upToDateState = knowledgeStateCalculator.updateKnowledgeStateToPresent(developer, knowledgeState)
        return upToDateState.totalKnowledgeLevel
    }
}
