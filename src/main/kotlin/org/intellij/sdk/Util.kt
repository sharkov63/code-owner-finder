package org.intellij.sdk

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import java.util.*

class CodeOwnerFinderException(message: String? = "") : Exception(
    "Could not find code owner: $message"
)

internal fun Date.toKotlinInstant() = this.toInstant().toKotlinInstant()

internal fun Instant.toJavaDate(): Date = Date.from(this.toJavaInstant())


