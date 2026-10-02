package com.example.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun toJsonElement(value: Any?): JsonElement {
    return when (value) {
        null -> JsonNull

        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)

        else -> JsonPrimitive(value.toString())
    }
}

fun toJsonArray(
    rows: List<Map<String, Any?>>
): JsonArray {

    val jsonRows = rows.map { row ->

        val jsonObject = row.mapValues { (_, value) ->
            toJsonElement(value)
        }

        JsonObject(jsonObject)
    }

    return JsonArray(jsonRows)
}