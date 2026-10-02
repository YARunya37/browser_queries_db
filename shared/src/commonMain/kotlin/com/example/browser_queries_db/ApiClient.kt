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


suspend fun loadEmployees(): JsonArray {
    return httpClient.get(
        "http://localhost:8080/employees?fields=fio,phone,salary"
    ).body<JsonArray>()
}