package org.intellij.sdk.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages.showMessageDialog

class CodeOwnerFinderPerformer(
    private val event: AnActionEvent,
) {
    private val project = event.project

    fun errorMessage(message: String) = showMessageDialog(
        project,
        message,
        "Code Owner Finder",
        AllIcons.General.Error,
    )

    fun perform() {
    }
}