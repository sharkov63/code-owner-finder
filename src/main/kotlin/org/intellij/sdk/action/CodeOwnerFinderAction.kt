package org.intellij.sdk.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CodeOwnerFinderAction: AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val renderer = CodeOwnerFinderDialogRenderer(event.project)
        val project: Project = event.project
            ?: return renderer.error("### null project")
        val file: VirtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: return renderer.error("### no file")
        val calculator = CodeOwnerCalculator(project, file)
        val result = calculator.calculate()
        renderer.success(result)
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = project != null && file != null && !file.isDirectory
    }
}
