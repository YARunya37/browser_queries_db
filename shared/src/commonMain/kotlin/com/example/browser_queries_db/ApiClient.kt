package com.example.browser_queries_db

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.JsonArray

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }

    install(HttpTimeout) {
        // Максимум 2 сек на установление соединения
        requestTimeoutMillis = 5_000
        // Максимум 5 сек на запрос
        connectTimeoutMillis = 2_000
        // Максимум 5 сек на получение данных
        socketTimeoutMillis = 5_000
    }
}


suspend fun loadEmployees(query: EmployeeQuery): JsonArray {
    val url = buildEmployeeUrl(query)

    return httpClient
        .get(url)
        .body()
}

fun buildEmployeeUrl(query: EmployeeQuery): String {
    val params = mutableListOf<String>()

    query.fields?.let {
        params += "fields=${it.joinToString(",")}"
    }

    query.sort?.let {
        params += "sort=$it"
    }

    query.direction?.let {
        params += "direction=$it"
    }

    query.experienceMin?.let {
        params += "experienceMin=$it"
    }

    return if (params.isEmpty()) {
        "http://localhost:8080/employees"
    } else {
        "http://localhost:8080/employees?${params.joinToString("&")}"
    }
}