package org.intellij.sdk.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class CodeOwnerFinderDialogRenderer(
    private val project: Project?
) {
    companion object {
        const val DIALOG_TITLE = "Code Owner Finder"
    }

    fun error(message: String) = Messages.showErrorDialog(
        project,
        message,
        DIALOG_TITLE,
    )

    fun success(result: CodeOwnerResult) {
        TODO()
    }
}