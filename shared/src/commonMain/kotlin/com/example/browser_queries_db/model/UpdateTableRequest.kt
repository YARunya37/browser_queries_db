package com.example.browser_queries_db.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Actions{
    @SerialName("add")
    ADD,
    @SerialName("drop")
    DELETE,
    @SerialName("alter")
    ALTER,
}

@Serializable
data class UpdateColumnQuery(
    val name: String,
    val type: String,
    @SerialName("nullable")
    val isNullable: Boolean,
    val action: Actions,
)

@Serializable
data class UpdateTableRequest(
    val columns: List<UpdateColumnQuery>
)