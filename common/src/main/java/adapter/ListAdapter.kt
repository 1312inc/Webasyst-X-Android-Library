package com.webasyst.api.adapter

import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * This [JsonDeserializer] accepts both json arrays (`[{"a":1},{"a":2}]`) and
 * objects (`{"x":{"a":1},"y":{"a":2}}`) and deserializes them to [List].
 */
class ListAdapter : JsonDeserializer<List<*>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): List<*>? {
        val valueType = (typeOfT as ParameterizedType).actualTypeArguments.first()
        return when (json) {
            null -> null
            is JsonNull -> null
            is JsonArray -> {
                json.map {
                    context.deserialize<Any>(it, valueType)
                }
            }
            is JsonObject -> {
                json.keySet()
                    .map { key ->
                        context.deserialize<Any>(json.getAsJsonObject(key), valueType)
                    }
            }
            else -> null
        }
    }
}
