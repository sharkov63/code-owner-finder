package org.intellij.sdk.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.actions.VcsContextFactory
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.history.VcsHistoryProvider
import com.intellij.openapi.vcs.history.VcsHistorySession
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.*


/**
 * Represents a specific revision of a file.
 *
 * The difference between [VcsFileRevision]
 * is that [author] and [date] are not null
 * and [content] is already loaded into memory.
 */
data class LoadedVcsFileRevision(
    val revisionNumber: VcsRevisionNumber,
    val author: String,
    val date: Date,
    val content: String,
) {
    val lines: List<String> by lazy {
        // It's important here to use the same line split algorithm
        // as in com.intellij.util.diff.Diff.buildChanges()
        // to ensure that the lines always match.
        com.intellij.util.diff.Diff.splitLines(content).toList()
    }

    val lineCount: Int = lines.size
}


class RevisionListLoadingException(
    virtualFile: VirtualFile,
    message: String = "",
): Exception("Could not load revisions of file '${virtualFile.path}'.\n$message")

class RevisionLoadingException(
    revision: VcsFileRevision,
    message: String = "",
): Exception("Unable to load revision ${revision.revisionNumber}.\n$message")

/**
 * Loads revisions of [VirtualFile]s.
 */
class RevisionLoader(
    project: Project
) {
    private val vcsManager: ProjectLevelVcsManager = ProjectLevelVcsManager.getInstance(project)
    private val vcsContextFactory: VcsContextFactory = VcsContextFactory.SERVICE.getInstance()

    /**
     * Load all revisions of given [virtualFile].
     * @return the non-empty list of all [LoadedVcsFileRevision]s of [virtualFile] in order from past to present.
     * @throws [RevisionListLoadingException] if the loading went wrong.
     */
    fun loadAll(virtualFile: VirtualFile): List<LoadedVcsFileRevision> {
        val vcsFileRevisions = getVcsFileRevisions(virtualFile)
        if (vcsFileRevisions.isEmpty()) {
            throw RevisionListLoadingException(
                virtualFile,
                "File has no valid revisions.",
            )
        }
        return try {
            vcsFileRevisions.map(::loadRevision)
        } catch (e: RevisionLoadingException) {
            throw RevisionListLoadingException(virtualFile, e.message.toString())
        }
    }

    /**
     * Converts [VcsFileRevision] to [LoadedVcsFileRevision]
     * by loading all data.
     * @throws [RevisionLoadingException] on loading fail.
     */
    private fun loadRevision(revision: VcsFileRevision): LoadedVcsFileRevision {
        val author = revision.author ?: throw RevisionLoadingException(
            revision,
            "Revision has invalid author.",
        )
        val date = revision.revisionDate ?: throw RevisionLoadingException(
            revision,
            "Revision has invalid revision date.",
        )

        val bytes = try {
            revision.loadContent() ?: throw RevisionLoadingException(revision)
        } catch (e: IOException) {
            throw RevisionLoadingException(revision, e.message.toString())
        } catch (e: VcsException) {
            throw RevisionLoadingException(revision, e.message)
        }

        val charset = revision.defaultCharset
            ?: CharsetToolkit.guessFromBOM(bytes)
            ?: CharsetToolkit.getPlatformCharset()
        val content = CharsetToolkit.bytesToString(bytes, charset)
        return LoadedVcsFileRevision(revision.revisionNumber, author, date, content)
    }

    /**
     * Finds all [VcsFileRevision]s of given [virtualFile].
     * @throws [RevisionListLoadingException]
     */
    private fun getVcsFileRevisions(virtualFile: VirtualFile): List<VcsFileRevision> {
        val filePath: FilePath = vcsContextFactory.createFilePathOn(virtualFile)
        val abstractVcs: AbstractVcs = vcsManager.getVcsFor(virtualFile)
            ?: throw RevisionListLoadingException(
                virtualFile,
                "Could not find a valid version control system (VCS) connected to the file.",
            )
        val vcsHistoryProvider: VcsHistoryProvider = abstractVcs.vcsHistoryProvider
            ?: throw RevisionListLoadingException(
                virtualFile,
                "Could not find proper VCS history provider.",
            )
        val session: VcsHistorySession = runBlocking(Dispatchers.IO) {
            vcsHistoryProvider.createSessionFor(filePath)
        } ?: throw RevisionListLoadingException(
            virtualFile,
            "Could not create VCS history session for the file.",
        )

        return session.revisionList.filterNotNull().reversed() // ordered from past to present
    }
}
