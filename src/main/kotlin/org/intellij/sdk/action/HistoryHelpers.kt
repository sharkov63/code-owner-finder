package org.intellij.sdk.action

import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.diff.Diff
import com.intellij.util.diff.FilesTooBigForDiffException
import java.io.IOException
import java.util.*


/**
 * Represents a specific version of a file.
 */
data class Revision(
    val revisionNumber: VcsRevisionNumber,
    val author: String,
    val date: Date,
    val content: String,
) {
    val lineCount: Int
        get() = content.trim().split("\n").size
}

class RevisionCreationException(
    message: String,
): Exception(message)


/**
 * Converts [VcsFileRevision] to [Revision].
 * @throws [RevisionCreationException] on conversion fail.
 */
fun VcsFileRevision.toRevision(): Revision {
    val author = author ?: throw RevisionCreationException(
        "Revision '${revisionNumber.asString()}' of the file has invalid author."
    )
    val date = revisionDate ?: throw RevisionCreationException(
        "Revision '${revisionNumber.asString()}' of the file has invalid revision date."
    )

    val notLoadedException = RevisionCreationException(
        "Could not load content for revision ${revisionNumber.asString()}"
    )
    val bytes = try {
        loadContent() ?: throw notLoadedException
    } catch (e: IOException) {
        throw notLoadedException
    } catch (e: VcsException) {
        throw notLoadedException
    }

    val charset = defaultCharset
        ?: CharsetToolkit.guessFromBOM(bytes)
        ?: CharsetToolkit.getPlatformCharset()
    val content = CharsetToolkit.bytesToString(bytes, charset)
    return Revision(revisionNumber, author, date, content)
}

/**
 * Represents a single change in diff file.
 * Currently, we are only interested in the number of [inserted] and [deleted] lines.
 */
data class AtomicChange(val inserted: Int, val deleted: Int)


/**
 * Converts [Diff.Change] to [AtomicChange].
 */
private fun Diff.Change.toAtomicChange(): AtomicChange {
    return AtomicChange(inserted, deleted)
}


/**
 * Represents a change between two revisions.
 * The change is done by [author],
 * at specific [date],
 * and consists of multiple [AtomicChange]s.
 */
data class RevisionChange(val author: String, val date: Date, val changes: List<AtomicChange>)


class RevisionChangeCalculationException(
    revision1: Revision,
    revision2: Revision,
    message: String = "",
): Exception("Could not calculate difference between revisions ${revision1.revisionNumber} and ${revision2.revisionNumber}:\n$message")

/**
 * Calculates [RevisionChange] between two revisions.
 */
fun calculateRevisionChange(revision1: Revision, revision2: Revision): RevisionChange {
    val headChange: Diff.Change = try {
        Diff.buildChanges(revision1.content, revision2.content)
            ?: throw RevisionChangeCalculationException(revision1, revision2)
    } catch (e: FilesTooBigForDiffException) {
        throw RevisionChangeCalculationException(
            revision1,
            revision2,
            "Revision content is too large!",
        )
    }

    val changeList: MutableList<AtomicChange> = mutableListOf(headChange.toAtomicChange())
    var currentChange: Diff.Change? = headChange.link
    while (currentChange != null) {
        changeList.add(currentChange.toAtomicChange())
        currentChange = currentChange.link
    }

    return RevisionChange(revision2.author, revision2.date, changeList)
}

/**
 * Represents total history of a file:
 * a sequence of [RevisionChange]s.
 */
data class History(val changes: List<RevisionChange>)