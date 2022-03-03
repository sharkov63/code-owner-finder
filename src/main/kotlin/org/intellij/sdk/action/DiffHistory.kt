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
     * @throws [DiffHistoryCalculationException].
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
        var i = 0
        for (change in difference.changes) {
            if (i < change.lineBegin1) {
                val remaining = content1.subList(i, change.lineBegin1)
                lines.addAll(remaining)
            }
            i = change.lineBegin1 + change.deleted

            if (change.inserted > 0) {
                val inserted = content2.subList(change.lineBegin2, change.lineBegin2 + change.inserted).map { line ->
                    val weight = lineWeightCalculator.calculate(line)
                    DiffLine(revision2.author, revision2.date, weight)
                }
                lines.addAll(inserted)
            }
        }
        lines.addAll(content1.subList(i, content1.size))
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
                    lineBegin1 = 1,
                    lineBegin2 = 1,
                )
            )
        )
    }
}
