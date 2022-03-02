package org.intellij.sdk.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * An object which can show results of [CodeOwnerFinderAction] to the user.
 */
interface CodeOwnerFinderRenderer {
    fun success(result: CodeOwnerResult)
    fun error(exception: CodeOwnerFinderException)
}

/**
 * Simple dialog box implementation of [CodeOwnerFinderRenderer].
 */
class CodeOwnerFinderDialogRenderer(
    private val project: Project?
) : CodeOwnerFinderRenderer {

    companion object {
        const val DIALOG_TITLE = "Code Owner Finder"
    }

    override fun success(result: CodeOwnerResult) = Messages.showMessageDialog(
        project,
        result._temp,
        DIALOG_TITLE,
        AllIcons.General.SuccessDialog,
    )

    override fun error(exception: CodeOwnerFinderException) = Messages.showErrorDialog(
        project,
        exception.message,
        DIALOG_TITLE,
    )
}