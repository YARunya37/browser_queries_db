package com.example

import io.ktor.server.config.*
import java.sql.Connection
import java.sql.DriverManager

fun createConnection(config: ApplicationConfig): Connection {
    val url = config.property("database.url").getString()
    val user = config.property("database.user").getString()
    val password = config.property("database.password").getString()

    return DriverManager.getConnection(
        url,
        user,
        password
    )
}