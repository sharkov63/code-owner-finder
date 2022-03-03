package org.intellij.sdk.action

import com.intellij.openapi.vcs.history.VcsRevisionNumber

object DummyVcsRevisionNumber: VcsRevisionNumber {
    override fun asString() = ""
    override fun compareTo(other: VcsRevisionNumber?) = 0
}
