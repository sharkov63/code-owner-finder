package org.intellij.sdk.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CodeOwnerCalculator(
    private val project: Project,
    private val file: VirtualFile,
) {
    fun calculate(): CodeOwnerResult {
        return CodeOwnerResult()
    }
}