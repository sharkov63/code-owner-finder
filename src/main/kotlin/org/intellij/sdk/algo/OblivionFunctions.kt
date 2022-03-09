package org.intellij.sdk.algo

import org.intellij.sdk.toJavaDate
import org.intellij.sdk.toKotlinInstant
import kotlin.math.pow
import kotlin.time.Duration


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


