package org.intellij.sdk.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class CodeOwnerFinderException(message: String? = "") : Exception(
    "Could not find code owner: $message"
)


/**
 * Entry point of the plugin,
 * the actual action in IDEA.
 */
class CodeOwnerFinderAction : AnAction() {

    private val codeOwnerFinder: CodeOwnerFinder = MainCodeOwnerFinderImpl

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
     * @throws [CodeOwnerFinderException]
     */
    private fun findCodeOwner(event: AnActionEvent): CodeOwnerResult {
        val project = event.project
            ?: throw CodeOwnerFinderException("Invalid project.")
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: throw CodeOwnerFinderException("No file selected.")
        val extractor = HistoryExtractor(project)
        val history = try {
            extractor.extract(virtualFile)
        } catch (e: HistoryExtractionException) {
            throw CodeOwnerFinderException(e.message)
        }
        return codeOwnerFinder.find(history)
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = project != null && file != null && !file.isDirectory
    }
}
