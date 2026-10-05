package com.example.browser_queries_db.model

data class EmployeeQuery(
    val fields: List<String>? = null,
    val sort: String? = null,
    val direction: String? = null,
    val experienceMin: Int? = null
)