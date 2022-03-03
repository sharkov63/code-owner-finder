package org.intellij.sdk.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date

internal class CalculateDifferenceTests {

    private val revisionNumber = DummyVcsRevisionNumber
    private val author = "testAuthor"
    private val date = Date.from(Instant.now())

    private fun wrapContentToRevision(content: String) = LoadedVcsFileRevision(
        revisionNumber = revisionNumber,
        author = author,
        date = date,
        content = content,
    )

    private fun getDifference(content1: String, content2: String) = calculateDifference(
        wrapContentToRevision(content1),
        wrapContentToRevision(content2),
    )

    private fun singleTest(
        content1: String,
        content2: String,
        expectedChanges: List<DiffChange>,
    ) {
        val difference = getDifference(content1, content2)
        val changes = difference.changes
        assertEquals(expectedChanges, changes)
    }

    @Test
    fun `calculateDifference() on prefix insertion`() {
        val content1 = listOf(            "a", "b", "c").joinToString("\n")
        val content2 = listOf("a1", "a2", "a", "b", "c").joinToString("\n")
        val expectedChanges = listOf(
            DiffChange(deleted = 0, inserted = 2, lineBegin1 = 0, lineBegin2 = 0),
        )
        singleTest(content1, content2, expectedChanges)
    }

    @Test
    fun `calculateDifference() on several insertions`() {
        val content1 = listOf("a",                   "b",       "c"            ).joinToString("\n")
        val content2 = listOf("a", "a1", "a2", "a3", "b", "b1", "c", "c2", "c3").joinToString("\n")
        val expectedChanges = listOf(
            DiffChange(deleted = 0, inserted = 3, lineBegin1 = 1, lineBegin2 = 1),
            DiffChange(deleted = 0, inserted = 1, lineBegin1 = 2, lineBegin2 = 5),
            DiffChange(deleted = 0, inserted = 2, lineBegin1 = 3, lineBegin2 = 7),
        )
        singleTest(content1, content2, expectedChanges)
    }

    @Test
    fun `calculateDifference() on prefix deletion`() {
        val content1 = listOf("a", "b", "c", "d").joinToString("\n")
        val content2 = listOf(          "c", "d").joinToString("\n")
        val expectedChanges = listOf(
            DiffChange(deleted = 2, inserted = 0, lineBegin1 = 0, lineBegin2 = 0),
        )
        singleTest(content1, content2, expectedChanges)
    }

    @Test
    fun `calculateDifference() on several deletions`() {
        val content1 = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i").joinToString("\n")
        val content2 = listOf("a",      "c", "d",           "g"          ).joinToString("\n")
        val expectedChanges = listOf(
            DiffChange(deleted = 1, inserted = 0, lineBegin1 = 1, lineBegin2 = 1),
            DiffChange(deleted = 2, inserted = 0, lineBegin1 = 4, lineBegin2 = 3),
            DiffChange(deleted = 2, inserted = 0, lineBegin1 = 7, lineBegin2 = 4),
        )
        singleTest(content1, content2, expectedChanges)
    }

    @Test
    fun `calculateDifference() on mixed changes`() {
        val content1 = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i").joinToString("\n")
        val content2 = listOf("a", "b1", "c", "d", "g1", "g2", "h", "i", "i1").joinToString("\n")
        val expectedChanges = listOf(
            DiffChange(deleted = 1, inserted = 1, lineBegin1 = 1, lineBegin2 = 1),
            DiffChange(deleted = 3, inserted = 2, lineBegin1 = 4, lineBegin2 = 4),
            DiffChange(deleted = 0, inserted = 1, lineBegin1 = 9, lineBegin2 = 8),
        )
        singleTest(content1, content2, expectedChanges)
    }
}