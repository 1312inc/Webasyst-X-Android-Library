package com.webasyst.api.util

import kotlin.reflect.KProperty

/**
 * Creates new instance of [ThreadLocalDelegate] that uses [create] for object instantiation
 */
fun <T> threadLocal(create: () -> T) = ThreadLocalDelegate(create)

/**
 * Represents thread-local value backed up by [ThreadLocal].
 *
 * To create an instance of [ThreadLocalDelegate] use the [threadLocal] function.
 */
class ThreadLocalDelegate<T> internal constructor (val create: () -> T) {
    private val threadLocal = ThreadLocal<T>()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        var value = threadLocal.get()
        if (null == value) {
            value = create()
            threadLocal.set(value)
        }
        return value!!
    }
}
