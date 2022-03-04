package org.intellij.sdk.action

import com.intellij.openapi.diagnostic.Logger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.Date
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

class MainKnowledgeStateCalculatorImpl(
    oblivionFunction: OblivionFunction = MainOblivionFunctionImpl,
) : OblivionKnowledgeStateCalculator(oblivionFunction) {

    override fun instantNextKnowledgeState(
        author: String,
        upToDateState: KnowledgeState,
        nextRevision: DiffRevision,
    ): KnowledgeState {
        val lines: MutableList<LineWithKnowledge> = mutableListOf()
        val handler = object : DiffExecutorHandler {
            override fun onStayedLine(line1: Int) {
                lines.add(upToDateState.lines[line1])
            }
            override fun onInsertedLine(line2: Int) {
                val line = nextRevision.content[line2]
                val knowledge = if (nextRevision.author == author) {
                    1.0
                } else {
                    0.0
                }
                lines.add(LineWithKnowledge(line, knowledge))
            }
        }
        DiffExecutor(handler).execute(upToDateState.lines.size, nextRevision.differenceWithPrevious)
//        log.info("calculating knowledge for $author")
//        log.info("commit: ${nextRevision.author} ${nextRevision.date} ${nextRevision.revisionNumber}")
//        val msg = lines.joinToString("\n") { line ->
//            "${line.line.weight}-${line.line.author}-${line.line.date}  ${line.knowledge}"
//        }
//        log.info(msg)
        return KnowledgeState(nextRevision.date, lines)
    }
}

class KnowledgeStateCodeOwnerFinder(
    private val knowledgeStateCalculator: KnowledgeStateCalculator = MainKnowledgeStateCalculatorImpl(),
) : AuthorIndependentCodeOwnerFinder() {

    override fun calculateKnowledgeLevelOf(author: String, history: DiffHistory): Double {
        val knowledgeState = history.revisions.foldIndexed(KnowledgeState.INITIAL) { index, knowledgeState, diffRevision ->
            knowledgeStateCalculator.nextKnowledgeState(author, knowledgeState, diffRevision)
        }
        val upToDateState = knowledgeStateCalculator.updateKnowledgeStateToPresent(author, knowledgeState)
        return upToDateState.totalKnowledgeLevel
    }
}
