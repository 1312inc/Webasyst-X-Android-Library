package com.webasyst.api.adapter

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Json adapter factory to be used when it is known that api response may be malformed/
 */
class FailSafeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>?): TypeAdapter<T> {
        val typeAdapter = gson.getAdapter(type)
        return FailSafeTypeAdapter(typeAdapter)
    }

    private class FailSafeTypeAdapter<T>(
        private val typeAdapter: TypeAdapter<T>
    ) : TypeAdapter<T>() {
        override fun write(out: JsonWriter, value: T) =
            typeAdapter.write(out, value)

        override fun read(input: JsonReader): T? = try {
            typeAdapter.read(input)
        } catch (e: Exception) {
            fallback(input)
        }

        private fun fallback(input: JsonReader): T? {
            when (val token = input.peek()) {
                JsonToken.BEGIN_ARRAY,
                JsonToken.BEGIN_OBJECT,
                JsonToken.NAME,
                JsonToken.STRING,
                JsonToken.NUMBER,
                JsonToken.BOOLEAN,
                JsonToken.NULL ->
                    input.skipValue()
                JsonToken.END_ARRAY ->
                    input.endArray()
                JsonToken.END_OBJECT ->
                    input.endObject()
                JsonToken.END_DOCUMENT ->
                    Unit
                else ->
                    throw AssertionError(token)
            }

            return null
        }
    }
}
