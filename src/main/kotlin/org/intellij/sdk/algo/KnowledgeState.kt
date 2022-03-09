package org.intellij.sdk.algo

import kotlinx.datetime.Instant
import org.intellij.sdk.diff.DiffLine
import org.intellij.sdk.toJavaDate
import java.util.*


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

