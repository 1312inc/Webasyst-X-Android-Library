package com.webasyst.api.util

/**
 * Returns [Throwable]'s root cause. Correctly handles cause cycles.
 */
fun Throwable.getRootCause(): Throwable =
    this.cause?.getRootCause(emptySet()) ?: this

internal fun Throwable.getRootCause(visited: Set<Throwable>): Throwable =
    if (visited.contains(this)) this
    else this.cause?.getRootCause(visited + this) ?: this
