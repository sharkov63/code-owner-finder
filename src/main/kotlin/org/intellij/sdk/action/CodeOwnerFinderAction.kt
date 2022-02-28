package org.intellij.sdk.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys


class CodeOwnerFinderAction: AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        TODO("Write your code here")
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = project != null && file != null && !file.isDirectory
    }
}
