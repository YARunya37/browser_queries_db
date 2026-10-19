package com.example.browser_queries_db

import com.example.browser_queries_db.model.ColumnDefinition
import com.example.browser_queries_db.model.CreateTableRequest
import com.example.browser_queries_db.model.TableQuery
import com.example.browser_queries_db.model.UpdateColumnQuery
import com.example.browser_queries_db.model.UpdateTableRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.JsonArray

// Таблица, данные которой читаются по умолчанию
const val DEFAULT_TABLE_NAME = "table_kliuchikov"

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

suspend fun getTableHeaders(tableName: String): JsonArray {
    return httpClient.get("http://localhost:8080/tables/$tableName").body()
}

suspend fun loadTableRows(tableName: String, query: TableQuery): JsonArray {
    return httpClient.get("http://localhost:8080/tables/$tableName/rows") {
        // Формируем параметры, если они есть
        query.fields?.let {
            parameter("fields", it.joinToString(","))
        }

        query.sort?.let {
            parameter("sort", it)
        }

        query.direction?.let {
            parameter("direction", it)
        }
    }.body()
}

suspend fun updateTable(tableName: String, columns: List<UpdateColumnQuery>): HttpResponse{
    return httpClient.patch("http://localhost:8080/tables/$tableName"){
        contentType(ContentType.Application.Json)
        setBody(UpdateTableRequest(columns))
    }
}

suspend fun createTable(tableName:String, columns: List<ColumnDefinition>): HttpResponse{
    return httpClient.post("http://localhost:8080/tables/$tableName") {
        contentType(ContentType.Application.Json)
        setBody(CreateTableRequest(columns))
    }
}

suspend fun deleteTable(tableName: String): HttpResponse{
    return httpClient.delete("http://localhost:8080/tables/$tableName")
}