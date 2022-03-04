package org.intellij.sdk.action


/**
 * Represents complete history of a file.
 */
data class DiffHistory(val revisions: List<DiffRevision>)

class DiffHistoryCalculationException(
    message: String?,
) : Exception("Could not calculate diff history.\n$message")

/**
 * Calculates complete [DiffHistory] of a file.
 */
class DiffHistoryCalculator(
    private val lineWeightCalculator: LineWeightCalculator,
) {
    /**
     * Given the timeline of [revisions],
     * calculate [DiffHistory] of the file.
     *
     * @throws [DiffHistoryCalculationException]
     */
    fun calculate(revisions: List<LoadedVcsFileRevision>): DiffHistory {
        if (revisions.isEmpty()) return DiffHistory(listOf())
        val firstDifference = createFirstDifference(revisions.first())
        val firstDiffRevision = createFirstDiffRevision(revisions.first(), firstDifference)
        val diffRevisions: MutableList<DiffRevision> = mutableListOf(firstDiffRevision)
        revisions.windowed(2) { (revision1, revision2) ->
            val difference = try {
                calculateDifference(revision1, revision2)
            } catch (e: DifferenceCalculationException) {
                throw DiffHistoryCalculationException(e.message)
            }
            val newDiffRevision = applyDifferenceToRevision(diffRevisions.last(), difference, revision2)
            diffRevisions.add(newDiffRevision)
        }
        return DiffHistory(diffRevisions)
    }

    /**
     * Given old [DiffRevision], [difference]
     * and new [LoadedVcsFileRevision],
     * calculate new [DiffRevision].
     * @throws [DiffHistoryCalculationException]
     */
    private fun applyDifferenceToRevision(
        diffRevision1: DiffRevision,
        difference: Difference,
        revision2: LoadedVcsFileRevision,
    ): DiffRevision {
        val content1 = diffRevision1.content
        val content2 = revision2.lines

        val lines: MutableList<DiffLine> = mutableListOf()

        val handler = object : DiffExecutorHandler {
            override fun onStayedLine(line1: Int) {
                lines.add(content1[line1])
            }
            override fun onInsertedLine(line2: Int) {
                val line = content2[line2]
                val weight = lineWeightCalculator.calculate(line)
                val diffLine = DiffLine(revision2.author, revision2.date, weight)
                lines.add(diffLine)
            }
        }
        DiffExecutor(handler).execute(content1.size, difference)

        if (lines.size != content2.size)
            throw DiffHistoryCalculationException("Bad difference applier: the number of lines does not match!")

        return DiffRevision(revision2.revisionNumber, lines, difference)
    }

    private fun createFirstDiffRevision(revision: LoadedVcsFileRevision, difference: Difference): DiffRevision {
        val content = revision.lines.map { line ->
            val weight = lineWeightCalculator.calculate(line)
            DiffLine(revision.author, revision.date, weight)
        }
        return DiffRevision(
            revisionNumber = revision.revisionNumber,
            content = content,
            differenceWithPrevious = difference,
        )
    }

    private fun createFirstDifference(revision: LoadedVcsFileRevision): Difference = with(revision) {
        Difference(
            author = author,
            date = date,
            changes = listOf(
                DiffChange(
                    deleted = 0,
                    inserted = lineCount,
                    lineBegin1 = 0,
                    lineBegin2 = 0,
                )
            )
        )
    }
}
