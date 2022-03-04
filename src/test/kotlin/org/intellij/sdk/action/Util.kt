package org.intellij.sdk.action

import com.intellij.openapi.vcs.history.VcsRevisionNumber
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.*

object DummyVcsRevisionNumber : VcsRevisionNumber {
    override fun asString() = ""
    override fun compareTo(other: VcsRevisionNumber?) = 0
}

internal fun Instant.toJavaDate(): Date = Date.from(this.toJavaInstant())

internal val testRevisionNumber = DummyVcsRevisionNumber
internal const val testAuthor = "testAuthor"
internal val testDate = Clock.System.now().toJavaDate()

object ConsecutiveDatesGenerator {
    private var currentDateMillis: Long = 1000000000000L
    private const val millisStep: Long = 100000L

    val nextDate: Date
        get() {
            val instant = Instant.fromEpochMilliseconds(currentDateMillis)
            val date = instant.toJavaDate()
            currentDateMillis += millisStep
            return date
        }
}

