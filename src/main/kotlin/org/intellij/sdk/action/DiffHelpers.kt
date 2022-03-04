package org.intellij.sdk.action

import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.util.diff.Diff
import com.intellij.util.diff.FilesTooBigForDiffException
import java.util.*

/**
 * Represents a single change in [Difference],
 * which is deleting [deleted] lines and inserting [inserted] lines.
 * Pretty much the same as [com.intellij.util.diff.Diff.Change],
 * except here we do not connect changes in a linked list.
 *
 * Lines are numbered from 0 in both files.
 * @property [lineBegin1] is the number of the first line of old file which was deleted.
 * If no lines were deleted, it is equal to the number of the line of old file,
 * before which the insertion was done.
 * @property [lineBegin2] is the number of the line of new file,
 * before which the insertion was done.
 * If no lines were inserted, it is the number of the line of new file,
 * which is the first after deleted lines.
 */
data class DiffChange(
    val deleted: Int,
    val inserted: Int,
    val lineBegin1: Int,
    val lineBegin2: Int,
)

fun Diff.Change.toCodeOwnerFinderDiffChange(): DiffChange = DiffChange(
    deleted = deleted,
    inserted = inserted,
    lineBegin1 = line0,
    lineBegin2 = line1,
)


/**
 * Represents a difference between two [DiffRevision]s.
 * In other words, it's a commit by [author],
 * done on a certain [date], and which
 * consists of [changes].
 */
data class Difference(val author: String, val date: Date, val changes: List<DiffChange>)




class DifferenceCalculationException(
    revision1: LoadedVcsFileRevision,
    revision2: LoadedVcsFileRevision,
    message: String = "",
): Exception(
    "Could not calculate difference between revisions ${revision1.revisionNumber} and ${revision2.revisionNumber}:\n$message"
)


/**
 * Given two revisions, calculates their [Difference].
 *
 * @throws [DifferenceCalculationException]
 */
fun calculateDifference(revision1: LoadedVcsFileRevision, revision2: LoadedVcsFileRevision): Difference {
    val headChange: Diff.Change = try {
        Diff.buildChanges(revision1.content, revision2.content)
            ?: // no changes between revisions
            return Difference(revision2.author, revision2.date, listOf())
    } catch (e: FilesTooBigForDiffException) {
        throw DifferenceCalculationException(
            revision1,
            revision2,
            "Revision content is too large!",
        )
    }
    val changeList = headChange.toList().map { it.toCodeOwnerFinderDiffChange() }
    return Difference(revision2.author, revision2.date, changeList)
}

/**
 * Represents a single line (row) in a specific [DiffRevision] of a file.
 * We are only interested in:
 * * the [author] of the line;
 * * the [date] when the line was created;
 * * and it's [weight] - a quantity which tells us how much
 * "useful information" this line holds.
 *
 * A [weight] can be defined in many ways,
 * see [LineWeightCalculator].
 */
data class DiffLine(val author: String, val date: Date, val weight: Int)

/**
 * Represents a specific file revision,
 * storing [content] in a convenient format:
 * each line is represented by an instance of [DiffLine].
 */
data class DiffRevision(
    val revisionNumber: VcsRevisionNumber,
    val content: List<DiffLine>,
    val differenceWithPrevious: Difference,
) {
    val author: String
        get() = differenceWithPrevious.author
    val date: Date
        get() = differenceWithPrevious.date
    val changes: List<DiffChange>
        get() = differenceWithPrevious.changes
}
