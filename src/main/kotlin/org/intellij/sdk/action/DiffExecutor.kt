package org.intellij.sdk.action

class DiffExecutor(
    private val handler: DiffExecutorHandler,
) {
    fun execute(size1: Int, difference: Difference) {
        var i = 0
        for (change in difference.changes) {
            while (i < change.lineBegin1) {
                handler.onStayedLine(i++)
            }
            while (i < change.lineBegin1 + change.deleted) {
                handler.onDeletedLine(i++)
            }
            for (j in change.lineBegin2 until change.lineBegin2 + change.inserted) {
                handler.onInsertedLine(j)
            }
        }
        while (i < size1) {
            handler.onStayedLine(i++)
        }
    }
}

interface DiffExecutorHandler {
    fun onStayedLine(line1: Int): Unit = Unit
    fun onDeletedLine(line1: Int): Unit = Unit
    fun onInsertedLine(line2: Int): Unit = Unit
}