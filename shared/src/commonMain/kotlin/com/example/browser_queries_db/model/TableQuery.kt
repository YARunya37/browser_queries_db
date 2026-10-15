package com.example.browser_queries_db.model

data class TableQuery(
    val fields: List<String>? = null,
    val sort: String? = null,
    val direction: String? = null
)
