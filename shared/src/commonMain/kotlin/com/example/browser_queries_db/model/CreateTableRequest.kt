package com.example.browser_queries_db.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ColumnDefinition(
    val name: String,
    val type: String,
    @SerialName("nullable")
    val isNullable: Boolean,
)

@Serializable
data class CreateTableRequest(
    val columns: List<ColumnDefinition>,
)