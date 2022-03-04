package org.intellij.sdk.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * The actual action in IDEA.
 */
class CodeOwnerFinderAction : AnAction() {

    private val diffHistoryCalculator = DiffHistoryCalculator(WordLineWeightCalculator)

    /**
     * Selected [CodeOwnerFinder] algorithm.
     */
    private val codeOwnerFinder: CodeOwnerFinder = KnowledgeStateCodeOwnerFinder()

    /**
     * Entry point of the plugin.
     * This function is called every time user presses "Find Code Owner...".
     */
    override fun actionPerformed(event: AnActionEvent) {
        val renderer = CodeOwnerFinderDialogRenderer(event.project)
        val result = try {
            findCodeOwner(event)
        } catch (e: CodeOwnerFinderException) {
            return renderer.error(e)
        } catch (e: Throwable) {
            return renderer.error(CodeOwnerFinderException(e.message))
        }
        renderer.success(result)
    }

    /**
     * Main logic function.
     * @throws [CodeOwnerFinderException]
     */
    private fun findCodeOwner(event: AnActionEvent): CodeOwnerResult {
        val project = event.project
            ?: throw CodeOwnerFinderException("Invalid project.")
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: throw CodeOwnerFinderException("No file selected.")

        val revisionLoader = RevisionLoader(project)
        val revisions = try {
            revisionLoader.loadAll(virtualFile)
        } catch (e: RevisionListLoadingException) {
            throw CodeOwnerFinderException(e.message)
        }

        val diffHistory = diffHistoryCalculator.calculate(revisions)

        return codeOwnerFinder.find(diffHistory)
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = project != null && file != null && !file.isDirectory
    }
}
