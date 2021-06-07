package com.webasyst.api.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.webasyst.api.adapter.ListAdapter
import kotlin.reflect.KProperty

object GsonInstance {
    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(List::class.java, ListAdapter())
            .create()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Gson = gson
}
