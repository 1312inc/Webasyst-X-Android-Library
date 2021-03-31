package com.webasyst.api.util

import kotlin.reflect.KProperty

fun <T> threadLocal(create: () -> T) = ThreadLocalDelegate(create)

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
