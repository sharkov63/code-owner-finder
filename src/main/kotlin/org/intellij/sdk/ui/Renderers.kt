package org.intellij.sdk.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.intellij.sdk.CodeOwnerFinderException
import org.intellij.sdk.CodeOwnerResult
import org.intellij.sdk.action.CodeOwnerFinderAction
import kotlin.math.roundToInt

/**
 * An object which can show results of [CodeOwnerFinderAction] to the user.
 */
interface CodeOwnerFinderRenderer {
    fun success(result: CodeOwnerResult)
    fun error(exception: CodeOwnerFinderException)
}

/**
 * Selects [maxDevelopers] best candidates,
 * and renders them to string.
 */
private fun renderResultToTopNLines(result: CodeOwnerResult, maxDevelopers: Int): String {
    return result
        .developerToKnowledgeLevel
        .toList()
        .sortedByDescending { it.second }
        .take(maxDevelopers)
        .joinToString("\n") { (developer, knowledgeLevel) ->
            val percentage = (knowledgeLevel * 100).roundToInt()
            "$developer: knows $percentage%"
        }
}

/**
 * Simple dialog box implementation of [CodeOwnerFinderRenderer].
 */
class CodeOwnerFinderDialogRenderer(
    private val project: Project?
) : CodeOwnerFinderRenderer {

    companion object {
        const val DIALOG_TITLE = "Code Owner Finder"
        const val MAX_DEVELOPERS_IN_RESULT = 5
    }

    override fun success(result: CodeOwnerResult) = Messages.showMessageDialog(
        project,
        renderResultToTopNLines(result, MAX_DEVELOPERS_IN_RESULT),
        DIALOG_TITLE,
        AllIcons.General.SuccessDialog,
    )

    override fun error(exception: CodeOwnerFinderException) = Messages.showErrorDialog(
        project,
        exception.message,
        DIALOG_TITLE,
    )
}