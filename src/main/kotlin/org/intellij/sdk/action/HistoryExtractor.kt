package org.intellij.sdk.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.actions.VcsContextFactory
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.history.VcsHistoryProvider
import com.intellij.openapi.vcs.history.VcsHistorySession
import com.intellij.openapi.vfs.VirtualFile


class HistoryExtractionException(
    virtualFile: VirtualFile,
    message: String,
): Exception("Could not load history of file '${virtualFile.path}'.\n$message")

/**
 * Extracts [History] from [VirtualFile]s.
 */
class HistoryExtractor(
    project: Project
) {
    private val vcsManager: ProjectLevelVcsManager = ProjectLevelVcsManager.getInstance(project)
    private val vcsContextFactory: VcsContextFactory = VcsContextFactory.SERVICE.getInstance()

    /**
     * Extracts [History] from given [virtualFile].
     * @throws [HistoryExtractionException] if the extraction went wrong.
     */
    fun extract(virtualFile: VirtualFile): History {
        val vcsFileRevisions = getVcsFileRevisions(virtualFile)
        val revisions = try {
            vcsFileRevisions.map { it.toRevision() }
        } catch (e: RevisionCreationException) {
            throw HistoryExtractionException(virtualFile, e.message.toString())
        }

        if (revisions.isEmpty())
            throw HistoryExtractionException(
                virtualFile,
                "File has no valid revisions.",
            )
        val firstChange = with(revisions.first()) {
            RevisionChange(author, date, listOf(AtomicChange(lineCount, 0)))
        }
        val changes = try {
            listOf(firstChange) + revisions.windowed(2) { (revision1, revision2) ->
                calculateRevisionChange(revision1, revision2)
            }
        } catch (e: RevisionChangeCalculationException) {
            throw HistoryExtractionException(virtualFile, e.message.toString())
        }
        return History(changes)
    }

    private fun getVcsFileRevisions(virtualFile: VirtualFile): List<VcsFileRevision> {
        val filePath: FilePath = vcsContextFactory.createFilePathOn(virtualFile)
        val abstractVcs: AbstractVcs = vcsManager.getVcsFor(virtualFile)
            ?: throw HistoryExtractionException(
                virtualFile,
                "Could not find a valid version control system (VCS) connected to the file.",
            )
        val vcsHistoryProvider: VcsHistoryProvider = abstractVcs.vcsHistoryProvider
            ?: throw HistoryExtractionException(
                virtualFile,
                "Could not find proper VCS history provider.",
            )
        val session: VcsHistorySession = vcsHistoryProvider.createSessionFor(filePath)
            ?: throw HistoryExtractionException(
                virtualFile,
                "Could not create VCS history session for the file.",
            )
        return session.revisionList.filterNotNull().reversed() // ordered from past to present
    }
}
