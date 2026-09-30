package com.example

import io.ktor.server.config.*
import java.sql.Connection
import java.sql.DriverManager

fun createConnection(): Connection {
    val url = "jdbc:postgresql://localhost:5432/kliuchikov"
    val user = "postgres"
    val password = System.getenv("DB_PASSWORD")

    return DriverManager.getConnection(
        url,
        user,
        password
    )
}