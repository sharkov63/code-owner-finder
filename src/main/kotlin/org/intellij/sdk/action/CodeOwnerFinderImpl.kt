package org.intellij.sdk.action

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.Date
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration

data class LineWithKnowledge(val line: DiffLine, val knowledge: Double) {
    fun withKnowledge(newKnowledge: Double): LineWithKnowledge {
        return LineWithKnowledge(line, newKnowledge)
    }
}

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

interface OblivionFunction {
    fun calculateNewKnowledgeLevel(knowledge: Double, timePassed: Duration): Double

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

object MainOblivionFunctionImpl : OblivionFunction {
    private val halfLife: Duration = with(Duration) { 200.days }

    override fun calculateNewKnowledgeLevel(knowledge: Double, timePassed: Duration): Double {
        val exp = (timePassed / halfLife).toFloat()
        return knowledge * (0.5f).pow(exp).toDouble()
    }
}

interface KnowledgeStateCalculator {
    fun nextKnowledgeState(
        author: String,
        state: KnowledgeState,
        nextRevision: DiffRevision,
    ): KnowledgeState

    fun updateKnowledgeStateToPresent(
        author: String,
        state: KnowledgeState,
    ): KnowledgeState
}

abstract class OblivionKnowledgeStateCalculator(
    private val oblivionFunction: OblivionFunction = MainOblivionFunctionImpl,
) : KnowledgeStateCalculator {

    override fun nextKnowledgeState(
        author: String,
        state: KnowledgeState,
        nextRevision: DiffRevision,
    ): KnowledgeState {
        val instant1 = state.date.toKotlinInstant()
        val instant2 = nextRevision.date.toKotlinInstant()
        val timePassed = instant2 - instant1
        val upToDateState = oblivionFunction.applyToKnowledgeState(state, timePassed)
        return instantNextKnowledgeState(author, upToDateState, nextRevision)
    }

    override fun updateKnowledgeStateToPresent(
        author: String,
        state: KnowledgeState,
    ): KnowledgeState {
        val timePassed = Clock.System.now() - state.date.toKotlinInstant()
        return oblivionFunction.applyToKnowledgeState(state, timePassed)
    }

    abstract fun instantNextKnowledgeState(
        author: String,
        upToDateState: KnowledgeState,
        nextRevision: DiffRevision,
    ): KnowledgeState
}

class AuthorKnowledgeAdder(
    private val lines: MutableList<LineWithKnowledge>,
) {
    companion object {
        const val SPREAD_COEFFICIENT = 6 // to one direction
        const val READING_KNOWLEDGE_COEFFICIENT = 0.5
    }

    fun addKnowledge(insertedLineIndices: List<Int>) {
        insertedLineIndices.forEach { lineIndex ->
            addKnowledge(lineIndex)
        }
    }

    private fun addKnowledge(lineIndex: Int) {
        // knowledge by writing
        lines[lineIndex] = lines[lineIndex].withKnowledge(1.0)

        // knowledge by reading
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

class MainKnowledgeStateCalculatorImpl(
    oblivionFunction: OblivionFunction = MainOblivionFunctionImpl,
) : OblivionKnowledgeStateCalculator(oblivionFunction) {

    override fun instantNextKnowledgeState(
        author: String,
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
        if (nextRevision.author == author) {
            val adder = AuthorKnowledgeAdder(lines)
            adder.addKnowledge(insertedLineIndices)
        }
        return KnowledgeState(nextRevision.date, lines)
    }
}

class KnowledgeStateCodeOwnerFinder(
    private val knowledgeStateCalculator: KnowledgeStateCalculator = MainKnowledgeStateCalculatorImpl(),
) : AuthorIndependentCodeOwnerFinder() {

    override fun calculateKnowledgeLevelOf(author: String, history: DiffHistory): Double {
        val knowledgeState = history.revisions.fold(KnowledgeState.INITIAL) { knowledgeState, diffRevision ->
            knowledgeStateCalculator.nextKnowledgeState(author, knowledgeState, diffRevision)
        }
        val upToDateState = knowledgeStateCalculator.updateKnowledgeStateToPresent(author, knowledgeState)
        return upToDateState.totalKnowledgeLevel
    }
}
