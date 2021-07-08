package com.webasyst.api.adapter

import com.webasyst.api.util.GsonInstance
import kotlin.test.Test
import kotlin.test.assertEquals

class ListAdapterTest {
    @Test
    fun testListAdapter() {
        val gson by GsonInstance

        val reference = Container(listOf(Container.Item("a"), Container.Item("b")))

        val arrayS = """{"items":[{"value":"a"},{"value":"b"}]}"""
        val array = gson.fromJson(arrayS, Container::class.java)
        assertEquals(reference, array)

        val objS = """{"items":{"q":{"value":"a"},"w":{"value":"b"}}}"""
        val obj = gson.fromJson(objS, Container::class.java)
        assertEquals(reference, obj)

        assertEquals(arrayS, gson.toJson(array))
        assertEquals(arrayS, gson.toJson(obj))
    }

    data class Container(val items: List<Item>) {
        data class Item(val value: String)
    }
}
